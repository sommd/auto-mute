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

package xyz.sommd.audiotester

import android.media.AudioAttributes
import android.media.MediaPlayer
import xyz.sommd.audiotester.utils.log

class AudioStream(url: String, audioAttributes: AudioAttributes,
                  val sampleName: CharSequence,
                  val usageName: CharSequence,
                  val contentTypeName: CharSequence) {
    
    private val mediaPlayer = MediaPlayer()
    
    val isPlaying get() = mediaPlayer.isPlaying
    
    init {
        mediaPlayer.setDataSource(url)
        mediaPlayer.setAudioAttributes(audioAttributes)
        mediaPlayer.prepareAsync()
    }
    
    fun play() {
        log("Playing: $mediaPlayer")
        mediaPlayer.start()
    }
    
    fun pause() {
        log("Pausing: $mediaPlayer")
        mediaPlayer.pause()
    }
    
    fun release() {
        log("Releasing: $mediaPlayer")
        
        if (isPlaying) {
            pause()
        }
        
        mediaPlayer.release()
    }
}