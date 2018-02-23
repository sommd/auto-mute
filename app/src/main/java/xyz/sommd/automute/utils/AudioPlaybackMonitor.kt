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

class AudioPlaybackMonitor(private val listener: Listener): AudioManager.AudioPlaybackCallback() {
    interface Listener {
        fun audioPlaybackStarted(config: AudioPlaybackConfiguration) {}
        fun audioPlaybackStopped(config: AudioPlaybackConfiguration) {}
        fun audioPlaybackChanged(configs: List<AudioPlaybackConfiguration>) {}
    }
    
    private val _playbackConfigs = mutableSetOf<AudioPlaybackConfiguration>()
    val playbackConfigs: Set<AudioPlaybackConfiguration> = _playbackConfigs
    
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