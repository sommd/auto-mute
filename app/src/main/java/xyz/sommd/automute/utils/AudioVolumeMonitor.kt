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
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.SparseArray
import android.util.SparseIntArray
import androidx.core.content.systemService
import androidx.core.util.*
import javax.inject.Inject

/**
 * Class for monitoring the volume level changes of audio streams.
 *
 * Note: this class may not be completely reliable as it relies on the [Settings.System]
 * [ContentProvider], which may not be notified when volume levels are changed. It also won't be
 * notified of streams being muted/unmuted.
 *
 * @param context The [Context] to use.
 * @param streams The audio streams to monitor the volume of, e.g. [AudioManager.STREAM_MUSIC].
 * @param handler The [Handler] for the thread on which to execute listeners.
 */
class AudioVolumeMonitor @Inject constructor(
        private val context: Context,
        private val handler: Handler = Handler(Looper.getMainLooper())
) {
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
        // TODO: Remove
        @Deprecated("Not supported any more")
        fun onAudioBecomingNoisy()
    }
    
    companion object {
        /** All available audio streams. */
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
    
    private val audioManager = context.systemService<AudioManager>()
    private val resolver = context.contentResolver
    
    /** Previous stream volumes to keep track of which volumes changed. */
    private val streamVolumes = SparseIntArray()
    
    /**
     * [Listener]s to be notified of volume changes for each stream. Streams are added and removed
     * as listeners are added and removed so we know which streams we need to track the changes of.
     */
    private val streamListeners = SparseArray<MutableList<Listener>>()
    
    /** [Uri]s for volume settings in [Settings.System]. */
    private val volumeUris: List<Uri> = mutableListOf<Uri>().apply {
        resolver.query(Settings.System.CONTENT_URI, arrayOf("name"), null, null).use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0)
                if ("volume" in name) {
                    add(Settings.System.getUriFor(name))
                }
            }
        }
    }
    
    /** [ContentObserver] for monitoring [Settings.System]. */
    private val contentObserver = object: ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            this@AudioVolumeMonitor.log { "Volume setting changed: $uri" }
            
            updateVolumes()
        }
    }
    
    /** [AudioDeviceCallback] to monitor for [AudioDeviceInfo] changes. */
    private val deviceCallback = object: AudioDeviceCallback() {
        override fun onAudioDevicesAdded(devices: Array<AudioDeviceInfo>) {
            this@AudioVolumeMonitor.log { "Devices added: ${devices.map { it.productName }}" }
            devicesUpdated(devices)
        }
        
        override fun onAudioDevicesRemoved(devices: Array<AudioDeviceInfo>) {
            this@AudioVolumeMonitor.log { "Devices removed: ${devices.map { it.productName }}" }
            devicesUpdated(devices)
        }
    }
    
    /**
     * Add a [Listener] to be notified of volume changes for the given [streams].
     */
    fun addListener(listener: Listener, streams: IntArray = ALL_STREAMS) {
        // Start monitor if this is the first listener
        if (streamListeners.isEmpty()) {
            start()
        }
        
        // Add listener to volume streams
        for (stream in streams) {
            // Create new list or add to existing
            if (stream !in streamListeners) {
                streamListeners[stream] = mutableListOf(listener)
                
                // Record current volume to only track changes in the future
                streamVolumes[stream] = audioManager.getStreamVolume(stream)
            } else {
                streamListeners[stream].add(listener)
            }
        }
    }
    
    /**
     * Remove the [Listener].
     */
    fun removeListener(listener: Listener) {
        // Remove listener from all streams
        streamListeners.forEach { _, listeners -> listeners.remove(listener) }
        
        // Remove streams with no streamListeners
        // Modifying streamListeners while iterating won't work, so iterate over ALL_STREAMS
        ALL_STREAMS.forEach { stream ->
            if (streamListeners[stream]?.isEmpty() == true) {
                streamListeners.remove(stream)
            }
        }
        
        // Stop monitor if no more listeners
        if (streamListeners.isEmpty()) {
            stop()
        }
    }
    
    /**
     * Register this [AudioVolumeMonitor] to start monitor for stream volume changes.
     */
    private fun start() {
        // Register contentObserver for each volumeUri
        log { "Registering observers for $volumeUris" }
        volumeUris.forEach { resolver.registerContentObserver(it, false, contentObserver) }
        
        // Register deviceCallback and receiver on handler thread
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)
    }
    
    /**
     * Stop monitoring stream volume changes.
     */
    private fun stop() {
        // Stop listening
        resolver.unregisterContentObserver(contentObserver)
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }
    
    /** Updates volumes if output devices are added or removed. */
    private fun devicesUpdated(devices: Array<AudioDeviceInfo>) {
        if (devices.any { it.isSink }) {
            updateVolumes()
        }
    }
    
    /** Update [streamVolumes] and notify [streamListeners] of any changes. */
    private fun updateVolumes() {
        // Get volume of each stream
        streamListeners.forEach { stream, listeners ->
            val volume = audioManager.getStreamVolume(stream)
            
            // Update volume if previous value is different
            if (volume != streamVolumes[volume]) {
                log { "Audio stream $stream volume changed from ${streamVolumes[stream]} to $volume" }
                
                streamVolumes[stream] = volume
                
                // Notify listeners
                listeners.forEach { it.onVolumeChange(stream, volume) }
            }
        }
    }
}