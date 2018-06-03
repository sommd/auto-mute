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

package xyz.sommd.automute.utils.monitors

import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for monitoring [AudioPlaybackConfiguration]s.
 *
 * Unlike [AudioManager.AudioPlaybackCallback], [AudioPlaybackMonitor] uses an interface and
 * notifies it when an [AudioPlaybackConfiguration] is started or stopped.
 *
 * @param audioManager The [AudioManager] to use.
 * @param handler The [Handler] for the thread on which to execute listeners.
 */
@Singleton
class AudioPlaybackMonitor @Inject constructor(
        private val audioManager: AudioManager,
        private val handler: Handler = Handler(Looper.getMainLooper())
): AbstractMonitor<AudioPlaybackMonitor.Listener>() {
    interface Listener {
        /**
         * Called when a new [AudioPlaybackConfiguration] is added.
         */
        fun onAudioPlaybackStarted(config: AudioPlaybackConfiguration) {}
        
        /**
         * Called when an [AudioPlaybackConfiguration] is removed.
         */
        fun onAudioPlaybackStopped(config: AudioPlaybackConfiguration) {}
        
        /**
         * Called after [onAudioPlaybackStarted] and [onAudioPlaybackStopped] with all
         * [AudioPlaybackConfiguration]s.
         */
        fun onAudioPlaybacksChanged(configs: Set<AudioPlaybackConfiguration>) {}
    }
    
    /** [MutableSet] to keep track of current [AudioPlaybackConfiguration]s. */
    private val _playbackConfigs = mutableSetOf<AudioPlaybackConfiguration>()
    
    /** [AudioManager.AudioPlaybackCallback] to monitor for [AudioPlaybackConfiguration] changes. */
    private val playbackConfigCallback = object: AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            updatePlaybackConfigurations(configs)
        }
    }
    
    /** [Set] of all current [AudioPlaybackConfiguration]s. */
    val playbackConfigs: Set<AudioPlaybackConfiguration> = _playbackConfigs
    
    /**
     * Register this [AudioPlaybackMonitor] to start monitoring [AudioPlaybackConfiguration]
     * changes.
     */
    override fun start() {
        // Add playback configs to track only future changes
        _playbackConfigs.addAll(audioManager.activePlaybackConfigurations)
        
        // Register playbackConfigCallback on handler thread
        audioManager.registerAudioPlaybackCallback(playbackConfigCallback, handler)
    }
    
    /**
     * Stop monitoring [AudioPlaybackConfiguration] changes.
     */
    override fun stop() {
        // Stop listening and clear current playback configs
        audioManager.unregisterAudioPlaybackCallback(playbackConfigCallback)
        _playbackConfigs.clear()
    }
    
    /** Update [playbackConfigs] and notify [listeners] of any changes. */
    private fun updatePlaybackConfigurations(newConfigs: List<AudioPlaybackConfiguration>) {
        // Add new playback configs and call Listener.onAudioPlaybackStarted for each
        for (config in newConfigs) {
            if (config !in _playbackConfigs) {
                _playbackConfigs.add(config)
                listeners.forEach { it.onAudioPlaybackStarted(config) }
            }
        }
        
        // Remove old playback configs and call Listener.onAudioPlaybackStopped for each
        val iter = _playbackConfigs.iterator()
        for (config in iter) {
            if (config !in newConfigs) {
                iter.remove()
                listeners.forEach { it.onAudioPlaybackStopped(config) }
            }
        }
        
        // Notify audio playbacks changed
        listeners.forEach { it.onAudioPlaybacksChanged(playbackConfigs) }
    }
}