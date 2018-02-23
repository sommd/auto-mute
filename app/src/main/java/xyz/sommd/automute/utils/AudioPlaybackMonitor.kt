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
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import androidx.content.systemService

/**
 * Class for monitoring [AudioPlaybackConfiguration]s.
 *
 * Unlike [AudioManager.AudioPlaybackCallback], [AudioPlaybackMonitor] uses an interface and
 * notifies it when an [AudioPlaybackConfiguration] is started or stopped.
 *
 * @param context The [Context] to get the [AudioManager] service from.
 * @param listener The [Listener] to notify changes of.
 * @param handler The [Handler] to pass to [AudioManager.registerAudioPlaybackCallback].
 */
class AudioPlaybackMonitor(context: Context,
                           private val listener: Listener,
                           private val handler: Handler? = null):
        AudioManager.AudioPlaybackCallback() {
    
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
    
    private val audioManager = context.systemService<AudioManager>()
    
    /** Mutable [Set] to keep track of current [AudioPlaybackConfiguration]s. */
    private val _playbackConfigs = mutableSetOf<AudioPlaybackConfiguration>()
    /** Read only [Set] of all current [AudioPlaybackConfiguration]s. */
    val playbackConfigs: Set<AudioPlaybackConfiguration> = _playbackConfigs
    
    /**
     * Register this [AudioPlaybackMonitor] to start monitoring [AudioPlaybackConfiguration]
     * changes.
     *
     * @param notifyNow If `true`, the [Listener] will be notified of any current
     * [AudioPlaybackConfiguration]s. Note: the [Listener] will be called on the current [Thread],
     * not via the [Handler].
     */
    fun start(notifyNow: Boolean = false) {
        if (notifyNow) {
            onPlaybackConfigChanged(audioManager.activePlaybackConfigurations)
        } else {
            _playbackConfigs.addAll(audioManager.activePlaybackConfigurations)
        }
        
        audioManager.registerAudioPlaybackCallback(this, handler)
    }
    
    /**
     * Stop monitoring [AudioPlaybackConfiguration] changes.
     */
    fun stop() {
        audioManager.unregisterAudioPlaybackCallback(this)
        _playbackConfigs.clear()
    }
    
    override fun onPlaybackConfigChanged(newConfigs: List<AudioPlaybackConfiguration>) {
        for (config in newConfigs) {
            if (config !in _playbackConfigs) {
                _playbackConfigs.add(config)
                listener.audioPlaybackStarted(config)
            }
        }
        
        val iter = _playbackConfigs.iterator()
        for (config in iter) {
            if (config !in newConfigs) {
                iter.remove()
                listener.audioPlaybackStopped(config)
            }
        }
        
        listener.audioPlaybackChanged(newConfigs)
    }
}