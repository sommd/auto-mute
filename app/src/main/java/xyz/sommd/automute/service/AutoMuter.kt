/*
 * Copyright (C) 2024 Dana Sommerich
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

import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import xyz.sommd.automute.settings.Settings
import xyz.sommd.automute.utils.*
import xyz.sommd.automute.utils.monitors.AudioOutputMonitor
import xyz.sommd.automute.utils.monitors.AudioPlaybackMonitor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AutoMuter @Inject constructor(
    private val audioManager: AudioManager,
    private val playbackMonitor: AudioPlaybackMonitor,
    private val outputMonitor: AudioOutputMonitor,
    private val settings: Settings,
    private val handler: Handler
): AudioPlaybackMonitor.Listener, AudioOutputMonitor.Listener {
    interface Listener {
        /**
         * Called when volume [stream] muted by [AutoMuter], either automatically or by [mute].
         */
        fun onMuted(stream: Int)
        
        /**
         * Called when volume [stream] unmuted by [AutoMuter], either automatically or by [unmute].
         */
        fun onUnmuted(stream: Int)
    }
    
    private val listeners = mutableListOf<Listener>()
    
    private val autoMuteRunnable = Runnable { autoMute() }
    
    /**
     * Start monitoring for playback changes and auto muting/unmuting.
     */
    fun start() {
        // Start listening
        playbackMonitor.addListener(this)
        outputMonitor.addListener(this)
    }
    
    /**
     * Stop monitoring for playback changes and auto muting/unmuting.
     */
    fun stop() {
        // Stop listening
        playbackMonitor.removeListener(this)
        outputMonitor.removeListener(this)
        
        // Cancel scheduled auto mute
        cancelAutoMute()
    }
    
    fun addListener(listener: Listener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }
    
    /**
     * Unconditionally mute the given [stream] with the user's auto mute settings.
     */
    fun mute(stream: Int = STREAM_DEFAULT) {
        audioManager.mute(show = settings.autoMuteShowUi)
        listeners.forEach { it.onMuted(stream) }
    }
    
    /**
     * Unconditionally unmute the given [stream] with the user's auto unmute settings.
     */
    fun unmute(stream: Int = STREAM_DEFAULT) {
        // Unmute
        audioManager.unmute(
            stream,
            defaultVolume = settings.autoUnmuteDefaultVolume,
            maximumVolume = settings.autoUnmuteMaximumVolume,
            settings.autoUnmuteShowUi
        )
        
        // Notify listeners
        listeners.forEach { it.onUnmuted(stream) }
    }
    
    /**
     * Called on boot to mute the default volume stream depending on the user's settings.
     */
    fun onBoot() {
        if (!settings.autoMuteEnabled) {
            log { "Auto mute disabled, not muting on boot" }
        } else if (settings.autoMuteHeadphonesDisabled && outputMonitor.isOutputExternal) {
            log { "Headphones plugged in, not muting on boot" }
        } else {
            log { "Muting on boot" }
            
            audioManager.mute()
        }
    }
    
    // Auto unmute
    
    override fun onAudioPlaybackStarted(config: AudioPlaybackConfiguration) {
        // Get audio type
        val audioAttr = config.audioAttributes
        val audioType = audioAttr.audioType
        
        log { "Playback started (type=$audioType, attr=$audioAttr)" }
        
        // Cancel scheduled auto mute if audio we care about is now playing
        if (audioType != AudioType.OTHER) {
            cancelAutoMute()
        }
        
        // Get unmute mode
        val unmuteMode = getUnmuteMode(audioType)
        
        // Get volume stream or use STREAM_DEFAULT if not specified
        val stream = audioAttr.volumeControlStream.let { stream ->
            if (stream == AudioManager.USE_DEFAULT_STREAM_TYPE) STREAM_DEFAULT else stream
        }
        
        // Auto unmute
        autoUnmute(unmuteMode, stream)
    }
    
    private fun getUnmuteMode(audioType: AudioType) = when (audioType) {
        AudioType.MUSIC -> settings.autoUnmuteMusicMode
        AudioType.MEDIA -> settings.autoUnmuteMediaMode
        AudioType.ASSISTANT -> settings.autoUnmuteAssistantMode
        AudioType.GAME -> settings.autoUnmuteGameMode
        AudioType.OTHER -> Settings.UnmuteMode.NEVER
    }
    
    private fun autoUnmute(unmuteMode: Settings.UnmuteMode, stream: Int = STREAM_DEFAULT) {
        // Only unmute if volume is off
        if (audioManager.isVolumeOff(stream)) {
            when (unmuteMode) {
                Settings.UnmuteMode.ALWAYS -> {
                    log { "Unmute mode $unmuteMode, unmuting" }
                    
                    unmute(stream)
                }
                Settings.UnmuteMode.SHOW_UI -> {
                    log { "Unmute mode $unmuteMode, showing control" }
                    
                    audioManager.showVolumeControl(stream)
                }
                Settings.UnmuteMode.NEVER -> log { "Unmute mode $unmuteMode, ignoring" }
            }
        } else {
            log { "Volume already on, ignoring" }
        }
    }
    
    // Auto mute
    
    override fun onAudioPlaybackStopped(config: AudioPlaybackConfiguration) {
        log { "Playback stopped (type=${config.audioAttributes.audioType}, attr=${config.audioAttributes})" }
        
        if (!settings.autoMuteEnabled) {
            log { "Auto mute disabled, not auto muting" }
        } else if (settings.autoMuteHeadphonesDisabled && outputMonitor.isOutputExternal) {
            log { "Audio output external, not auto muting" }
        } else {
            // Check if any audio types are playing that we care about
            val audioPlaying = playbackMonitor.playbackConfigs
                .any { it.audioAttributes.audioType != AudioType.OTHER }
            
            // Schedule auto mute if no audio playing
            if (!audioPlaying) {
                log { "Audio stopped, scheduling auto mute" }
                
                scheduleAutoMute()
            } else {
                log { "Audio still playing, not auto muting" }
            }
        }
    }
    
    override fun onAudioOutputInternal() {
        if (settings.autoMuteHeadphonesUnplugged) {
            log { "Audio output internal, muting" }
            
            mute()
        }
    }
    
    override fun onAudioOutputExternal() {
        if (settings.autoUnmuteHeadphonesPluggedIn) {
            log { "Audio output external, unmuting" }
            
            unmute()
        }
    }
    
    private fun scheduleAutoMute() {
        val delay = TimeUnit.SECONDS.toMillis(settings.autoMuteDelay)
        handler.postDelayed(autoMuteRunnable, delay)
        
        log { "Scheduled auto mute in ${delay}ms" }
    }
    
    private fun cancelAutoMute() {
        handler.removeCallbacks(autoMuteRunnable)
        
        log { "Cancelled scheduled auto mute" }
    }
    
    private fun autoMute() {
        if (settings.autoMuteEnabled) {
            log { "Auto muting now" }
            
            mute()
        } else {
            log { "Auto mute was disabled, not auto muting" }
        }
    }
}