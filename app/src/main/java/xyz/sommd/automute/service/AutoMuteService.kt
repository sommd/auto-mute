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
import android.os.IBinder
import xyz.sommd.automute.di.Injection
import xyz.sommd.automute.utils.STREAM_DEFAULT
import xyz.sommd.automute.utils.log
import xyz.sommd.automute.utils.monitors.AudioPlaybackMonitor
import xyz.sommd.automute.utils.monitors.AudioVolumeMonitor
import xyz.sommd.automute.utils.showVolumeControl
import javax.inject.Inject

class AutoMuteService: Service(), AudioPlaybackMonitor.Listener, AudioVolumeMonitor.Listener,
    AutoMuter.Listener {
    companion object {
        const val ACTION_MUTE = "xyz.sommd.automute.action.MUTE"
        const val ACTION_UNMUTE = "xyz.sommd.automute.action.UNMUTE"
        const val ACTION_SHOW = "xyz.sommd.automute.action.SHOW"
        
        /** Extra to signal that the service was started on boot by [AutoStartReceiver]. */
        const val EXTRA_BOOT = "xyz.sommd.automute.extra.BOOT"
        
        fun start(context: Context, boot: Boolean = false) {
            context.startForegroundService(
                Intent(context, AutoMuteService::class.java).apply {
                    putExtra(EXTRA_BOOT, boot)
                }
            )
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, AutoMuteService::class.java))
        }
    }
    
    @Inject
    lateinit var notifications: Notifications
    
    @Inject
    lateinit var audioManager: AudioManager
    
    @Inject
    lateinit var playbackMonitor: AudioPlaybackMonitor
    @Inject
    lateinit var volumeMonitor: AudioVolumeMonitor
    
    @Inject
    lateinit var autoMuter: AutoMuter
    
    // Service
    
    override fun onCreate() {
        log { "Starting" }
        
        Injection.inject(this)
        
        // Setup notifications
        notifications.createChannels()
        
        // Start monitors
        playbackMonitor.addListener(this)
        volumeMonitor.addListener(this, STREAM_DEFAULT)
        autoMuter.addListener(this)
        
        // Show foreground status notification
        val statusNotification = notifications.createStatusNotification()
        startForeground(Notifications.STATUS_ID, statusNotification)
        
        // Start auto muter
        autoMuter.start()
    }
    
    /**
     * Handle actions sent from status notification (or other places) or mute on boot.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.getBooleanExtra(EXTRA_BOOT, false)) {
                autoMuter.onBoot()
            } else if (intent.action != null) {
                log { "Received command: ${intent.action}" }
        
                when (intent.action) {
                    ACTION_MUTE -> autoMuter.mute()
                    ACTION_UNMUTE -> autoMuter.unmute()
                    ACTION_SHOW -> audioManager.showVolumeControl()
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        log { "Stopping" }
        
        // Remove listeners
        playbackMonitor.removeListener(this)
        volumeMonitor.removeListener(this)
        autoMuter.removeListener(this)
        
        // Stop auto muter
        autoMuter.stop()
    }
    
    // AutoMuter
    
    override fun onMuted(stream: Int) {
        updateStatusNotification()
    }
    
    override fun onUnmuted(stream: Int) {
        updateStatusNotification()
    }
    
    // AudioPlaybackMonitor
    
    /**
     * Updates the status notification to show the currently playing audio streams.
     */
    override fun onAudioPlaybacksChanged(configs: Set<AudioPlaybackConfiguration>) {
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
     * Update the status notification.
     */
    private fun updateStatusNotification() {
        notifications.updateStatusNotification()
    }
    
    // Unused
    
    override fun onBind(intent: Intent?): IBinder? = null
}