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

package xyz.sommd.automute.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.*
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import xyz.sommd.automute.BuildConfig
import xyz.sommd.automute.R
import xyz.sommd.automute.di.Injection
import xyz.sommd.automute.service.AutoMuteService
import xyz.sommd.automute.service.Notifications
import javax.inject.Inject

class SettingsFragment: PreferenceFragmentCompat(), Settings.ChangeListener {
    @Inject
    lateinit var settings: Settings
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Injection.inject(this)
        
        if (settings.serviceEnabled) {
            AutoMuteService.start(requireContext())
        }
    }
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        // Setup notifications settings intent
        findPreference<Preference>("notifications")!!.intent =
            Intent(ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                putExtra(EXTRA_CHANNEL_ID, Notifications.STATUS_CHANNEL)
            }
        
        // Set app version string
        findPreference<Preference>("app_version")!!.summary = resources.getString(
            R.string.pref_about_app_version_summary,
            BuildConfig.VERSION_NAME,
            BuildConfig.BUILD_TYPE
        )
    }
    
    override fun onStart() {
        super.onStart()
        
        settings.addChangeListener(this)
    }
    
    override fun onStop() {
        super.onStop()
        
        settings.removeChangeListener(this)
    }
    
    override fun onSettingsChanged(settings: Settings, key: String) {
        when (key) {
            Settings.SERVICE_ENABLED_KEY -> {
                // Start or stop the AutoMuteService
                if (settings.serviceEnabled) {
                    AutoMuteService.start(requireContext())
                } else {
                    AutoMuteService.stop(requireContext())
                }
            }
        }
    }
}