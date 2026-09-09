import uuid
from datetime import datetime
from typing import Annotated, Literal

from pydantic import BaseModel, Field


class RegisterPushTokenRequest(BaseModel):
    fcm_token: str = Field(min_length=1, max_length=255)


class RegisterPushTokenResponse(BaseModel):
    device_id: uuid.UUID
    updated_at: datetime


# --------------------------------------------------------------- screen sharing (Sprint 23)
#
# WebRTC signalling frames, relayed verbatim between the two peers over the device channel that
# already exists (app/api/v1/endpoints/realtime.py). The backend never interprets an SDP or an ICE
# candidate — it is a relay, not a peer — but it does validate the envelope, because until Sprint
# 23 that socket only ever *sent*: anything a client wrote was discarded unread. Now that inbound
# frames are acted upon, they are untrusted input on an authenticated socket and get the same
# treatment as any request body: a closed set of types and an explicit length cap on every string,
# so a peer cannot push unbounded text through the backend into the other peer.


class ScreenShareRequest(BaseModel):
    """Tutor -> device: "may I see your screen?". Carries nothing: the device already knows which
    channel it arrived on, and consent is answered separately."""

    type: Literal["screen_share_request"]


class ScreenShareConsent(BaseModel):
    """Device -> tutor: the supervised user's answer, before Android's own capture dialog is even
    shown. `granted=True` here does not mean capture started — the OS still has to ask."""

    type: Literal["screen_share_consent"]
    granted: bool


class ScreenShareOffer(BaseModel):
    type: Literal["screen_share_offer"]
    sdp: str = Field(min_length=1, max_length=16384)


class ScreenShareAnswer(BaseModel):
    type: Literal["screen_share_answer"]
    sdp: str = Field(min_length=1, max_length=16384)


class ScreenShareIceCandidate(BaseModel):
    type: Literal["screen_share_ice_candidate"]
    candidate: str = Field(max_length=1024)
    sdp_mid: str | None = Field(default=None, max_length=64)
    sdp_m_line_index: int | None = Field(default=None, ge=0, le=64)


class ScreenShareStop(BaseModel):
    type: Literal["screen_share_stop"]
    reason: str | None = Field(default=None, max_length=64)


ScreenShareSignal = Annotated[
    ScreenShareRequest
    | ScreenShareConsent
    | ScreenShareOffer
    | ScreenShareAnswer
    | ScreenShareIceCandidate
    | ScreenShareStop,
    Field(discriminator="type"),
]


class WebRtcConfigResponse(BaseModel):
    """What both clients need to build an RTCPeerConnection. `ice_servers` is a list of URLs
    rather than the full RTCIceServer object shape because this project has no TURN server, and
    TURN is the only part of that shape that needs credentials — see settings.webrtc_stun_urls.
    """

    ice_servers: list[str]
