"use client";

import { Check, PhoneOff, ScreenShare, ShieldCheck } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";

import { deviceSubtitle } from "@/components/shell/deviceStatus";
import { Button, Card, Spinner } from "@/components/ui";
import { ApiError, type Device, deviceRealtimeWebSocketUrl, fetchWebRtcConfig } from "@/lib/apiClient";

import styles from "./RemoteViewPanel.module.css";

type ViewerState =
  | { kind: "idle" }
  | { kind: "requesting" }
  | { kind: "connecting"; detail: string }
  | { kind: "streaming" }
  | { kind: "ended"; message: string };

type StepKey = "request" | "wait" | "connect" | "live";

const STEPS: { key: StepKey; label: string }[] = [
  { key: "request", label: "Solicitar" },
  { key: "wait", label: "Esperando" },
  { key: "connect", label: "Conectando" },
  { key: "live", label: "Activa" },
];

function currentStep(state: ViewerState): StepKey | null {
  switch (state.kind) {
    case "idle":
      return null;
    case "requesting":
      return "wait";
    case "connecting":
      return "connect";
    case "streaming":
      return "live";
    case "ended":
      return null;
  }
}

/** What the status column says about the connection, derived only from the viewer state. */
const CONNECTION_LABEL: Record<ViewerState["kind"], string> = {
  idle: "Sin solicitar",
  requesting: "Esperando respuesta",
  connecting: "Conectando",
  streaming: "Conectado",
  ended: "Desconectado",
};

/** The quiet caption inside the dark phone screen while there is no picture to show. */
function screenCaption(kind: ViewerState["kind"]): string {
  if (kind === "requesting" || kind === "connecting") return "Esperando la transmisión";
  if (kind === "ended") return "Transmisión terminada";
  return "Sin transmisión";
}

