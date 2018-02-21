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
import android.widget.Toast
import androidx.content.systemService
import xyz.sommd.automute.settings.Settings
import xyz.sommd.automute.utils.AudioPlaybackMonitor
import xyz.sommd.automute.utils.log
import java.util.concurrent.TimeUnit

class AutoMuteService: Service(), AudioPlaybackMonitor.Listener, Settings.ChangeListener {
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
    
    private lateinit var settings: Settings
    private lateinit var notifications: Notifications
    private lateinit var handler: Handler
    private lateinit var audioManager: AudioManager
    private lateinit var playbackMonitor: AudioPlaybackMonitor
    
    private val autoMuteRunnable = Runnable {
        log("Auto muting now")
        
        val flags = if (settings.autoMuteShowUi) AudioManager.FLAG_SHOW_UI else 0
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, flags)
    }
    
    override fun onCreate() {
        log("Starting")
        
        settings = Settings.from(this)
        notifications = Notifications.from(this)
        handler = Handler()
        audioManager = systemService()
        playbackMonitor = AudioPlaybackMonitor(this, this)
        
        settings.addChangeListener(this)
        audioManager.registerAudioPlaybackCallback(playbackMonitor, handler)
        
        startForeground(Notifications.STATUS_ID, notifications.createStatusNotification())
    }
    
    override fun onDestroy() {
        log("Stopping")
        
        settings.removeChangeListener(this)
        audioManager.unregisterAudioPlaybackCallback(playbackMonitor)
        cancelAutoMute()
    }
    
    override fun audioPlaybackStarted(config: AudioPlaybackConfiguration) {
        val audioType = AudioType.from(config.audioAttributes)
        val unmuteMode = getAutoUnmuteMode(audioType)
        
        log("Playback started: ${config.audioAttributes}")
        log("Audio type: $audioType")
        log("Unmute mode: $unmuteMode")
        
        if (unmuteMode != null) {
            // Cancel auto mute
            cancelAutoMute()
            
            // Unmute Stream
            val stream = config.audioAttributes.volumeControlStream
            unmute(unmuteMode, stream)
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
            val flags = if (settings.autoUnmuteShowUi) AudioManager.FLAG_SHOW_UI else 0
            
            when (mode) {
                Settings.UnmuteMode.ALWAYS -> {
                    // Unmute stream
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, flags)
                    
                    // Set to default volume if volume is 0
                    if (audioManager.getStreamVolume(stream) == 0) {
                        log("Audio unmuted to 0, setting default volume")
                        
                        val maxVolume = audioManager.getStreamMaxVolume(stream)
                        val volume = (settings.autoUnmuteDefaultVolume * maxVolume).toInt()
                        audioManager.setStreamVolume(stream, volume, flags)
                    }
                }
                Settings.UnmuteMode.SHOW_UI -> {
                    // Show volume UI
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_SAME,
                                                    flags or AudioManager.FLAG_SHOW_UI)
                }
            }
        }
    }
    
    private fun cancelAutoMute() {
        handler.removeCallbacks(autoMuteRunnable)
        log("Auto mute cancelled")
    }
    
    override fun onSettingsChanged(settings: Settings, key: String) {
        when (key) {
            Settings.AUTO_MUTE_ENABLED_KEY -> {
                if (!settings.autoMuteEnabled) {
                    cancelAutoMute()
                }
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}