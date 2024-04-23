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

package xyz.sommd.automute.utils.monitors

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import xyz.sommd.automute.utils.description
import xyz.sommd.automute.utils.isExternal
import xyz.sommd.automute.utils.log
import javax.inject.Inject

/**
 * Class for monitor if audio output is internal or external.
 *
 * @param audioManager The [AudioManager] to use.
 * @param handler The [Handler] for the thread on which to execute listeners.
 */
class AudioOutputMonitor @Inject constructor(
    private val audioManager: AudioManager,
    private val handler: Handler
): AbstractMonitor<AudioOutputMonitor.Listener>() {
    interface Listener {
        /**
         * Called when audio output switches from internal to external.
         */
        fun onAudioOutputExternal() {}
        
        /**
         * Called when audio output switches from external to internal.
         */
        fun onAudioOutputInternal() {}
    }
    
    /** The ID's of all current external audio outputs. */
    private val externalOutputs = mutableSetOf<Int>()
    
    /** Callback for audio devices being added/removed. */
    private val audioDeviceCallback = object: AudioDeviceCallback() {
        override fun onAudioDevicesAdded(devices: Array<AudioDeviceInfo>) {
            this@AudioOutputMonitor.log { "Devices added: ${devices.map { it.description }}" }
            devicesAdded(devices)
        }
        
        override fun onAudioDevicesRemoved(devices: Array<AudioDeviceInfo>) {
            this@AudioOutputMonitor.log { "Devices removed: ${devices.map { it.description }}" }
            devicesRemoved(devices)
        }
    }
    
    /**
     * If audio output is currently external.
     */
    val isOutputExternal
        get() = externalOutputs.isNotEmpty()
    
    /**
     * Start monitoring audio devices.
     */
    override fun start() {
        // Get current external outputs to only track future changes
        externalOutputs.addAll(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                                   .filter { it.isExternal }
                                   .map { it.id })
        
        // Start listening
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
    }
    
    /**
     * Stop monitoring audio devices.
     */
    override fun stop() {
        // Stop listener and clear external devices
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        externalOutputs.clear()
    }
    
    private fun devicesAdded(devices: Array<AudioDeviceInfo>) {
        val oldOutputExternal = isOutputExternal
        
        // Add external outputs to externalOutputs
        for (device in devices) {
            if (device.isSink && device.isExternal) {
                this@AudioOutputMonitor.log { "External output added: ${device.productName}" }
                externalOutputs.add(device.id)
            }
        }
        
        // Notify listener if isOutputExternal has changed
        if (oldOutputExternal != isOutputExternal) {
            this@AudioOutputMonitor.log { "Output now external" }
            listeners.forEach { it.onAudioOutputExternal() }
        }
    }
    
    private fun devicesRemoved(devices: Array<AudioDeviceInfo>) {
        val oldOutputExternal = isOutputExternal
        
        // Remove outputs from externalOutputs
        for (device in devices) {
            if (externalOutputs.remove(device.id)) {
                this@AudioOutputMonitor.log { "External output removed: ${device.description}" }
            }
        }
        
        // Notify listener if isOutputExternal has changed
        if (oldOutputExternal != isOutputExternal) {
            this@AudioOutputMonitor.log { "Output now internal" }
            listeners.forEach { it.onAudioOutputInternal() }
        }
    }
}