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

import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import xyz.sommd.automute.utils.AudioPlaybackMonitor.Listener

/**
 * Class for monitoring [AudioPlaybackConfiguration]s.
 *
 * Unlike [AudioManager.AudioPlaybackCallback], [AudioPlaybackMonitor] uses an interface and
 * notifies it when an [AudioPlaybackConfiguration] is started or stopped.
 *
 * @param audioManager The [AudioManager] to use.
 * @param listener The [Listener] to be notified of volume changes.
 * @param handler The [Handler] for the thread on which to execute the [listener].
 */
@AutoFactory
class AudioPlaybackMonitor(
        @Provided
        private val audioManager: AudioManager,
        private val listener: Listener,
        @Provided
        private val handler: Handler = Handler(Looper.getMainLooper())
) {
    interface Listener {
        /**
         * Called when a new [AudioPlaybackConfiguration] is added.
         */
        fun audioPlaybackStarted(config: AudioPlaybackConfiguration) {}
        
        /**
         * Called when an [AudioPlaybackConfiguration] is removed.
         */
        fun audioPlaybackStopped(config: AudioPlaybackConfiguration) {}
        
        /**
         * Called after [audioPlaybackStarted] and [audioPlaybackStopped] with all
         * [AudioPlaybackConfiguration]s.
         */
        fun audioPlaybackChanged(configs: List<AudioPlaybackConfiguration>) {}
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
     *
     * @param notifyNow If `true`, the [Listener] will be notified of any current
     * [AudioPlaybackConfiguration]s. The [Listener] will be called on the [handler] thread.
     */
    fun start(notifyNow: Boolean = false) {
        if (notifyNow) {
            handler.postOrRunNow {
                // playbackConfigs should be empty so this will notify for all playback configs
                updatePlaybackConfigurations(audioManager.activePlaybackConfigurations)
            }
        } else {
            // Add playback configs to track only future changes
            _playbackConfigs.addAll(audioManager.activePlaybackConfigurations)
        }
        
        // Register playbackConfigCallback on handler thread
        audioManager.registerAudioPlaybackCallback(playbackConfigCallback, handler)
    }
    
    /**
     * Stop monitoring [AudioPlaybackConfiguration] changes.
     */
    fun stop() {
        // Stop listening and clear current playback configs
        audioManager.unregisterAudioPlaybackCallback(playbackConfigCallback)
        _playbackConfigs.clear()
    }
    
    /** Update [playbackConfigs] and notify [listener] of any changes. */
    private fun updatePlaybackConfigurations(newConfigs: List<AudioPlaybackConfiguration>) {
        // Add new playback configs and call Listener.audioPlaybackStarted for each
        for (config in newConfigs) {
            if (config !in _playbackConfigs) {
                _playbackConfigs.add(config)
                listener.audioPlaybackStarted(config)
            }
        }
        
        // Remove old playback configs and call Listener.audioPlaybackStopped for each
        val iter = _playbackConfigs.iterator()
        for (config in iter) {
            if (config !in newConfigs) {
                iter.remove()
                listener.audioPlaybackStopped(config)
            }
        }
        
        // Notify audio playbacks changed
        listener.audioPlaybackChanged(newConfigs)
    }
}