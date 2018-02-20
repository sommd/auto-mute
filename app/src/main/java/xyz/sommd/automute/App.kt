package xyz.sommd.automute

import android.app.Application

class App: Application() {
    lateinit var settings: Settings
    
    override fun onCreate() {
        super.onCreate()
        
        settings = Settings(this)
        settings.setDefaultValues()
        
        
    }
}