function formatDuration(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
    .toString()
    .padStart(2, "0");
  const seconds = (totalSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

/** Why a session ended, in the tutor's terms rather than the protocol's. The device sends these
 * reasons (see ScreenShareService); anything unrecognised falls back to a neutral message instead
 * of showing a raw code to a parent.
 */
function describeStopReason(reason: unknown): string {
  switch (reason) {
    case "projection_cancelled":
      return "No se confirmó el diálogo de Android, así que la transmisión no llegó a empezar.";
    case "supervised_stopped":
      return "La persona supervisada detuvo la transmisión.";
    case "projection_stopped":
      return "Android detuvo la captura (por ejemplo, al bloquearse la pantalla).";
    default:
      return "La transmisión terminó.";
  }
}

/** Sprint 23 (logic), Sprint 38 (design). Live view of a supervised device's screen, over WebRTC.
 *
 * The browser is deliberately the passive half: the device captures and offers, this panel only
 * answers and renders. That is what keeps the WebRTC dependency on the Android side alone — a
 * browser already speaks WebRTC natively, so nothing is added here.
 *
 * Nothing starts without the supervised person accepting twice (this panel's request, then
 * Android's own capture dialog), and nothing is recorded: the stream is rendered and discarded.
 * There is no session state on the backend to reconnect to either — closing this panel ends the
 * session, by design (see docs/sprint-23.md). The stepper only distinguishes what the frontend
 * can actually tell apart: "consentimiento" and "negociando" are both the same `connecting`
 * state internally (the `detail` text tells them apart, shown under "Conectando"), because the
 * signalling protocol never reports which of the two is in progress separately.
 *
 * Sprint 59 (editorial recomposition): the phone frame is always drawn and holds the same,
 * always-mounted <video>; a calm status column sits beside it (below it on phones). `device` is
 * optional and display-only — when the shell passes the active device, its name and Android
 * version are shown; nothing here fetches it. Battery, network, rotate and fullscreen from the
 * mockup are omitted: no model carries them and the signalling channel has no command for them.
 */
export function RemoteViewPanel({
  accessToken,
  deviceId,
  device,
}: {
  accessToken: string;
  deviceId: string;
  device?: Pick<Device, "name" | "platform" | "os_version">;
}) {
  const [state, setState] = useState<ViewerState>({ kind: "idle" });
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const socketRef = useRef<WebSocket | null>(null);
  const peerRef = useRef<RTCPeerConnection | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamStartedAtRef = useRef<number | null>(null);

  const teardown = useCallback((notifyPeer: boolean) => {
    if (notifyPeer && socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(
        JSON.stringify({ type: "screen_share_stop", reason: "tutor_closed" })
      );
    }
    peerRef.current?.close();
    peerRef.current = null;
    socketRef.current?.close();
    socketRef.current = null;
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
  }, []);

  // Cleanup only — no setState in the effect body, so this stays clear of the
  // react-hooks/set-state-in-effect rule the whole project works around (see CLAUDE.md).
  useEffect(() => () => teardown(true), [teardown]);

  // Ticks the visible duration while streaming; the interval callback is the only place that
  // calls setState here, never the effect body itself.
  useEffect(() => {
    if (state.kind !== "streaming") return;
    const id = setInterval(() => {
      if (streamStartedAtRef.current) {
        setElapsedSeconds(Math.floor((Date.now() - streamStartedAtRef.current) / 1000));
      }
    }, 1000);
    return () => clearInterval(id);
  }, [state.kind]);

  const stop = useCallback(() => {
    teardown(true);
    setState({ kind: "ended", message: "Cerraste la transmisión." });
  }, [teardown]);

  // A click handler, not an effect: every setState below runs from a user gesture or from a real
  // async browser callback, never synchronously during render.
  const start = useCallback(() => {
    setElapsedSeconds(0);
    streamStartedAtRef.current = null;
    setState({ kind: "requesting" });

    fetchWebRtcConfig(accessToken, deviceId)
      .then(({ ice_servers, turn_servers }) => {
        // STUN for a direct path when one exists, plus the TURN relay (Sprint 28) for when it
        // doesn't — an emulator behind its own NAT, or two mobile networks.
        const peer = new RTCPeerConnection({
          iceServers: [
            ...ice_servers.map((urls) => ({ urls })),
            ...turn_servers.map(({ urls, username, credential }) => ({
              urls,
              username,
              credential,
            })),
          ],
        });
        peerRef.current = peer;

        peer.addEventListener("track", (event) => {
          if (videoRef.current) {
            videoRef.current.srcObject = event.streams[0] ?? null;
          }
          streamStartedAtRef.current = Date.now();
          setState({ kind: "streaming" });
        });

        peer.addEventListener("icecandidate", (event) => {
          if (!event.candidate || socketRef.current?.readyState !== WebSocket.OPEN) return;
          socketRef.current.send(
            JSON.stringify({
              type: "screen_share_ice_candidate",
              candidate: event.candidate.candidate,
              sdp_mid: event.candidate.sdpMid,
              sdp_m_line_index: event.candidate.sdpMLineIndex,
            })
          );
        });

        // Reaching this means even the TURN relay (Sprint 28) couldn't carry the stream — usually
        // a network that blocks the relay's port — so it gets its own message, not a generic one.
        peer.addEventListener("connectionstatechange", () => {
          if (peer.connectionState === "failed") {
            teardown(false);
            setState({
              kind: "ended",
              message:
                "No se pudo establecer la conexión de video con el dispositivo, ni directa ni " +
                "a través del servidor de retransmisión. Revisa la conexión a internet de ambos.",
            });
          }
        });

        const socket = new WebSocket(deviceRealtimeWebSocketUrl(deviceId));
        socketRef.current = socket;

        socket.addEventListener("open", () => {
          socket.send(JSON.stringify({ token: accessToken }));
          socket.send(JSON.stringify({ type: "screen_share_request" }));
        });

        socket.addEventListener("close", () => {
          setState((current) =>
            current.kind === "streaming" || current.kind === "connecting"
              ? { kind: "ended", message: "Se perdió la conexión con el servidor." }
              : current
          );
        });

        socket.addEventListener("message", (event) => {
          let payload: Record<string, unknown>;
          try {
            payload = JSON.parse(event.data as string);
          } catch {
            return;
          }

          if (payload.event === "screen_share_busy") {
            // Backend rejected this request because another tutor connection already holds a
            // live session on this device (see ConnectionManager.begin_screen_share) — the
            // in-progress session was left untouched, so this tutor just has to wait its turn.
            teardown(false);
            setState({
              kind: "ended",
              message: "Ya hay otro tutor viendo la pantalla de este dispositivo ahora mismo.",
            });
            return;
          }

          if (payload.type === "screen_share_consent") {
            if (payload.granted) {
              setState({
                kind: "connecting",
                detail:
                  "Aceptado. Falta que se confirme el diálogo de Android en el dispositivo…",
              });
            } else {
              teardown(false);
              setState({
                kind: "ended",
                message: "La persona supervisada no aceptó la solicitud.",
              });
            }
            return;
          }

          if (payload.type === "screen_share_offer" && typeof payload.sdp === "string") {
            setState({ kind: "connecting", detail: "Negociando la conexión de video…" });
            peer
              .setRemoteDescription({ type: "offer", sdp: payload.sdp })
              .then(() => peer.createAnswer())
              .then((answer) => peer.setLocalDescription(answer).then(() => answer))
              .then((answer) => {
                socket.send(JSON.stringify({ type: "screen_share_answer", sdp: answer.sdp }));
              })
              .catch(() => {
                teardown(false);
                setState({ kind: "ended", message: "No se pudo negociar la conexión de video." });
              });
            return;
          }

          if (
            payload.type === "screen_share_ice_candidate" &&
            typeof payload.candidate === "string"
          ) {
            peer
              .addIceCandidate({
                candidate: payload.candidate,
                sdpMid: (payload.sdp_mid as string | null) ?? undefined,
                sdpMLineIndex: (payload.sdp_m_line_index as number | null) ?? undefined,
              })
              .catch(() => {
                // A candidate that arrives before the remote description, or a malformed one:
                // WebRTC recovers from losing individual candidates, so this is not fatal.
              });
            return;
          }

          if (payload.type === "screen_share_stop") {
            teardown(false);
            setState({ kind: "ended", message: describeStopReason(payload.reason) });
          }
        });
      })
      .catch((error) => {
        setState({
          kind: "ended",
          message:
            error instanceof ApiError
              ? error.message
              : "No se pudo preparar la conexión con el dispositivo.",
        });
      });
  }, [accessToken, deviceId, teardown]);

  const isActive =
    state.kind === "requesting" || state.kind === "connecting" || state.kind === "streaming";
  const step = currentStep(state);
  const activeIndex = step ? STEPS.findIndex((item) => item.key === step) : -1;

  return (
    <Card padding="none" className={styles.panel}>
      <div className={styles.status}>
        <div className={styles.statusHead}>
          <h2 className={styles.title}>Transmisión</h2>
          {state.kind === "streaming" && (
            <span className={styles.live}>
              <span className={styles.liveDot} aria-hidden="true" />
              <span className="eyebrow">En vivo</span>
              <span className={`tabular ${styles.elapsed}`}>
                <span className={styles.srOnly}>Tiempo transcurrido: </span>
                {formatDuration(elapsedSeconds)}
              </span>
            </span>
          )}
        </div>
        <dl className={styles.facts}>
          <div className={styles.fact}>
            <dt>Conexión</dt>
            <dd>{CONNECTION_LABEL[state.kind]}</dd>
          </div>
          {device && (
            <div className={styles.fact}>
              <dt>Dispositivo</dt>
              <dd>
                {device.name}
                <span className={styles.factMeta}>{deviceSubtitle(device.platform, device.os_version)}</span>
              </dd>
            </div>
          )}
        </dl>
      </div>

      <div className={styles.stage}>
        <div className={styles.phone}>
          <span className={styles.camera} aria-hidden="true" />
          <div className={styles.screen}>
            {state.kind !== "streaming" && (
              <div className={styles.screenIdle}>
                <ScreenShare size={28} strokeWidth={1.5} aria-hidden="true" />
                <span>{screenCaption(state.kind)}</span>
              </div>
            )}
            {/* Always mounted — never conditionally rendered — so the single DOM node the
                peer's "track" listener attaches srcObject to survives every state transition.
                Only the phone around it is decorative; the element, its ref and its attributes
                are exactly the ones Sprint 23 shipped. */}
            <video
              ref={videoRef}
              className={state.kind === "streaming" ? styles.video : undefined}
              autoPlay
              playsInline
              muted
              hidden={state.kind !== "streaming"}
            />
          </div>
        </div>
      </div>

      <div className={styles.details}>
        <div className={styles.notice}>
          <ShieldCheck size={20} strokeWidth={1.75} aria-hidden="true" />
          <p>
            La transmisión solo empieza si la persona supervisada acepta, ella la ve mientras dure y
            puede cortarla cuando quiera. No se graba nada: el video se muestra aquí y no se guarda.
          </p>
        </div>

        {activeIndex >= 0 && (
          <ol className={styles.stepper} aria-label="Progreso de la solicitud">
            {STEPS.map((item, index) => {
              const status = index < activeIndex ? "done" : index === activeIndex ? "active" : "pending";
              return (
                <li
                  key={item.key}
                  className={styles.step}
                  data-status={status}
                  aria-current={status === "active" ? "step" : undefined}
                >
                  <span className={styles.stepDot} aria-hidden="true">
                    {status === "done" ? <Check size={12} strokeWidth={3} /> : index + 1}
                  </span>
                  <span className={styles.stepLabel}>{item.label}</span>
                </li>
              );
            })}
          </ol>
        )}

        {state.kind === "idle" && (
          <p className={styles.message}>Todavía no has pedido ver la pantalla de este dispositivo.</p>
        )}

        {state.kind === "requesting" && (
          <div className={styles.waiting}>
            <Spinner label="Esperando respuesta en el dispositivo…" />
          </div>
        )}

        {state.kind === "connecting" && (
          <div className={styles.waiting}>
            <Spinner label={state.detail} />
          </div>
        )}

        {state.kind === "ended" && (
          <p className={styles.message} role="status">
            {state.message}
          </p>
        )}

        <div className={styles.actions}>
          {isActive ? (
            <Button variant="danger" icon={PhoneOff} fullWidth onClick={stop}>
              Finalizar vista remota
            </Button>
          ) : (
            <Button variant="primary" icon={ScreenShare} fullWidth onClick={start}>
              Solicitar ver pantalla
            </Button>
          )}
        </div>
      </div>
    </Card>
  );
}
