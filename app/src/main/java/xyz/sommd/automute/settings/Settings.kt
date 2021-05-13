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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import xyz.sommd.automute.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Settings @Inject constructor(
    context: Context,
    private val sharedPrefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
): SharedPreferences.OnSharedPreferenceChangeListener {
    interface ChangeListener {
        fun onSettingsChanged(settings: Settings, key: String)
    }
    
    enum class UnmuteMode {
        ALWAYS,
        SHOW_UI,
        NEVER;
    }
    
    companion object {
        const val SERVICE_ENABLED_KEY = "service_enabled"
        
        const val AUTO_MUTE_ENABLED_KEY = "auto_mute_enabled"
        const val AUTO_MUTE_DELAY_KEY = "auto_mute_delay"
        const val AUTO_MUTE_HEADPHONES_DISABLED_KEY = "auto_mute_headphones_disabled"
        const val AUTO_MUTE_HEADPHONES_UNPLUGGED_KEY = "auto_mute_headphones_unplugged"
        const val AUTO_MUTE_SHOW_UI_KEY = "auto_mute_show_ui"
        
        const val AUTO_UNMUTE_DEFAULT_VOLUME_KEY = "auto_unmute_default_volume"
        const val AUTO_UNMUTE_MAXIMUM_VOLUME_KEY = "auto_unmute_maximum_volume"
        const val AUTO_UNMUTE_SHOW_UI_KEY = "auto_unmute_show_ui"
        const val AUTO_UNMUTE_HEADPHONES_PLUGGED_IN_KEY = "auto_unmute_headphones_plugged_in"
        const val AUTO_UNMUTE_MUSIC_MODE_KEY = "auto_unmute_music_mode"
        const val AUTO_UNMUTE_MEDIA_MODE_KEY = "auto_unmute_media_mode"
        const val AUTO_UNMUTE_ASSISTANT_MODE_KEY = "auto_unmute_assistant_mode"
        const val AUTO_UNMUTE_GAME_MODE_KEY = "auto_unmute_game_mode"
    }
    
    private val listeners = mutableSetOf<ChangeListener>()
    
    init {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
        
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }
    
    fun addChangeListener(listener: ChangeListener) {
        listeners.add(listener)
    }
    
    fun removeChangeListener(listener: ChangeListener) {
        listeners.remove(listener)
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        for (listener in listeners) {
            listener.onSettingsChanged(this, key)
        }
    }
    
    var serviceEnabled: Boolean
        get() = sharedPrefs.getBoolean(SERVICE_ENABLED_KEY, true)
        set(value) = sharedPrefs.edit { putBoolean(SERVICE_ENABLED_KEY, value) }
    
    // Auto Mute
    
    var autoMuteEnabled: Boolean
        get() = sharedPrefs.getBoolean(AUTO_MUTE_ENABLED_KEY, true)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_MUTE_ENABLED_KEY, value) }
    
    // TODO use int pref
    var autoMuteDelay: Long
        get() = sharedPrefs.getString(AUTO_MUTE_DELAY_KEY, "30")!!.toLong()
        set(value) = sharedPrefs.edit { putString(AUTO_MUTE_DELAY_KEY, value.toString()) }
    
    var autoMuteHeadphonesDisabled: Boolean
        get() = sharedPrefs.getBoolean(AUTO_MUTE_HEADPHONES_DISABLED_KEY, true)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_MUTE_HEADPHONES_DISABLED_KEY, value) }
    
    var autoMuteHeadphonesUnplugged: Boolean
        get() = sharedPrefs.getBoolean(AUTO_MUTE_HEADPHONES_UNPLUGGED_KEY, true)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_MUTE_HEADPHONES_UNPLUGGED_KEY, value) }
    
    var autoMuteShowUi: Boolean
        get() = sharedPrefs.getBoolean(AUTO_MUTE_SHOW_UI_KEY, false)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_MUTE_SHOW_UI_KEY, value) }
    
    // Auto Unmute
    
    var autoUnmuteDefaultVolume: Float
        get() = sharedPrefs.getFloat(AUTO_UNMUTE_DEFAULT_VOLUME_KEY, 0.5f)
        set(value) = sharedPrefs.edit { putFloat(AUTO_UNMUTE_DEFAULT_VOLUME_KEY, value) }
    
    var autoUnmuteMaximumVolume: Float
        get() = sharedPrefs.getFloat(AUTO_UNMUTE_MAXIMUM_VOLUME_KEY, 1f)
        set(value) = sharedPrefs.edit { putFloat(AUTO_UNMUTE_MAXIMUM_VOLUME_KEY, value) }
    
    var autoUnmuteShowUi: Boolean
        get() = sharedPrefs.getBoolean(AUTO_UNMUTE_SHOW_UI_KEY, true)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_UNMUTE_SHOW_UI_KEY, value) }
    
    var autoUnmuteHeadphonesPluggedIn: Boolean
        get() = sharedPrefs.getBoolean(AUTO_UNMUTE_HEADPHONES_PLUGGED_IN_KEY, true)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_UNMUTE_HEADPHONES_PLUGGED_IN_KEY, value) }
    
    var autoUnmuteMusicMode: UnmuteMode
        get() = getUnmuteMode(AUTO_UNMUTE_MUSIC_MODE_KEY, UnmuteMode.ALWAYS)
        set(value) = sharedPrefs.edit { putString(AUTO_UNMUTE_MUSIC_MODE_KEY, value.name) }
    
    var autoUnmuteMediaMode: UnmuteMode
        get() = getUnmuteMode(AUTO_UNMUTE_MEDIA_MODE_KEY, UnmuteMode.SHOW_UI)
        set(value) = sharedPrefs.edit { putString(AUTO_UNMUTE_MEDIA_MODE_KEY, value.name) }
    
    var autoUnmuteAssistantMode: UnmuteMode
        get() = getUnmuteMode(AUTO_UNMUTE_ASSISTANT_MODE_KEY, UnmuteMode.ALWAYS)
        set(value) = sharedPrefs.edit { putString(AUTO_UNMUTE_ASSISTANT_MODE_KEY, value.name) }
    
    var autoUnmuteGameMode: UnmuteMode
        get() = getUnmuteMode(AUTO_UNMUTE_GAME_MODE_KEY, UnmuteMode.NEVER)
        set(value) = sharedPrefs.edit { putString(AUTO_UNMUTE_GAME_MODE_KEY, value.name) }
    
    private fun getUnmuteMode(key: String, default: UnmuteMode): UnmuteMode {
        return try {
            UnmuteMode.valueOf(sharedPrefs.getString(key, "")!!)
        } catch (e: IllegalArgumentException) {
            default
        }
    }
}
