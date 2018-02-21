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

package xyz.sommd.automute

import android.app.Application
import android.content.Context
import android.content.Intent
import xyz.sommd.automute.service.AutoMuteService
import xyz.sommd.automute.service.Notifications
import xyz.sommd.automute.settings.Settings

class App: Application(), Settings.ChangeListener {
    companion object {
        fun from(context: Context) = context.applicationContext as App
    }
    
    lateinit var notifications: Notifications
    lateinit var settings: Settings
    
    override fun onCreate() {
        super.onCreate()
        
        notifications = Notifications(this)
        settings = Settings(this)
        
        notifications.createChannels()
        
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
        startForegroundService(Intent(this, AutoMuteService::class.java))
    }
    
    private fun stopAutoMuteService() {
        stopService(Intent(this, AutoMuteService::class.java))
    }
}