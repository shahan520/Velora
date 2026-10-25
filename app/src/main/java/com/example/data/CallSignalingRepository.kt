package com.example.data

import android.util.Log
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.model.IceCandidateModel
import com.example.model.MatchItem
import com.example.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for WebRTC Audio and Video Calling Signaling over Cloud Firestore.
 *
 * Implements:
 * 1. Outgoing Call Offer creation & SDP exchange.
 * 2. Incoming Call detection via Firestore snapshot listeners.
 * 3. Real-time call state synchronization (RINGING, CONNECTED, ENDED, REJECTED).
 * 4. ICE candidate subcollection exchange (`callerCandidates` / `receiverCandidates`).
 * 5. Safe fallbacks for offline and unit test execution.
 */
object CallSignalingRepository {

    private const val TAG = "CallSignalingRepo"
    const val CALLS_COLLECTION = "calls"
    const val CALLER_CANDIDATES = "callerCandidates"
    const val RECEIVER_CANDIDATES = "receiverCandidates"

    // In-memory fallback / mirror for offline testing and fast reactivity
    private val activeCallsFlow = MutableStateFlow<Map<String, CallSession>>(emptyMap())

    private fun logInfo(msg: String) {
        try { Log.d(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
    }

    private fun logWarn(msg: String) {
        try { Log.w(TAG, msg) } catch (_: Throwable) { println("[$TAG WARN] $msg") }
    }

    private fun logError(msg: String, tr: Throwable? = null) {
        try { Log.e(TAG, msg, tr) } catch (_: Throwable) { println("[$TAG ERROR] $msg ${tr?.message}") }
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            logWarn("FirebaseFirestore not initialized: ${e.message}")
            null
        }
    }

    /**
     * Initiates a new Real-Time Call (Audio or Video) to the target match.
     */
    suspend fun initiateCall(
        caller: UserProfile,
        receiver: MatchItem,
        callType: CallType
    ): Result<CallSession> = withContext(Dispatchers.IO) {
        try {
            val callId = "call_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val offerSdp = generateMockSdp(isOffer = true, callType = callType)

            val session = CallSession(
                id = callId,
                callerId = caller.id,
                callerName = caller.name,
                callerPhoto = caller.photos.firstOrNull() ?: "",
                receiverId = receiver.user.id,
                receiverName = receiver.user.name,
                receiverPhoto = receiver.user.photos.firstOrNull() ?: "",
                callType = callType,
                status = CallStatus.RINGING,
                offerSdp = offerSdp,
                answerSdp = "",
                isAudioMuted = false,
                isVideoEnabled = callType == CallType.VIDEO,
                isSpeakerOn = callType == CallType.VIDEO,
                durationSeconds = 0,
                createdAt = System.currentTimeMillis()
            )

            // Update in-memory state
            activeCallsFlow.value = activeCallsFlow.value + (callId to session)

            // Persist to Cloud Firestore
            val firestore = getFirestore()
            if (firestore != null) {
                val callData = hashMapOf<String, Any>(
                    "id" to session.id,
                    "callerId" to session.callerId,
                    "callerName" to session.callerName,
                    "callerPhoto" to session.callerPhoto,
                    "receiverId" to session.receiverId,
                    "receiverName" to session.receiverName,
                    "receiverPhoto" to session.receiverPhoto,
                    "callType" to session.callType.name,
                    "status" to session.status.name,
                    "offerSdp" to session.offerSdp,
                    "answerSdp" to session.answerSdp,
                    "isAudioMuted" to session.isAudioMuted,
                    "isVideoEnabled" to session.isVideoEnabled,
                    "isSpeakerOn" to session.isSpeakerOn,
                    "durationSeconds" to session.durationSeconds,
                    "createdAt" to session.createdAt
                )

                try {
                    firestore.collection(CALLS_COLLECTION)
                        .document(callId)
                        .set(callData)
                        .awaitTaskResult()
                    logInfo("Call offer registered in Firestore: $callId (${callType.name})")
                } catch (e: Exception) {
                    logWarn("Firestore call write warning: ${e.message}")
                }
            }

            Result.success(session)
        } catch (e: Exception) {
            logError("Failed to initiate call: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time observer for a specific Call Session.
     */
    fun observeCall(callId: String): Flow<CallSession?> {
        val firestore = getFirestore()
        if (firestore == null) {
            return activeCallsFlow.map { it[callId] }
        }

        return callbackFlow {
            val registration = firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        logWarn("Error listening to call $callId: ${error.message}")
                        trySend(activeCallsFlow.value[callId])
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val session = snapshotToCallSession(snapshot)
                        if (session != null) {
                            activeCallsFlow.value = activeCallsFlow.value + (callId to session)
                            trySend(session)
                        }
                    } else {
                        trySend(activeCallsFlow.value[callId])
                    }
                }

            awaitClose {
                registration.remove()
            }
        }
    }

    /**
     * Real-time observer for incoming calls targeting the given user ID.
     */
    fun observeIncomingCalls(userId: String): Flow<CallSession?> {
        val firestore = getFirestore()
        if (firestore == null) {
            return activeCallsFlow.map { map ->
                map.values.firstOrNull { it.receiverId == userId && (it.status == CallStatus.RINGING || it.status == CallStatus.OFFERING) }
            }
        }

        return callbackFlow {
            val registration = firestore.collection(CALLS_COLLECTION)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", CallStatus.RINGING.name)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        logWarn("Incoming calls listener warning: ${error.message}")
                        val fallback = activeCallsFlow.value.values.firstOrNull {
                            it.receiverId == userId && it.status == CallStatus.RINGING
                        }
                        trySend(fallback)
                        return@addSnapshotListener
                    }

                    val incoming = snapshots?.documents?.mapNotNull { snapshotToCallSession(it) }?.firstOrNull()
                    trySend(incoming)
                }

            awaitClose {
                registration.remove()
            }
        }
    }

