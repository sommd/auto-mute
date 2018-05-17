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
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.IBinder
import xyz.sommd.automute.di.Injection
import xyz.sommd.automute.settings.Settings
import xyz.sommd.automute.utils.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AutoMuteService: Service(),
        AudioPlaybackMonitor.Listener, AudioVolumeMonitor.Listener, Settings.ChangeListener {
    
    companion object {
        const val ACTION_MUTE = "xyz.sommd.automute.action.MUTE"
        const val ACTION_UNMUTE = "xyz.sommd.automute.action.UNMUTE"
        const val ACTION_SHOW = "xyz.sommd.automute.action.SHOW"
        
        /** Extra to signal that the service was started on boot by [AutoMuteService]. */
        const val EXTRA_BOOT = "xyz.sommd.automute.extra.BOOT"
        
        fun start(context: Context, boot: Boolean = false) {
            context.startForegroundService(Intent(context, AutoMuteService::class.java)
                                                   .putExtra(EXTRA_BOOT, boot))
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, AutoMuteService::class.java))
        }
    }
    
    @Inject
    lateinit var settings: Settings
    @Inject
    lateinit var notifications: Notifications
    
    @Inject
    lateinit var audioManager: AudioManager
    @Inject
    lateinit var handler: Handler
    
    @Inject
    lateinit var playbackMonitor: AudioPlaybackMonitor
    @Inject
    lateinit var volumeMonitor: AudioVolumeMonitor
    
    /** [Runnable] to be posted when volume should be auto muted. */
    private val autoMuteRunnable = Runnable {
        if (audioManager.isVolumeOff()) {
            log { "Already muted, not auto muting" }
        } else {
            log { "Auto muting now" }
            mute()
        }
    }
    
    // Service
    
    override fun onCreate() {
        log { "Starting" }
        
        Injection.inject(this)
        
        // Setup notifications
        notifications.createChannels()
        
        // Setup settings
        settings.addChangeListener(this)
        
        // Start monitors
        playbackMonitor.addListener(this)
        volumeMonitor.addListener(this, STREAM_DEFAULT)
        
        // Show foreground status notification
        val statusNotification = notifications.createStatusNotification()
        startForeground(Notifications.STATUS_ID, statusNotification)
    }
    
    /**
     * Handle actions sent from status notification (or other places) or mute on boot.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.getBooleanExtra(EXTRA_BOOT, false) && settings.autoMuteEnabled) {
            if (settings.autoMuteHeadphonesDisabled && audioManager.areHeadphonesPluggedIn) {
                log { "Not muting on boot, headphones plugged in" }
            } else {
                log { "Muting on boot" }
                
                mute()
            }
        } else if (intent.action != null) {
            log { "Received command: ${intent.action}" }
            
            when (intent.action) {
                ACTION_MUTE -> mute()
                ACTION_UNMUTE -> unmute()
                ACTION_SHOW -> audioManager.showVolumeControl()
            }
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        log { "Stopping" }
        
        // Remove listeners
        settings.removeChangeListener(this)
        playbackMonitor.removeListener(this)
        volumeMonitor.removeListener(this)
        
        // Cancel scheduled auto mute
        cancelAutoMute()
    }
    
    // AudioPlaybackMonitor
    
    /**
     * Check type of audio stream that started playing and auto unmute based on user's settings.
     */
    override fun audioPlaybackStarted(config: AudioPlaybackConfiguration) {
        // Get audio type and unmute mode
        val audioType = config.audioAttributes.audioType
        val unmuteMode = getAutoUnmuteMode(audioType)
        
        log { "Playback started: $audioType, $unmuteMode, ${config.audioAttributes}" }
        
        if (unmuteMode != null) {
            // Always cancel auto mute, even if not auto unmuting, because audio is now playing
            cancelAutoMute()
            
            // Get stream or use STREAM_DEFAULT
            val stream = config.audioAttributes.volumeControlStream
                    .let { if (it == AudioManager.USE_DEFAULT_STREAM_TYPE) STREAM_DEFAULT else it }
            
            // Unmute stream only if volume is off
            if (audioManager.isVolumeOff(stream)) {
                when (unmuteMode) {
                    Settings.UnmuteMode.ALWAYS -> unmute(stream)
                    Settings.UnmuteMode.SHOW_UI -> audioManager.showVolumeControl(stream)
                }
            }
        }
    }
    
    /**
     * Get the user's auto unmute setting for the given [AudioType].
     */
    private fun getAutoUnmuteMode(audioType: AudioType) = when (audioType) {
        AudioType.MUSIC -> settings.autoUnmuteMusicMode
        AudioType.MEDIA -> settings.autoUnmuteMediaMode
        AudioType.ASSISTANT -> settings.autoUnmuteAssistantMode
        AudioType.GAME -> settings.autoUnmuteGameMode
        else -> null
    }
    
    /**
     * Auto mute based on user's settings if there are no more audio streams playing.
     */
    override fun audioPlaybackStopped(config: AudioPlaybackConfiguration) {
        log { "Playback stopped: ${config.audioAttributes}" }
        
        if (settings.autoMuteEnabled) {
            if (settings.autoMuteHeadphonesDisabled && audioManager.areHeadphonesPluggedIn) {
                log { "Headphones plugged in, not auto muting" }
            } else {
                // Check if any audio types are playing that we care about
                val audioPlaying = playbackMonitor.playbackConfigs
                        .any { it.audioAttributes.audioType != AudioType.UNKNOWN }
                
                // Schedule auto mute if no audio playing
                if (!audioPlaying) {
                    val delay = TimeUnit.SECONDS.toMillis(settings.autoMuteDelay)
                    handler.postDelayed(autoMuteRunnable, delay)
                    
                    log { "Audio stopped, auto mute in ${delay}ms" }
                } else {
                    log { "Audio still playing, not auto muting" }
                }
            }
        }
    }
    
    /**
     * Updates the status notification to show the currently playing audio streams.
     */
    override fun audioPlaybackChanged(configs: List<AudioPlaybackConfiguration>) {
        updateStatusNotification()
    }
    
    // AudioVolumeMonitor
    
    /**
     * Updates the status notification to show mute/unmute state in case user manually mutes/unmutes
     * the volume.
     */
    override fun onVolumeChange(stream: Int, volume: Int) {
        updateStatusNotification()
    }
    
    /**
     * Mutes volume if [Settings.autoMuteHeadphonesUnplugged] is enabled.
     */
    override fun onAudioBecomingNoisy() {
        if (settings.autoMuteHeadphonesUnplugged) {
            log { "Headphones unplugged, muting" }
            
            mute()
        }
    }
    
    // Settings
    
    /**
     * Cancel scheduled auto mute if auto mute was disabled by user.
     */
    override fun onSettingsChanged(settings: Settings, key: String) {
        when (key) {
            Settings.AUTO_MUTE_ENABLED_KEY -> {
                if (!settings.autoMuteEnabled) {
                    cancelAutoMute()
                }
            }
        }
    }
    
    // Methods
    
    /**
     * Unmute the given audio stream.
     *
     * If unmuting left the stream volume at 0 (which is likely if
     * the user manually muted via the system volume controls), then set stream's volume to the
     * user's default volume setting.
     *
     * Also updates the status notification to show the current mute/unmute state.
     */
    private fun unmute(stream: Int = STREAM_DEFAULT) {
        audioManager.unmute(settings.autoUnmuteDefaultVolume, stream, settings.autoUnmuteShowUi)
        
        // Update status notification mute/unmute state
        updateStatusNotification()
    }
    
    /**
     * Mute the given audio stream.
     *
     * Also updates the status notification to show the current mute/unmute state.
     */
    private fun mute(stream: Int = STREAM_DEFAULT) {
        audioManager.mute(stream, settings.autoMuteShowUi)
        
        // Update status notification mute/unmute state
        updateStatusNotification()
    }
    
    /**
     * Cancel auto mute if scheduled.
     */
    private fun cancelAutoMute() {
        handler.removeCallbacks(autoMuteRunnable)
        log { "Auto mute cancelled" }
    }
    
    /**
     * Update the status notification.
     */
    private fun updateStatusNotification() {
        notifications.updateStatusNotification()
    }
    
    // Unused
    
    override fun onBind(intent: Intent?): IBinder? = null
}