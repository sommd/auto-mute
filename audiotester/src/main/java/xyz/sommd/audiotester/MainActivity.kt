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
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import androidx.net.toUri
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: AppCompatActivity() {
    companion object {
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
    
    private lateinit var audioSampleUris: List<Uri>
    private val adapter = AudioStreamAdapter()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        audioSampleUris = resources.getStringArray(R.array.audio_sample_urls).map(String::toUri)
        
        audioStreamRecycler.adapter = adapter
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        for (audioStream in adapter.audioStreams) {
            audioStream.release()
        }
    }
    
    fun addAudioStream(view: View) {
        val audioAttributes = AudioAttributes.Builder()
                .setUsage(AUDIO_USAGES[usageSpinner.selectedItemPosition])
                .setContentType(AUDIO_CONTENT_TYPES[contentTypeSpinner.selectedItemPosition])
                .build()
        
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(this, audioSampleUris[sampleSpinner.selectedItemPosition])
        mediaPlayer.setAudioAttributes(audioAttributes)
        mediaPlayer.isLooping = true
        mediaPlayer.prepare()
        
        val audioStream = AudioStream(mediaPlayer,
                                      sampleSpinner.selectedItem as CharSequence,
                                      usageSpinner.selectedItem as CharSequence,
                                      contentTypeSpinner.selectedItem as CharSequence)
        
        adapter.addAudioStream(audioStream)
    }
}
