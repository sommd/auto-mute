package xyz.sommd.automute

import android.app.Application
import xyz.sommd.automute.settings.Settings

class App: Application() {
    lateinit var settings: Settings
    
    override fun onCreate() {
        super.onCreate()
        
        settings = Settings(this)
        settings.setDefaultValues()
    }
}