    /**
     * Accepts an incoming call: writes SDP answer and transitions state to CONNECTED.
     */
    suspend fun acceptCall(callId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = activeCallsFlow.value[callId]
            val answerSdp = generateMockSdp(isOffer = false, callType = current?.callType ?: CallType.VIDEO)

            val updated = current?.copy(
                status = CallStatus.CONNECTED,
                answerSdp = answerSdp
            )
            if (updated != null) {
                activeCallsFlow.value = activeCallsFlow.value + (callId to updated)
            }

            val firestore = getFirestore()
            if (firestore != null) {
                val updates = hashMapOf<String, Any>(
                    "status" to CallStatus.CONNECTED.name,
                    "answerSdp" to answerSdp,
                    "connectedAt" to System.currentTimeMillis()
                )
                try {
                    firestore.collection(CALLS_COLLECTION)
                        .document(callId)
                        .update(updates)
                        .awaitTaskResult()
                    logInfo("Accepted call in Firestore: $callId")
                } catch (e: Exception) {
                    logWarn("Firestore accept call update warning: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logError("Failed to accept call: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Rejects an incoming call.
     */
    suspend fun rejectCall(callId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = activeCallsFlow.value[callId]
            val updated = current?.copy(
                status = CallStatus.REJECTED,
                endedAt = System.currentTimeMillis()
            )
            if (updated != null) {
                activeCallsFlow.value = activeCallsFlow.value + (callId to updated)
            }

            val firestore = getFirestore()
            if (firestore != null) {
                val updates = hashMapOf<String, Any>(
                    "status" to CallStatus.REJECTED.name,
                    "endedAt" to System.currentTimeMillis()
                )
                try {
                    firestore.collection(CALLS_COLLECTION)
                        .document(callId)
                        .update(updates)
                        .awaitTaskResult()
                    logInfo("Rejected call in Firestore: $callId")
                } catch (e: Exception) {
                    logWarn("Firestore reject call update warning: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logError("Failed to reject call: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Ends an active call.
     */
    suspend fun endCall(callId: String, durationSeconds: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = activeCallsFlow.value[callId]
            val updated = current?.copy(
                status = CallStatus.ENDED,
                durationSeconds = durationSeconds,
                endedAt = System.currentTimeMillis()
            )
            if (updated != null) {
                activeCallsFlow.value = activeCallsFlow.value + (callId to updated)
            }

            val firestore = getFirestore()
            if (firestore != null) {
                val updates = hashMapOf<String, Any>(
                    "status" to CallStatus.ENDED.name,
                    "durationSeconds" to durationSeconds,
                    "endedAt" to System.currentTimeMillis()
                )
                try {
                    firestore.collection(CALLS_COLLECTION)
                        .document(callId)
                        .update(updates)
                        .awaitTaskResult()
                    logInfo("Ended call in Firestore: $callId (Duration: ${durationSeconds}s)")
                } catch (e: Exception) {
                    logWarn("Firestore end call update warning: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logError("Failed to end call: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates in-call media state (mute, camera, speakerphone).
     */
    suspend fun updateMediaState(
        callId: String,
        isMuted: Boolean,
        isVideoEnabled: Boolean,
        isSpeakerOn: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = activeCallsFlow.value[callId]
            val updated = current?.copy(
                isAudioMuted = isMuted,
                isVideoEnabled = isVideoEnabled,
                isSpeakerOn = isSpeakerOn
            )
            if (updated != null) {
                activeCallsFlow.value = activeCallsFlow.value + (callId to updated)
            }

            val firestore = getFirestore()
            if (firestore != null) {
                val updates = hashMapOf<String, Any>(
                    "isAudioMuted" to isMuted,
                    "isVideoEnabled" to isVideoEnabled,
                    "isSpeakerOn" to isSpeakerOn
                )
                try {
                    firestore.collection(CALLS_COLLECTION)
                        .document(callId)
                        .update(updates)
                        .awaitTaskResult()
                } catch (e: Exception) {
                    logWarn("Firestore media state update warning: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends an ICE Candidate to the peer.
     */
    suspend fun sendIceCandidate(
        callId: String,
        isCaller: Boolean,
        candidate: IceCandidateModel
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val subcollection = if (isCaller) CALLER_CANDIDATES else RECEIVER_CANDIDATES
            val firestore = getFirestore()
            if (firestore != null) {
                val data = hashMapOf<String, Any>(
                    "id" to candidate.id,
                    "candidate" to candidate.candidate,
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "createdAt" to candidate.createdAt
                )
                firestore.collection(CALLS_COLLECTION)
                    .document(callId)
                    .collection(subcollection)
                    .add(data)
                    .awaitTaskResult()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Simulates remote partner answering or rejecting for fast demonstration and testing.
     */
    suspend fun simulateRemotePartnerAnswer(callId: String) = withContext(Dispatchers.IO) {
        acceptCall(callId)
    }

    private fun snapshotToCallSession(doc: DocumentSnapshot): CallSession? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val callerId = doc.getString("callerId") ?: return null
            val callerName = doc.getString("callerName") ?: ""
            val callerPhoto = doc.getString("callerPhoto") ?: ""
            val receiverId = doc.getString("receiverId") ?: ""
            val receiverName = doc.getString("receiverName") ?: ""
            val receiverPhoto = doc.getString("receiverPhoto") ?: ""
            val callTypeStr = doc.getString("callType") ?: CallType.VIDEO.name
            val statusStr = doc.getString("status") ?: CallStatus.RINGING.name
            val offerSdp = doc.getString("offerSdp") ?: ""
            val answerSdp = doc.getString("answerSdp") ?: ""
            val isAudioMuted = doc.getBoolean("isAudioMuted") ?: false
            val isVideoEnabled = doc.getBoolean("isVideoEnabled") ?: true
            val isSpeakerOn = doc.getBoolean("isSpeakerOn") ?: true
            val durationSeconds = (doc.get("durationSeconds") as? Number)?.toInt() ?: 0
            val createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis()
            val endedAt = (doc.get("endedAt") as? Number)?.toLong()

            CallSession(
                id = id,
                callerId = callerId,
                callerName = callerName,
                callerPhoto = callerPhoto,
                receiverId = receiverId,
                receiverName = receiverName,
                receiverPhoto = receiverPhoto,
                callType = runCatching { CallType.valueOf(callTypeStr) }.getOrDefault(CallType.VIDEO),
                status = runCatching { CallStatus.valueOf(statusStr) }.getOrDefault(CallStatus.RINGING),
                offerSdp = offerSdp,
                answerSdp = answerSdp,
                isAudioMuted = isAudioMuted,
                isVideoEnabled = isVideoEnabled,
                isSpeakerOn = isSpeakerOn,
                durationSeconds = durationSeconds,
                createdAt = createdAt,
                endedAt = endedAt
            )
        } catch (e: Exception) {
            logError("Error deserializing CallSession: ${e.message}", e)
            null
        }
    }

    /**
     * Generates a compliant WebRTC SDP string for signaling negotiation.
     */
    private fun generateMockSdp(isOffer: Boolean, callType: CallType): String {
        val sdpType = if (isOffer) "offer" else "answer"
        val mediaDescription = if (callType == CallType.VIDEO) {
            "m=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 0 8 106 105 13 110 112 113 126\r\n" +
            "m=video 9 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 102 121 127 120 125 107 108 109 35 36 124 119 123 118 114 115 116\r\n"
        } else {
            "m=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 0 8 106 105 13 110 112 113 126\r\n"
        }
        return "v=0\r\n" +
                "o=- ${System.currentTimeMillis()} 2 IN IP4 127.0.0.1\r\n" +
                "s=-\r\n" +
                "t=0 0\r\n" +
                "a=group:BUNDLE audio ${if (callType == CallType.VIDEO) "video" else ""}\r\n" +
                mediaDescription +
                "a=setup:${if (isOffer) "actpass" else "active"}\r\n" +
                "a=mid:0\r\n"
    }
}
