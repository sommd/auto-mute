package xyz.sommd.automute.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import androidx.content.systemService
import xyz.sommd.automute.settings.Settings
import xyz.sommd.automute.utils.AudioPlaybackMonitor
import xyz.sommd.automute.utils.log

class AutoMuteService: Service(), AudioPlaybackMonitor.Listener {
    enum class AudioType {
        MUSIC,
        MEDIA,
        ASSISTANT,
        GAME,
        UNKNOWN;
        
        companion object {
            fun from(audioAttributes: AudioAttributes) = when (audioAttributes.contentType) {
                AudioAttributes.CONTENT_TYPE_MUSIC -> MUSIC
                AudioAttributes.CONTENT_TYPE_MOVIE -> MEDIA
                else -> {
                    when (audioAttributes.usage) {
                        AudioAttributes.USAGE_MEDIA -> MEDIA
                        AudioAttributes.USAGE_ASSISTANT,
                        AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> ASSISTANT
                        AudioAttributes.USAGE_GAME -> GAME
                        else -> when (audioAttributes.volumeControlStream) {
                            AudioManager.STREAM_MUSIC -> MEDIA
                            else -> UNKNOWN
                        }
                    }
                }
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private lateinit var settings: Settings
    private lateinit var notifications: Notifications
    private lateinit var handler: Handler
    private lateinit var audioManager: AudioManager
    private lateinit var playbackMonitor: AudioPlaybackMonitor
    
    override fun onCreate() {
        Toast.makeText(this, "Starting Auto Mute Service", Toast.LENGTH_SHORT).show()
        
        settings = Settings.from(this)
        notifications = Notifications.from(this)
        handler = Handler()
        audioManager = systemService()
        playbackMonitor = AudioPlaybackMonitor(this, this)
        
        audioManager.registerAudioPlaybackCallback(playbackMonitor, handler)
        
        startForeground(Notifications.STATUS_ID, notifications.createStatusNotification())
    }
    
    override fun onDestroy() {
        Toast.makeText(this, "Stopping Auto Mute Service", Toast.LENGTH_SHORT).show()
        
        audioManager.unregisterAudioPlaybackCallback(playbackMonitor)
    }
    
    override fun audioPlaybackStarted(config: AudioPlaybackConfiguration) {
        val audioType = AudioType.from(config.audioAttributes)
        val unmuteMode = getAutoUnmuteMode(audioType)
        
        log("Playback started: ${config.audioAttributes}")
        log("Audio type: $audioType")
        log("Unmute mode: $unmuteMode")
        
        if (unmuteMode != null) {
            val stream = config.audioAttributes.volumeControlStream
            unmute(unmuteMode, stream)
        }
    }
    
    override fun audioPlaybackStopped(config: AudioPlaybackConfiguration) {
        log("Playback stopped: ${config.audioAttributes}")
        
        // TODO
    }
    
    private fun getAutoUnmuteMode(audioType: AudioType) = when (audioType) {
        AudioType.MUSIC -> settings.autoUnmuteMusicMode
        AudioType.MEDIA -> settings.autoUnmuteMediaMode
        AudioType.ASSISTANT -> settings.autoUnmuteAssistantMode
        AudioType.GAME -> settings.autoUnmuteGameMode
        else -> null
    }
    
    private fun unmute(mode: Settings.UnmuteMode, stream: Int) {
        val stream = if (stream == AudioManager.USE_DEFAULT_STREAM_TYPE) {
            AudioManager.STREAM_MUSIC
        } else {
            stream
        }
        
        if (audioManager.isStreamMute(stream) || audioManager.getStreamVolume(stream) == 0) {
            when (mode) {
                Settings.UnmuteMode.ALWAYS -> {
                    // Unmute stream
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE,
                                                    AudioManager.FLAG_SHOW_UI)
                    
                    // Set to default volume if volume is 0
                    if (audioManager.getStreamVolume(stream) == 0) {
                        log("Audio unmuted to 0, setting default volume")
                        
                        val maxVolume = audioManager.getStreamMaxVolume(stream)
                        val volume = (settings.autoUnmuteDefaultVolume * maxVolume).toInt()
                        audioManager.setStreamVolume(stream, volume, AudioManager.FLAG_SHOW_UI)
                    }
                }
                Settings.UnmuteMode.ASK -> {
                    // Show volume UI
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_SAME,
                                                    AudioManager.FLAG_SHOW_UI)
                }
            }
        }
    }
}