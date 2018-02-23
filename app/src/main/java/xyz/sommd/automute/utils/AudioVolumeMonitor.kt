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

class AudioVolumeMonitor(context: Context,
                         private val listener: Listener,
                         private val streams: IntArray = ALL_STREAMS,
                         handler: Handler = Handler()): ContentObserver(handler) {
    
    companion object {
        val ALL_STREAMS = intArrayOf(
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
        fun onVolumeChange(stream: Int, volume: Int) {}
    }
    
    private val contentResolver = context.contentResolver
    private val audioManager = context.systemService<AudioManager>()
    
    private val streamVolumes = SparseIntArray()
    
    fun start(notifyNow: Boolean = false) {
        updateVolumes(notifyNow)
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, this)
    }
    
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