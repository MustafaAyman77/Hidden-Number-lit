package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Process
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class VoiceChatManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val sampleRate = 8000
    private val channelConfigRecord = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigTrack = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val minBufferSizeRecord = AudioRecord.getMinBufferSize(sampleRate, channelConfigRecord, audioFormat)
    private val minBufferSizeTrack = AudioTrack.getMinBufferSize(sampleRate, channelConfigTrack, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var recordJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerMuted = MutableStateFlow(false)
    val isSpeakerMuted: StateFlow<Boolean> = _isSpeakerMuted.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f) // 0.0 to 1.0 for UI visualizer
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    var onAudioPacketReady: ((String) -> Unit)? = null

    fun hasMicrophonePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startVoiceChat() {
        if (!hasMicrophonePermission()) {
            Log.w("VoiceChatManager", "No RECORD_AUDIO permission")
            return
        }

        if (_isRecording.value) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfigRecord,
                audioFormat,
                minBufferSizeRecord.coerceAtLeast(1024)
            )

            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfigTrack)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSizeTrack.coerceAtLeast(1024))
                .build()

            audioTrack?.play()
            audioRecord?.startRecording()
            _isRecording.value = true

            recordJob = scope.launch(Dispatchers.IO) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buffer = ByteArray(minBufferSizeRecord.coerceAtLeast(1024))
                val byteBufferStream = java.io.ByteArrayOutputStream()
                var lastSendTime = System.currentTimeMillis()

                while (_isRecording.value) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        // Calculate audio amplitude level
                        val rms = calculateRms(buffer, readSize)
                        _audioLevel.value = (rms / 32768.0).toFloat().coerceIn(0f, 1f)

                        if (!_isMuted.value) {
                            // Silence suppression gate: only accumulate if there's actual speech
                            if (rms > 700.0) {
                                byteBufferStream.write(buffer, 0, readSize)
                            }

                            val now = System.currentTimeMillis()
                            // Send accumulated audio chunk every 250ms (max 4 requests per sec)
                            if (now - lastSendTime >= 250) {
                                val pcmData = byteBufferStream.toByteArray()
                                byteBufferStream.reset()
                                lastSendTime = now

                                if (pcmData.isNotEmpty()) {
                                    val encoded = Base64.encodeToString(pcmData, 0, pcmData.size, Base64.NO_WRAP)
                                    onAudioPacketReady?.invoke(encoded)
                                }
                            }
                        } else {
                            byteBufferStream.reset()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceChatManager", "Error starting voice chat: ${e.message}")
            stopVoiceChat()
        }
    }

    fun playAudioChunk(base64Audio: String) {
        if (_isSpeakerMuted.value) return
        try {
            val audioBytes = Base64.decode(base64Audio, Base64.NO_WRAP)
            audioTrack?.write(audioBytes, 0, audioBytes.size)
        } catch (e: Exception) {
            Log.e("VoiceChatManager", "Error playing audio chunk: ${e.message}")
        }
    }

    fun toggleMute(): Boolean {
        _isMuted.value = !_isMuted.value
        return _isMuted.value
    }

    fun toggleSpeaker(): Boolean {
        _isSpeakerMuted.value = !_isSpeakerMuted.value
        return _isSpeakerMuted.value
    }

    fun stopVoiceChat() {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e("VoiceChatManager", "Error stopping record: ${e.message}")
        }

        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e("VoiceChatManager", "Error stopping track: ${e.message}")
        }

        _audioLevel.value = 0f
    }

    private fun calculateRms(buffer: ByteArray, readSize: Int): Double {
        var sum = 0.0
        var i = 0
        while (i < readSize - 1) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            sum += sample * sample
            i += 2
        }
        return sqrt(sum / (readSize / 2))
    }
}
