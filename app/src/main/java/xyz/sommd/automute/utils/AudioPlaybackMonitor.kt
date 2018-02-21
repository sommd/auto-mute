package xyz.sommd.automute.utils

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper

class AudioPlaybackMonitor(private val context: Context,
                           private val listener: Listener,
                           private val handler: Handler = Handler(Looper.getMainLooper())):
        AudioManager.AudioPlaybackCallback() {
    
    interface Listener {
        fun audioPlaybackStarted(config: AudioPlaybackConfiguration)
        fun audioPlaybackStopped(config: AudioPlaybackConfiguration)
    }
    
    private val _playbackConfigs = mutableSetOf<AudioPlaybackConfiguration>()
    val playbackConfigs: Set<AudioPlaybackConfiguration> = _playbackConfigs
    
    override fun onPlaybackConfigChanged(newConfigs: MutableList<AudioPlaybackConfiguration>) {
        for (config in newConfigs) {
            if (config !in _playbackConfigs) {
                _playbackConfigs.add(config)
                listener.audioPlaybackStarted(config)
            }
        }
        
        for (config in _playbackConfigs) {
            if (config !in newConfigs) {
                _playbackConfigs.remove(config)
                listener.audioPlaybackStopped(config)
            }
        }
    }
}