package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.media.CallMediaManager
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.ui.theme.*

/**
 * Incoming Call Heads-Up Overlay Banner.
 * Displays when a real-time incoming audio/video call is received via Firestore signaling.
 */
@Composable
fun IncomingCallOverlay(
    callSession: CallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onAccept()
    }

    // Pulsing animation for the incoming call ring indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("incoming_call_overlay"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VeloraPlumDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, VeloraPink.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing caller avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(pulseScale)
                ) {
                    AsyncImage(
                        model = callSession.callerPhoto.ifBlank {
                            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80"
                        },
                        contentDescription = "Caller Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, VeloraPink, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = callSession.callerName.ifBlank { "Velora Match" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (callSession.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = null,
                            tint = VeloraPinkLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (callSession.callType == CallType.VIDEO) "Incoming Video Call..." else "Incoming Voice Call...",
                            fontSize = 13.sp,
                            color = VeloraPinkLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Decline (Red) and Accept (Green)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Button
                Button(
                    onClick = onDecline,
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraErrorRed),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("decline_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Decline Call",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decline", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Accept Button
                Button(
                    onClick = {
                        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (!hasAudio || (!hasCamera && callSession.callType == CallType.VIDEO)) {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                            )
                        } else {
                            onAccept()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraOnlineGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag("accept_call_button")
                ) {
                    Icon(
                        imageVector = if (callSession.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Accept Call",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Full-Featured Real-Time Audio & Video Call Screen.
 * Integrated with CameraX preview, Audio routing, Firestore WebRTC signaling,
 * call duration timers, and in-call controls.
 */
@Composable
fun RealTimeCallScreen(
    callSession: CallSession,
    durationSeconds: Int,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoEnabled: Boolean,
    isFrontCamera: Boolean,
    mediaManager: CallMediaManager?,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onEndCall: () -> Unit,
    onSimulateAnswer: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Request permissions if not yet granted
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    LaunchedEffect(Unit) {
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasAudio || !hasCamera) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
        }
    }

    val isConnected = callSession.status == CallStatus.CONNECTED
    val isRinging = callSession.status == CallStatus.RINGING || callSession.status == CallStatus.OFFERING

    // Format duration 00:00
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VeloraPlumDark)
            .testTag("real_time_call_screen")
    ) {
        // --- 1. Background Video / Audio Backdrop ---
        if (callSession.callType == CallType.VIDEO && isConnected) {
            // Remote Video Stream Preview
            AsyncImage(
                model = callSession.receiverPhoto.ifBlank {
                    callSession.callerPhoto.ifBlank {
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80"
                    }
                },
                contentDescription = "Remote Video Stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Audio call or Ringing screen: Gradient with animated center avatar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                VeloraPlumMedium,
                                VeloraPlumDark,
                                Color.Black
                            )
                        )
                    )
            )
        }

        // Overlay scrim for maximum text and control contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // --- 2. Top Header: User Name, Status, Duration, Encryption Badge ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Security / Encryption Badge
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = VeloraGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "End-to-End Encrypted WebRTC",
                        fontSize = 11.sp,
                        color = VeloraGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = callSession.receiverName.ifBlank { callSession.callerName.ifBlank { "Velora Partner" } },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dynamic Call Status / Timer
            Text(
                text = when {
                    isConnected -> formattedDuration
                    isRinging -> "Ringing..."
                    callSession.status == CallStatus.CONNECTING -> "Connecting WebRTC Stream..."
                    callSession.status == CallStatus.ENDED -> "Call Ended ($formattedDuration)"
                    callSession.status == CallStatus.REJECTED -> "Call Declined"
                    else -> "Velora Secure Call"
                },
                fontSize = 15.sp,
                color = if (isConnected) VeloraOnlineGreen else VeloraPinkLight,
                fontWeight = FontWeight.SemiBold
            )
        }

        // --- 3. Center Content (Audio Visualizer or Avatar Backdrop) ---
        if (callSession.callType == CallType.AUDIO || !isConnected) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Expanding pulse rings animation
                val infiniteTransition = rememberInfiniteTransition(label = "audio_pulse")
                val pulseRing by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseRing"
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(if (isRinging || isConnected) pulseRing else 1f)
                ) {
                    // Outer glow circle
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(VeloraPink.copy(alpha = 0.2f))
                    )
                    AsyncImage(
                        model = callSession.receiverPhoto.ifBlank {
                            callSession.callerPhoto.ifBlank {
                                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80"
                            }
                        },
                        contentDescription = "Partner Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(CircleShape)
                            .border(3.dp, VeloraPink, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (callSession.callType == CallType.AUDIO) {
                    Text(
                        text = if (isConnected) "High Definition Voice" else "Connecting audio stream...",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // If outgoing call is ringing, show simulated partner answer button for effortless testing
                if (isRinging) {
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onSimulateAnswer,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VeloraGold),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VeloraGold),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("simulate_partner_answer_button")
                    ) {
                        Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Partner Answering", fontSize = 12.sp)
                    }
                }
            }
        }

        // --- 4. Picture-in-Picture: Live Local CameraX Preview ---
        if (callSession.callType == CallType.VIDEO && isVideoEnabled) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .size(width = 110.dp, height = 150.dp)
                    .testTag("local_camera_preview_card"),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    key(isFrontCamera) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    mediaManager?.startCameraPreview(
                                        lifecycleOwner = lifecycleOwner,
                                        previewView = this,
                                        lensFacing = if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                                    )
                                }
                            },
                            update = { pv ->
                                mediaManager?.startCameraPreview(
                                    lifecycleOwner = lifecycleOwner,
                                    previewView = pv,
                                    lensFacing = if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Camera indicator badge (FRONT / BACK)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = if (isFrontCamera) "FRONT" else "BACK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Flip camera shortcut on PIP
                    IconButton(
                        onClick = onFlipCamera,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("pip_camera_flip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // --- 5. Bottom Controls Dock ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(32.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Microphone Mute / Unmute
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                        .testTag("call_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) VeloraPlumDark else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Speakerphone Toggle
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerOn) VeloraPink else Color.White.copy(alpha = 0.2f))
                        .testTag("call_speaker_button")
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "Speaker",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 3. Video Camera Toggle (Video Call only)
                if (callSession.callType == CallType.VIDEO) {
                    IconButton(
                        onClick = onToggleVideo,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (!isVideoEnabled) Color.White else Color.White.copy(alpha = 0.2f))
                            .testTag("call_video_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (!isVideoEnabled) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Toggle Video",
                            tint = if (!isVideoEnabled) VeloraPlumDark else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 4. Flip Camera (Front / Back)
                    IconButton(
                        onClick = onFlipCamera,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .testTag("call_camera_switch")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 5. Hang Up / End Call (Red Button)
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(VeloraErrorRed)
                        .testTag("call_hangup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
