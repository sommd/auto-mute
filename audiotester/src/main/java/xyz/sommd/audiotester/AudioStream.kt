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

package xyz.sommd.audiotester

import android.content.Context
import android.media.AsyncPlayer
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri

interface AudioStream {
    data class Description(
        val sampleName: CharSequence,
        val playerTypeName: CharSequence,
        val usageName: CharSequence,
        val contentTypeName: CharSequence
    )
    
    val description: Description
    
    val isPlaying: Boolean
    
    fun play()
    
    fun pause()
    
    fun release()
}

class MediaPlayerAudioStream(
    private val mediaPlayer: MediaPlayer,
    override val description: AudioStream.Description,
): AudioStream {
    
    override val isPlaying get() = mediaPlayer.isPlaying
    
    override fun play() = mediaPlayer.start()
    
    override fun pause() = mediaPlayer.pause()
    
    override fun release() = mediaPlayer.release()
}

class AsyncPlayerAudioStream(
    private val asyncPlayer: AsyncPlayer,
    private val context: Context,
    private val uri: Uri,
    private val audioAttributes: AudioAttributes,
    override val description: AudioStream.Description
): AudioStream {
    
    override var isPlaying: Boolean = false
        private set
    
    override fun play() {
        isPlaying = true
        asyncPlayer.play(context, uri, true, audioAttributes)
    }
    
    override fun pause() {
        isPlaying = false
        asyncPlayer.stop()
    }
    
    override fun release() {
        pause()
    }
}

class SoundPoolAudioStream(
    private val soundPool: SoundPool,
    override val description: AudioStream.Description,
): AudioStream {
    override var isPlaying: Boolean = false
        private set
    
    override fun play() {
        isPlaying = true
        soundPool.autoResume()
    }
    
    override fun pause() {
        isPlaying = false
        soundPool.autoPause()
    }
    
    override fun release() = soundPool.release()
}