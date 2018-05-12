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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.IBinder
import androidx.core.content.systemService
import xyz.sommd.automute.settings.Settings
import xyz.sommd.automute.utils.AudioPlaybackMonitor
import xyz.sommd.automute.utils.AudioVolumeMonitor
import xyz.sommd.automute.utils.isVolumeOff
import xyz.sommd.automute.utils.log
import java.util.concurrent.TimeUnit

class AutoMuteService: Service(),
        AudioPlaybackMonitor.Listener, AudioVolumeMonitor.Listener, Settings.ChangeListener {
    
    companion object {
        const val ACTION_MUTE = "xyz.sommd.automute.action.MUTE"
        const val ACTION_UNMUTE = "xyz.sommd.automute.action.UNMUTE"
        const val ACTION_SHOW = "xyz.sommd.automute.action.SHOW"
        
        private const val DEFAULT_STREAM = AudioManager.STREAM_MUSIC
        
        fun startIfEnabled(context: Context) {
            val settings = Settings(context)
            if (settings.serviceEnabled) {
                start(context)
            }
        }
        
        fun start(context: Context) {
            context.startForegroundService(Intent(context, AutoMuteService::class.java))
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, AutoMuteService::class.java))
        }
    }
    
    private lateinit var settings: Settings
    private lateinit var notifications: Notifications
    private lateinit var audioManager: AudioManager
    
    private lateinit var handler: Handler
    private lateinit var playbackMonitor: AudioPlaybackMonitor
    private lateinit var volumeMonitor: AudioVolumeMonitor
    
    /** [Runnable] to be posted when volume should be auto muted. */
    private val autoMuteRunnable = Runnable {
        if (audioManager.isVolumeOff()) {
            log("Already muted, not auto muting")
        } else {
            log("Auto muting now")
            mute()
        }
    }
    
    /** [BroadcastReceiver] for receiving [AudioManager.ACTION_AUDIO_BECOMING_NOISY].  */
    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (settings.autoMuteHeadphonesUnplugged) {
                mute()
            }
        }
    }
    
    private val headphonesPluggedIn: Boolean
        get() = audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
    
    // Service
    
    override fun onCreate() {
        log("Starting")
        
        // Get services
        settings = Settings(this)
        notifications = Notifications(this)
        audioManager = systemService()
        
        // Setup notifications
        notifications.createChannels()
        
        // Create audio monitors
        handler = Handler()
        playbackMonitor = AudioPlaybackMonitor(this, this, handler)
        volumeMonitor = AudioVolumeMonitor(this, this, intArrayOf(DEFAULT_STREAM), handler)
        
        // Setup listeners
        settings.addChangeListener(this)
        playbackMonitor.start()
        volumeMonitor.start()
        registerReceiver(receiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        
        // Show foreground status notification
        val statusNotification = notifications.createStatusNotification()
        startForeground(Notifications.STATUS_ID, statusNotification)
    }
    
    /**
     * Handle actions sent from status notification (or other places).
     */
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
        
        // Remove listeners
        settings.removeChangeListener(this)
        playbackMonitor.stop()
        volumeMonitor.stop()
        unregisterReceiver(receiver)
        
        // Cancel scheduled auto mute
        cancelAutoMute()
    }
    
    // AudioPlaybackMonitor
    
    /**
     * Check type of audio stream that started playing and auto unmute based on user's settings.
     */
    override fun audioPlaybackStarted(config: AudioPlaybackConfiguration) {
        // Get audio type and unmute mode
        val audioType = AudioType.from(config.audioAttributes)
        val unmuteMode = getAutoUnmuteMode(audioType)
        
        log("Playback started: $audioType, $unmuteMode, ${config.audioAttributes}")
        
        if (unmuteMode != null) {
            // Always cancel auto mute, even if not auto unmuting, because audio is now playing
            cancelAutoMute()
            
            // Get stream or use DEFAULT_STREAM
            val stream = config.audioAttributes.volumeControlStream
                    .let { if (it == AudioManager.USE_DEFAULT_STREAM_TYPE) DEFAULT_STREAM else it }
            
            // Unmute stream only if volume is off
            if (audioManager.isVolumeOff(stream)) {
                when (unmuteMode) {
                    Settings.UnmuteMode.ALWAYS -> unmute(stream)
                    Settings.UnmuteMode.SHOW_UI -> showVolumeControl(stream)
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
        log("Playback stopped: ${config.audioAttributes}")
        
        if (settings.autoMuteEnabled) {
            if (settings.autoMuteHeadphonesDisabled && headphonesPluggedIn) {
                log("Headphones plugged in, not auto muting")
            } else {
                // Check if any audio types are playing that we care about
                val audioPlaying = playbackMonitor.playbackConfigs
                        .any { AudioType.from(it.audioAttributes) != AudioType.UNKNOWN }
                
                // Schedule auto mute if no audio playing
                if (!audioPlaying) {
                    val delay = TimeUnit.SECONDS.toMillis(settings.autoMuteDelay)
                    handler.postDelayed(autoMuteRunnable, delay)
                    
                    log("Audio stopped, auto mute in ${delay}ms")
                } else {
                    log("Audio still playing, not auto muting")
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
    private fun unmute(stream: Int = DEFAULT_STREAM) {
        val flags = if (settings.autoUnmuteShowUi) AudioManager.FLAG_SHOW_UI else 0
        
        // Unmute stream
        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, flags)
        
        // Set to default volume if volume is 0
        if (audioManager.isVolumeOff(stream)) {
            log("Audio unmuted to 0, setting default volume")
            
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            val volume = (settings.autoUnmuteDefaultVolume * maxVolume).toInt()
            audioManager.setStreamVolume(stream, volume, flags)
        }
        
        // Update status notification mute/unmute state
        updateStatusNotification()
    }
    
    /**
     * Mute the given audio stream.
     *
     * Also updates the status notification to show the current mute/unmute state.
     */
    private fun mute(stream: Int = DEFAULT_STREAM) {
        val flags = if (settings.autoMuteShowUi) AudioManager.FLAG_SHOW_UI else 0
        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, flags)
        
        // Update status notification mute/unmute state
        updateStatusNotification()
    }
    
    /**
     * Show the system volume control UI for the given audio stream.
     */
    private fun showVolumeControl(stream: Int = DEFAULT_STREAM) {
        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }
    
    /**
     * Cancel auto mute if scheduled.
     */
    private fun cancelAutoMute() {
        handler.removeCallbacks(autoMuteRunnable)
        log("Auto mute cancelled")
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