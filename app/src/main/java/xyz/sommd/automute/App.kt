package xyz.sommd.automute

import android.app.Application
import android.content.Intent
import xyz.sommd.automute.service.AutoMuteService
import xyz.sommd.automute.settings.Settings

class App: Application(), Settings.ChangeListener {
    lateinit var settings: Settings
    
    override fun onCreate() {
        super.onCreate()
        
        settings = Settings(this)
        settings.setDefaultValues()
        settings.addChangeListener(this)
        
        if (settings.serviceEnabled) {
            startAutoMuteService()
        }
    }
    
    override fun onSettingsChanged(settings: Settings, key: String) {
        when (key) {
            Settings.SERVICE_ENABLED_KEY -> {
                if (settings.serviceEnabled) {
                    startAutoMuteService()
                } else {
                    stopAutoMuteService()
                }
            }
        }
    }
    
    private fun startAutoMuteService() {
        // TODO Use startForegroundService
        startService(Intent(this, AutoMuteService::class.java))
    }
    
    private fun stopAutoMuteService() {
        stopService(Intent(this, AutoMuteService::class.java))
    }
}