"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { ApiError, deviceRealtimeWebSocketUrl, fetchWebRtcConfig } from "@/lib/apiClient";

type ViewerState =
  | { kind: "idle" }
  | { kind: "requesting" }
  | { kind: "connecting"; detail: string }
  | { kind: "streaming" }
  | { kind: "ended"; message: string };

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

/** Sprint 23. Live view of a supervised device's screen, over WebRTC.
 *
 * The browser is deliberately the passive half: the device captures and offers, this panel only
 * answers and renders. That is what keeps the WebRTC dependency on the Android side alone — a
 * browser already speaks WebRTC natively, so nothing is added here.
 *
 * Nothing starts without the supervised person accepting twice (this panel's request, then
 * Android's own capture dialog), and nothing is recorded: the stream is rendered and discarded.
 * There is no session state on the backend to reconnect to either — closing this panel ends the
 * session, by design (see docs/sprint-23.md).
 */
export function RemoteViewPanel({
  accessToken,
  deviceId,
}: {
  accessToken: string;
  deviceId: string;
}) {
  const [state, setState] = useState<ViewerState>({ kind: "idle" });
  const socketRef = useRef<WebSocket | null>(null);
  const peerRef = useRef<RTCPeerConnection | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);

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

  const stop = useCallback(() => {
    teardown(true);
    setState({ kind: "ended", message: "Cerraste la transmisión." });
  }, [teardown]);

  // A click handler, not an effect: every setState below runs from a user gesture or from a real
  // async browser callback, never synchronously during render.
  const start = useCallback(() => {
    setState({ kind: "requesting" });

    fetchWebRtcConfig(accessToken, deviceId)
      .then(({ ice_servers }) => {
        const peer = new RTCPeerConnection({
          iceServers: ice_servers.map((urls) => ({ urls })),
        });
        peerRef.current = peer;

        peer.addEventListener("track", (event) => {
          if (videoRef.current) {
            videoRef.current.srcObject = event.streams[0] ?? null;
          }
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

        // Without a TURN server (this project has none — see docs/sprint-23.md), a failed
        // connection is the expected outcome on most mobile networks, so it gets its own message
        // rather than a generic error.
        peer.addEventListener("connectionstatechange", () => {
          if (peer.connectionState === "failed") {
            teardown(false);
            setState({
              kind: "ended",
              message:
                "No se pudo establecer la conexión directa con el dispositivo. Sin un servidor " +
                "TURN esto sólo funciona cuando ambos están en una red que lo permite.",
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

  return (
    <div className="rulesPanel">
      <div className="devicesPanelHeader">
        <strong>Ver pantalla</strong>
        {isActive ? (
          <button type="button" onClick={stop}>
            Detener
          </button>
        ) : (
          <button type="button" onClick={start}>
            Solicitar ver pantalla
          </button>
        )}
      </div>

      <p className="statusText">
        La transmisión sólo empieza si la persona supervisada acepta, ella la ve mientras dure y
        puede cortarla cuando quiera. No se graba nada: el video se muestra aquí y no se guarda.
      </p>

      {state.kind === "requesting" && (
        <p className="statusText">Esperando respuesta en el dispositivo…</p>
      )}
      {state.kind === "connecting" && <p className="statusText">{state.detail}</p>}
      {state.kind === "ended" && <p className="authError">{state.message}</p>}

      <video
        ref={videoRef}
        className="remoteScreenVideo"
        autoPlay
        playsInline
        muted
        hidden={state.kind !== "streaming"}
      />
    </div>
  );
}
