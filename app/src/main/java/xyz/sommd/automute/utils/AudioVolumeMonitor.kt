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

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.util.SparseIntArray
import androidx.content.systemService
import androidx.util.getOrDefault
import androidx.util.set
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
class AudioVolumeMonitor(context: Context,
                         private val listener: Listener,
                         private val streams: IntArray = ALL_STREAMS,
                         handler: Handler? = null): ContentObserver(handler) {
    
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
    }
    
    interface Listener {
        /**
         * Called when the volume of [stream] is changed.
         */
        fun onVolumeChange(stream: Int, volume: Int) {}
    }
    
    private val contentResolver = context.contentResolver
    private val audioManager = context.systemService<AudioManager>()
    
    /** Previous stream volumes to keep track of which volumes changed. */
    private val streamVolumes = SparseIntArray()
    
    /**
     * Register this [AudioVolumeMonitor] to start monitor for stream volume changes.
     *
     * @param notifyNow If `true`, the [Listener] will be notified of the current stream volumes.
     * Note: the [Listener] will be called on the current [Thread], not via the [Handler].
     */
    fun start(notifyNow: Boolean = false) {
        updateVolumes(notifyNow)
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, this)
    }
    
    /**
     * Stop monitoring stream volume changes.
     */
    fun stop() {
        contentResolver.unregisterContentObserver(this)
        streamVolumes.clear()
    }
    
    override fun onChange(selfChange: Boolean, uri: Uri?) {
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