package xyz.sommd.automute.service

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import androidx.content.systemService
import xyz.sommd.automute.settings.Settings
import xyz.sommd.automute.utils.AudioPlaybackMonitor

class AutoMuteService: Service(), AudioPlaybackMonitor.Listener {
    override fun onBind(intent: Intent?): IBinder? = null
    
    private lateinit var settings: Settings
    private lateinit var notifications: Notifications
    private lateinit var handler: Handler
    private lateinit var audioManager: AudioManager
    private lateinit var playbackMonitor: AudioPlaybackMonitor
    
    override fun onCreate() {
        Toast.makeText(this, "Starting Auto Mute Service", Toast.LENGTH_SHORT).show()
        
        settings = Settings.from(this)
        notifications = Notifications.from(this)
        handler = Handler()
        audioManager = systemService()
        playbackMonitor = AudioPlaybackMonitor(this, this)
        
        audioManager.registerAudioPlaybackCallback(playbackMonitor, handler)
        
        startForeground(Notifications.STATUS_ID, notifications.createStatusNotification())
    }
    
    override fun onDestroy() {
        Toast.makeText(this, "Stopping Auto Mute Service", Toast.LENGTH_SHORT).show()
        
        audioManager.unregisterAudioPlaybackCallback(playbackMonitor)
    }
    
    override fun audioPlaybackStarted(config: AudioPlaybackConfiguration) {
        // TODO
        Toast.makeText(this, "Playback Started: " + config, Toast.LENGTH_SHORT).show()
    }
    
    override fun audioPlaybackStopped(config: AudioPlaybackConfiguration) {
        // TODO
        Toast.makeText(this, "Playback Stopped: " + config, Toast.LENGTH_SHORT).show()
    }
}