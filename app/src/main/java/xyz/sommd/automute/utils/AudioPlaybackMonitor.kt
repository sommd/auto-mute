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
    
    private val playbackConfigs = mutableSetOf<AudioPlaybackConfiguration>()
    
    override fun onPlaybackConfigChanged(newConfigs: MutableList<AudioPlaybackConfiguration>) {
        for (config in newConfigs) {
            if (config !in playbackConfigs) {
                listener.audioPlaybackStarted(config)
                playbackConfigs.add(config)
            }
        }
        
        for (config in playbackConfigs) {
            if (config !in newConfigs) {
                listener.audioPlaybackStopped(config)
                playbackConfigs.remove(config)
            }
        }
    }
}