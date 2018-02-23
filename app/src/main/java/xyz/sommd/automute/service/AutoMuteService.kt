/*
 * Copyright (C) 2018 David Sommerich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.sommd.automute.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.IBinder
import androidx.content.systemService
import xyz.sommd.automute.settings.Settings
import xyz.sommd.automute.utils.AudioPlaybackMonitor
import xyz.sommd.automute.utils.AudioVolumeMonitor
import xyz.sommd.automute.utils.log
import java.util.concurrent.TimeUnit

class AutoMuteService: Service(),
        AudioPlaybackMonitor.Listener, AudioVolumeMonitor.Listener, Settings.ChangeListener {
    
    enum class AudioType {
        MUSIC,
        MEDIA,
        ASSISTANT,
        GAME,
        UNKNOWN;
        
        companion object {
            fun from(audioAttributes: AudioAttributes) = when (audioAttributes.usage) {
                AudioAttributes.USAGE_GAME -> GAME
                AudioAttributes.USAGE_ASSISTANT,
                AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> ASSISTANT
                AudioAttributes.USAGE_MEDIA -> when (audioAttributes.contentType) {
                    AudioAttributes.CONTENT_TYPE_MUSIC -> MUSIC
                    else -> MEDIA
                }
                AudioAttributes.USAGE_UNKNOWN -> when (audioAttributes.contentType) {
                    AudioAttributes.CONTENT_TYPE_MUSIC -> MUSIC
                    AudioAttributes.CONTENT_TYPE_MOVIE -> MEDIA
                    else -> UNKNOWN
                }
                else -> UNKNOWN
            }
        }
    }
    
    companion object {
        const val ACTION_MUTE = "xyz.sommd.automute.action.MUTE"
        const val ACTION_UNMUTE = "xyz.sommd.automute.action.UNMUTE"
        const val ACTION_SHOW = "xyz.sommd.automute.action.SHOW"
        
        private const val DEFAULT_STREAM = AudioManager.STREAM_MUSIC
    }
    
    private lateinit var settings: Settings
    private lateinit var notifications: Notifications
    private lateinit var handler: Handler
    private lateinit var audioManager: AudioManager
    private lateinit var playbackMonitor: AudioPlaybackMonitor
    private lateinit var volumeMonitor: AudioVolumeMonitor
    
    private val autoMuteRunnable = Runnable {
        if (isVolumeOff()) {
            log("Already muted, not auto muting")
        } else {
            log("Auto muting now")
            mute()
        }
    }
    
    // Service
    
    override fun onCreate() {
        log("Starting")
        
        settings = Settings.from(this)
        notifications = Notifications.from(this)
        handler = Handler()
        audioManager = systemService()
        playbackMonitor = AudioPlaybackMonitor(this)
        volumeMonitor = AudioVolumeMonitor(this, this, intArrayOf(AudioManager.STREAM_MUSIC))
        
        settings.addChangeListener(this)
        audioManager.registerAudioPlaybackCallback(playbackMonitor, handler)
        volumeMonitor.start()
        
        val statusNotification = notifications.createStatusNotification(isVolumeOff())
        startForeground(Notifications.STATUS_ID, statusNotification)
    }
    
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        log("Received command: ${intent.action}")
        
        when (intent.action) {
            ACTION_MUTE -> mute()
            ACTION_UNMUTE -> unmute()
            ACTION_SHOW -> showVolumeControl()
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        log("Stopping")
        
        settings.removeChangeListener(this)
        audioManager.unregisterAudioPlaybackCallback(playbackMonitor)
        volumeMonitor.stop()
        cancelAutoMute()
    }
    
    // AudioPlaybackMonitor
    
    override fun audioPlaybackStarted(config: AudioPlaybackConfiguration) {
        val audioType = AudioType.from(config.audioAttributes)
        val unmuteMode = getAutoUnmuteMode(audioType)
        
        log("Playback started: $audioType, $unmuteMode, ${config.audioAttributes}")
        
        if (unmuteMode != null) {
            // Cancel auto mute
            cancelAutoMute()
            
            // Unmute Stream
            val stream = config.audioAttributes.volumeControlStream
            autoUnmute(unmuteMode, stream)
        }
    }
    
    override fun audioPlaybackStopped(config: AudioPlaybackConfiguration) {
        log("Playback stopped: ${config.audioAttributes}")
        
        if (settings.autoMuteEnabled) {
            val audioPlaying = playbackMonitor.playbackConfigs
                    .any { AudioType.from(it.audioAttributes) != AudioType.UNKNOWN }
            
            if (!audioPlaying) {
                // Schedule auto mute
                val delay = TimeUnit.SECONDS.toMillis(settings.autoMuteDelay)
                handler.postDelayed(autoMuteRunnable, delay)
                
                log("Audio stopped, auto mute in ${delay}ms")
            } else {
                log("Audio still playing, not auto muting")
            }
        }
    }
    
    override fun audioPlaybackChanged(configs: List<AudioPlaybackConfiguration>) {
        updateStatusNotification()
    }
    
    // AudioVolumeMonitor
    
    override fun onVolumeChange(stream: Int, volume: Int) {
        updateStatusNotification()
    }
    
    // Methods
    
    private fun getAutoUnmuteMode(audioType: AudioType) = when (audioType) {
        AudioType.MUSIC -> settings.autoUnmuteMusicMode
        AudioType.MEDIA -> settings.autoUnmuteMediaMode
        AudioType.ASSISTANT -> settings.autoUnmuteAssistantMode
        AudioType.GAME -> settings.autoUnmuteGameMode
        else -> null
    }
    
    private fun autoUnmute(mode: Settings.UnmuteMode, stream: Int) {
        // Use STREAM_MUSIC if stream is USE_DEFAULT_STREAM_TYPE
        val stream = if (stream == AudioManager.USE_DEFAULT_STREAM_TYPE) {
            DEFAULT_STREAM
        } else {
            stream
        }
        
        // Only autoUnmute if volume is off
        if (isVolumeOff(stream)) {
            when (mode) {
                Settings.UnmuteMode.ALWAYS -> unmute(stream)
                Settings.UnmuteMode.SHOW_UI -> showVolumeControl(stream)
            }
        }
    }
    
    private fun unmute(stream: Int = DEFAULT_STREAM) {
        val flags = if (settings.autoUnmuteShowUi) AudioManager.FLAG_SHOW_UI else 0
        
        // Unmute stream
        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, flags)
        
        // Set to default volume if volume is 0
        if (isVolumeOff(stream)) {
            log("Audio unmuted to 0, setting default volume")
            
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            val volume = (settings.autoUnmuteDefaultVolume * maxVolume).toInt()
            audioManager.setStreamVolume(stream, volume, flags)
        }
        
        updateStatusNotification()
    }
    
    private fun mute(stream: Int = DEFAULT_STREAM) {
        val flags = if (settings.autoMuteShowUi) AudioManager.FLAG_SHOW_UI else 0
        audioManager.adjustStreamVolume(DEFAULT_STREAM, AudioManager.ADJUST_MUTE, flags)
        
        updateStatusNotification()
    }
    
    private fun showVolumeControl(stream: Int = DEFAULT_STREAM) {
        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }
    
    private fun isVolumeOff(stream: Int = AudioManager.STREAM_MUSIC): Boolean {
        return audioManager.isStreamMute(stream) || audioManager.getStreamVolume(stream) == 0
    }
    
    private fun cancelAutoMute() {
        handler.removeCallbacks(autoMuteRunnable)
        log("Auto mute cancelled")
    }
    
    private fun updateStatusNotification() {
        notifications.updateStatusNotification(isVolumeOff(), playbackMonitor.playbackConfigs)
    }
    
    // Settings
    
    override fun onSettingsChanged(settings: Settings, key: String) {
        when (key) {
            Settings.AUTO_MUTE_ENABLED_KEY -> {
                if (!settings.autoMuteEnabled) {
                    cancelAutoMute()
                }
            }
        }
    }
    
    // Unused
    
    override fun onBind(intent: Intent?): IBinder? = null
}