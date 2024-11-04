/*
 * Copyright (C) 2024 Dana Sommerich
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

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.*
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import xyz.sommd.automute.BuildConfig
import xyz.sommd.automute.R
import xyz.sommd.automute.di.Injection
import xyz.sommd.automute.service.AutoMuteService
import xyz.sommd.automute.service.Notifications
import javax.inject.Inject

class SettingsFragment: PreferenceFragmentCompat(), Settings.ChangeListener {
    @Inject
    lateinit var settings: Settings
    
    @Inject
    lateinit var notifications: Notifications
    
    @Inject
    lateinit var notifManager: NotificationManager
    
    private lateinit var serviceEnabledPreference: TwoStatePreference
    private lateinit var notificationSettingsPreference: Preference
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Injection.inject(this)
        
        if (settings.serviceEnabled) {
            AutoMuteService.start(requireContext())
        }
    }
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        serviceEnabledPreference = findPreference(Settings.SERVICE_ENABLED_KEY)!!
        notificationSettingsPreference = findPreference("notifications")!!
        
        // Set app version string
        findPreference<Preference>("app_version")!!.summary = resources.getString(
            R.string.pref_about_app_version_summary,
            BuildConfig.VERSION_NAME,
            BuildConfig.BUILD_TYPE
        )
    }
    
    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
        // Add padding to bottom so that content isn't stuck behind navigation bar (really hacky, wish this was easy to do in the layout instead)
        clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPaddingRelative(0, 0, 0, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
    
    override fun onStart() {
        super.onStart()
        
        settings.addChangeListener(this)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Open app notification settings instead of channel notifications settings if notifications
        // are disabled so that user can enable notifications since trying to enable a channel with
        // app notifications disabled doesn't work.
        notificationSettingsPreference.intent = if (notifManager.areNotificationsEnabled()) {
            Intent(ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(EXTRA_CHANNEL_ID, Notifications.STATUS_CHANNEL)
            }
        } else {
            Intent(ACTION_APP_NOTIFICATION_SETTINGS)
        }.apply {
            putExtra(EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
        }
        
        // Update status notification in case notifications were just enabled
        if (settings.serviceEnabled) {
            notifications.updateStatusNotification()
        }
    }
    
    override fun onStop() {
        super.onStop()
        
        settings.removeChangeListener(this)
    }
    
    override fun onSettingsChanged(settings: Settings, key: String) {
        when (key) {
            Settings.SERVICE_ENABLED_KEY -> updateServiceState()
        }
    }
    
    override fun onSettingsCleared(settings: Settings) {
        updateServiceState()
    }
    
    private fun updateServiceState() {
        // Update state in case change was external (e.g. from Quick Settings)
        serviceEnabledPreference.isChecked = settings.serviceEnabled
        
        // Start or stop the AutoMuteService
        if (settings.serviceEnabled) {
            AutoMuteService.start(requireContext())
        } else {
            AutoMuteService.stop(requireContext())
        }
    }
}