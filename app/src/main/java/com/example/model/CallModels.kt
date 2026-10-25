package com.example.model

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus {
    OFFERING,
    RINGING,
    CONNECTING,
    CONNECTED,
    ENDED,
    REJECTED,
    MISSED,
    BUSY
}

/**
 * Represents a Real-Time Audio or Video Call Session signaling state.
 * Synchronized through Cloud Firestore `/calls/{callId}` for WebRTC signaling.
 */
data class CallSession(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhoto: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverPhoto: String = "",
    val callType: CallType = CallType.VIDEO,
    val status: CallStatus = CallStatus.RINGING,
    val offerSdp: String = "",
    val answerSdp: String = "",
    val isAudioMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isSpeakerOn: Boolean = true,
    val durationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

/**
 * WebRTC ICE Candidate exchanged through Firestore subcollections
 * `/calls/{callId}/callerCandidates` and `/calls/{callId}/receiverCandidates`.
 */
data class IceCandidateModel(
    val id: String = "",
    val candidate: String = "",
    val sdpMid: String = "0",
    val sdpMLineIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
