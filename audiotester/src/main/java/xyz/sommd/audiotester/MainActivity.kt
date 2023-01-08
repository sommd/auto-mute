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

import android.content.ContentResolver
import android.media.AsyncPlayer
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import xyz.sommd.audiotester.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity() {
    companion object {
        val AUDIO_SAMPLES = listOf(
            R.raw.silence,
            R.raw.chirp,
            R.raw.tone
        )
        
        val AUDIO_USAGES = listOf(
            AudioAttributes.USAGE_MEDIA,
            AudioAttributes.USAGE_GAME,
            AudioAttributes.USAGE_ASSISTANT,
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            AudioAttributes.USAGE_UNKNOWN
        )
        
        val AUDIO_CONTENT_TYPES = listOf(
            AudioAttributes.CONTENT_TYPE_MUSIC,
            AudioAttributes.CONTENT_TYPE_MOVIE,
            AudioAttributes.CONTENT_TYPE_SPEECH,
            AudioAttributes.CONTENT_TYPE_UNKNOWN
        )
    }
    
    private lateinit var binding: ActivityMainBinding
    private val adapter = AudioStreamAdapter()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.addAudioStreamButton.setOnClickListener { addAudioStream() }
        binding.audioStreamRecycler.adapter = adapter
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        for (audioStream in adapter.audioStreams) {
            audioStream.release()
        }
    }
    
    private fun addAudioStream() {
        val sample = AUDIO_SAMPLES[binding.sampleSpinner.selectedItemPosition]
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AUDIO_USAGES[binding.usageSpinner.selectedItemPosition])
            .setContentType(AUDIO_CONTENT_TYPES[binding.contentTypeSpinner.selectedItemPosition])
            .build()
        val description = AudioStream.Description(
            binding.sampleSpinner.selectedItem as CharSequence,
            binding.playerTypeSpinner.selectedItem as CharSequence,
            binding.usageSpinner.selectedItem as CharSequence,
            binding.contentTypeSpinner.selectedItem as CharSequence
        )
        
        adapter.addAudioStream(
            when (binding.playerTypeSpinner.selectedItemPosition) {
                0 -> { // MediaPlayer
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setDataSource(resources.openRawResourceFd(sample))
                    mediaPlayer.setAudioAttributes(audioAttributes)
                    mediaPlayer.isLooping = true
                    mediaPlayer.prepare()
                    
                    MediaPlayerAudioStream(mediaPlayer, description)
                }
                1 -> { // AsyncPlayer
                    AsyncPlayerAudioStream(
                        AsyncPlayer(description.toString()),
                        this,
                        Uri.parse(
                            "${
                                ContentResolver.SCHEME_ANDROID_RESOURCE
                            }://${
                                resources.getResourcePackageName(sample)
                            }/${
                                resources.getResourceTypeName(sample)
                            }/${
                                resources.getResourceEntryName(sample)
                            }"
                        ),
                        audioAttributes,
                        description
                    )
                }
                2 -> { // SoundPool
                    val soundPool = SoundPool.Builder()
                        .setAudioAttributes(audioAttributes)
                        .build()
                    soundPool.load(this, sample, 1)
                    soundPool.setOnLoadCompleteListener { _, soundId, _ ->
                        soundPool.play(soundId, 1.0f, 1.0f, 1, -1, 1.0f)
                        soundPool.autoPause()
                    }
                    
                    SoundPoolAudioStream(soundPool, description)
                }
                else -> error("Unknown player type")
            }
        )
    }
}
