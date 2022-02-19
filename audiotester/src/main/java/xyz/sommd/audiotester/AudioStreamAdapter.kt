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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_audio_stream.view.*

class AudioStreamAdapter: RecyclerView.Adapter<AudioStreamAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View):
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        
        private val playPauseButton = itemView.playPauseButton
        private val deleteButton = itemView.deleteButton
        private val sampleText = itemView.sampleText
        private val usageText = itemView.usageText
        private val contentTypeText = itemView.contentTypeText
        
        private val audioStream get() = _audioStreams[bindingAdapterPosition]
        
        init {
            playPauseButton.setOnClickListener(this)
            deleteButton.setOnClickListener(this)
        }
        
        fun bind(audioStream: AudioStream) {
            playPauseButton.setImageResource(
                if (audioStream.isPlaying) {
                    R.drawable.ic_pause
                } else {
                    R.drawable.ic_play
                }
            )
            
            sampleText.text = audioStream.sampleName
            usageText.text = audioStream.usageName
            contentTypeText.text = audioStream.contentTypeName
        }
        
        override fun onClick(v: View) {
            when (v) {
                playPauseButton -> {
                    if (audioStream.isPlaying) {
                        audioStream.pause()
                    } else {
                        audioStream.play()
                    }
                    
                    notifyItemChanged(bindingAdapterPosition)
                }
                deleteButton -> {
                    audioStream.release()
                    _audioStreams.removeAt(bindingAdapterPosition)
                    notifyItemRemoved(bindingAdapterPosition)
                }
            }
        }
    }
    
    private val _audioStreams = mutableListOf<AudioStream>()
    val audioStreams: List<AudioStream> = _audioStreams
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_audio_stream, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(_audioStreams[position])
    }
    
    override fun getItemCount() = _audioStreams.size
    
    fun addAudioStream(audioStream: AudioStream) {
        _audioStreams.add(audioStream)
        notifyItemInserted(_audioStreams.size)
    }
}