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

package xyz.sommd.automute.utils

import android.content.*
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.util.SparseIntArray
import androidx.core.content.systemService
import androidx.core.util.getOrDefault
import androidx.core.util.set
import xyz.sommd.automute.utils.AudioVolumeMonitor.Listener

/**
 * Class for monitoring the volume level changes of audio streams.
 *
 * Note: this class may not be completely reliable as it relies of the [Settings] [ContentProvider],
 * which may not be notified when volume levels are changed. It also won't be notified of streams
 * being muted/unmuted.
 *
 * @param context The [Context] to get the [ContentResolver] and [AudioManager] from.
 * @param listener The [Listener] to be notified of volume changes.
 * @param streams The audio streams to monitor the volume of, e.g. [AudioManager.STREAM_MUSIC].
 * @param handler The [Handler] to be passed to [ContentObserver].
 */
class AudioVolumeMonitor(private val context: Context,
                         private val listener: Listener,
                         private val streams: IntArray = ALL_STREAMS,
                         private val handler: Handler = Handler()): ContentObserver(handler) {
    
    companion object {
        private val ALL_STREAMS = intArrayOf(
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_DTMF,
                AudioManager.STREAM_ACCESSIBILITY
        )
        
        /**
         * Amount to delay before updating volume after [AudioManager.ACTION_HEADSET_PLUG]. When
         * updating immediately, volume may not have changed yet.
         */
        private const val HEADSET_PLUG_DELAY_MS = 100L
    }
    
    interface Listener {
        /**
         * Called when the volume of [stream] is changed.
         */
        fun onVolumeChange(stream: Int, volume: Int) {}
        
        /**
         * Called when audio is about to become 'noisy' due to a change in audio outputs.
         *
         * @see AudioManager.ACTION_AUDIO_BECOMING_NOISY
         */
        fun onAudioBecomingNoisy()
    }
    
    private val contentResolver = context.contentResolver
    private val audioManager = context.systemService<AudioManager>()
    
    /** Previous stream volumes to keep track of which volumes changed. */
    private val streamVolumes = SparseIntArray()
    
    /**
     * [BroadcastReceiver] for receiving [AudioManager.ACTION_HEADSET_PLUG] and
     * [AudioManager.ACTION_AUDIO_BECOMING_NOISY].
     */
    private val receiver = object: BroadcastReceiver() {
        private val updateRunnable = Runnable {
            updateVolumes(true)
        }
        
        private val noisyRunnable = Runnable {
            listener.onAudioBecomingNoisy()
        }
        
        override fun onReceive(context: Context, intent: Intent) {
            this@AudioVolumeMonitor.log("Audio output changing")
            
            // Notify audio becoming noisy
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                this@AudioVolumeMonitor.log("Audio becoming noisy")
                
                handler.post(noisyRunnable)
            }
            
            // Cancel existing runnable to prevent unnecessary updates
            handler.removeCallbacks(updateRunnable)
            // Update volumes and notify
            handler.postDelayed(updateRunnable, HEADSET_PLUG_DELAY_MS)
        }
    }
    
    /**
     * Register this [AudioVolumeMonitor] to start monitor for stream volume changes.
     *
     * @param notifyNow If `true`, the [Listener] will be notified of the current stream volumes.
     * Note: the [Listener] will be called on the current [Thread], not via the [Handler].
     */
    fun start(notifyNow: Boolean = false) {
        updateVolumes(notifyNow)
        
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, this)
        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        })
    }
    
    /**
     * Stop monitoring stream volume changes.
     */
    fun stop() {
        contentResolver.unregisterContentObserver(this)
        context.unregisterReceiver(receiver)
        
        streamVolumes.clear()
    }
    
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        log("Volume changed by user")
        
        updateVolumes(true)
    }
    
    private fun updateVolumes(notify: Boolean) {
        // Get volume of each stream
        for (stream in streams) {
            val volume = audioManager.getStreamVolume(stream)
            
            // Update volume if previous value is different or unknown
            if (volume != streamVolumes.getOrDefault(stream, -1)) {
                streamVolumes[stream] = volume
                
                if (notify) {
                    listener.onVolumeChange(stream, volume)
                }
            }
        }
    }
    
    override fun onChange(selfChange: Boolean) = onChange(selfChange, null)
}