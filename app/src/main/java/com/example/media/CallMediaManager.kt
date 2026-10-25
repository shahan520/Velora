package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Handles Audio Routing, Speakerphone, Microphone Muting, CameraX Video Preview,
 * and Incoming Call Ringtone/Vibration for Real-Time Calls.
 */
class CallMediaManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var ringtone: Ringtone? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var currentLensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var isSpeakerphoneActive: Boolean = true

    init {
        initRingtone()
    }

    private fun initRingtone() {
        try {
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(context, ringUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
        } catch (e: Exception) {
            Log.w("CallMediaManager", "Unable to init ringtone: ${e.message}")
        }
    }

    /**
     * Prepares audio subsystem for real-time communication.
     */
    fun startCallAudio(enableSpeaker: Boolean) {
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = enableSpeaker
                isSpeakerphoneActive = enableSpeaker
                am.isMicrophoneMute = false
                stopRingtone()
            } catch (e: Exception) {
                Log.w("CallMediaManager", "startCallAudio error: ${e.message}")
            }
        }
    }

    /**
     * Toggles microphone mute state.
     */
    fun setMicrophoneMute(isMuted: Boolean) {
        audioManager?.let { am ->
            try {
                am.isMicrophoneMute = isMuted
            } catch (e: Exception) {
                Log.w("CallMediaManager", "setMicrophoneMute error: ${e.message}")
            }
        }
    }

    /**
     * Toggles speakerphone on or off.
     */
    fun setSpeakerphoneOn(isOn: Boolean) {
        audioManager?.let { am ->
            try {
                am.isSpeakerphoneOn = isOn
                isSpeakerphoneActive = isOn
            } catch (e: Exception) {
                Log.w("CallMediaManager", "setSpeakerphoneOn error: ${e.message}")
            }
        }
    }

    /**
     * Plays incoming call ringtone and triggers vibration pattern.
     */
    fun startRinging() {
        try {
            if (ringtone?.isPlaying != true) {
                ringtone?.play()
            }
            triggerVibration()
        } catch (e: Exception) {
            Log.w("CallMediaManager", "startRinging error: ${e.message}")
        }
    }

    /**
     * Stops ringing and vibration.
     */
    fun stopRinging() {
        stopRingtone()
        stopVibration()
    }

    private fun stopRingtone() {
        try {
            if (ringtone?.isPlaying == true) {
                ringtone?.stop()
            }
        } catch (e: Exception) {
            Log.w("CallMediaManager", "stopRingtone error: ${e.message}")
        }
    }

    private fun triggerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 800, 1000, 800, 1000)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(1000)
            }
        } catch (_: Exception) {}
    }

    private fun stopVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    /**
     * Binds CameraX PreviewView to show local self camera feed.
     */
    fun startCameraPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
        onCameraReady: () -> Unit = {}
    ) {
        currentLensFacing = lensFacing
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(currentLensFacing)
                    .build()

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                onCameraReady()
            } catch (e: Exception) {
                Log.w("CallMediaManager", "startCameraPreview failed: ${e.message}")
                // Fallback to whatever camera is available
                try {
                    val fallbackSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(lifecycleOwner, fallbackSelector, preview)
                    onCameraReady()
                } catch (fallbackError: Exception) {
                    Log.w("CallMediaManager", "fallback camera failed: ${fallbackError.message}")
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Flips camera between front and back.
     */
    fun flipCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val nextFacing = if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        startCameraPreview(lifecycleOwner, previewView, nextFacing)
    }

    /**
     * Stops camera preview.
     */
    fun stopCameraPreview() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w("CallMediaManager", "stopCameraPreview error: ${e.message}")
        }
    }

    /**
     * Cleans up audio mode and camera on call conclusion.
     */
    fun cleanup() {
        stopRinging()
        stopCameraPreview()
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_NORMAL
                am.isSpeakerphoneOn = false
                am.isMicrophoneMute = false
            } catch (_: Exception) {}
        }
    }
}
