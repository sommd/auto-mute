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
import xyz.sommd.audiotester.databinding.ItemAudioStreamBinding

class AudioStreamAdapter: RecyclerView.Adapter<AudioStreamAdapter.ViewHolder>() {
    inner class ViewHolder(private val binding: ItemAudioStreamBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        
        private val audioStream get() = _audioStreams[bindingAdapterPosition]
        
        init {
            binding.playPauseButton.setOnClickListener(this)
            binding.deleteButton.setOnClickListener(this)
        }
        
        fun bind(audioStream: AudioStream) {
            binding.playPauseButton.setImageResource(
                if (audioStream.isPlaying) {
                    R.drawable.ic_pause
                } else {
                    R.drawable.ic_play
                }
            )
            
            binding.titleText.text = binding.titleText.resources.getString(
                R.string.text_audio_stream_title,
                audioStream.description.sampleName,
                audioStream.description.playerTypeName
            )
            binding.usageText.text = audioStream.description.usageName
            binding.contentTypeText.text = audioStream.description.contentTypeName
        }
        
        override fun onClick(v: View) {
            when (v) {
                binding.playPauseButton -> {
                    if (audioStream.isPlaying) {
                        audioStream.pause()
                    } else {
                        audioStream.play()
                    }
                    
                    notifyItemChanged(bindingAdapterPosition)
                }
                binding.deleteButton -> {
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
        val binding = ItemAudioStreamBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
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