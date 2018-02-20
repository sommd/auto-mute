package xyz.sommd.automute

import android.content.Context
import android.support.v7.preference.PreferenceManager
import androidx.content.edit

class Settings(private val context: Context) {
    companion object {
        private const val SERVICE_ENABLED_KEY = "service_enabled"
        private const val AUTO_MUTE_ENABLED_KEY = "auto_mute_enabled"
        private const val AUTO_MUTE_DELAY_KEY = "auto_mute_delay"
        private const val AUTO_MUTE_TOAST_KEY = "auto_mute_toast"
        private const val AUTO_UNMUTE_DEFAULT_VOLUME_KEY = "auto_unmute_default_volume"
        private const val AUTO_UNMUTE_MUSIC_MODE_KEY = "auto_unmute_music_mode"
        private const val AUTO_UNMUTE_MEDIA_MODE_KEY = "auto_unmute_media_mode"
        private const val AUTO_UNMUTE_ASSISTANT_MODE_KEY = "auto_unmute_assistant_mode"
        private const val AUTO_UNMUTE_GAME_MODE_KEY = "auto_unmute_game_mode"
    }
    
    enum class UnmuteMode {
        NEVER,
        ASK,
        ALWAYS;
    }
    
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    
    var serviceEnabled: Boolean
        get() = sharedPrefs.getBoolean(SERVICE_ENABLED_KEY, true)
        set(value) = sharedPrefs.edit { putBoolean(SERVICE_ENABLED_KEY, value) }
    
    var autoMuteEnabled: Boolean
        get() = sharedPrefs.getBoolean(AUTO_MUTE_ENABLED_KEY, true)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_MUTE_ENABLED_KEY, value) }
    
    // TODO use int pref
    var autoMuteDelay: Int
        get() = sharedPrefs.getString(AUTO_MUTE_DELAY_KEY, "30").toInt()
        set(value) = sharedPrefs.edit { putString(AUTO_MUTE_DELAY_KEY, value.toString()) }
    
    var autoMuteToast: Boolean
        get() = sharedPrefs.getBoolean(AUTO_MUTE_TOAST_KEY, false)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_MUTE_TOAST_KEY, value) }
    
    var autoUnmuteDefaultVolume: Int
        get() = sharedPrefs.getInt(AUTO_UNMUTE_DEFAULT_VOLUME_KEY, 50)
        set(value) = sharedPrefs.edit { putInt(AUTO_UNMUTE_DEFAULT_VOLUME_KEY, value) }
    
    var autoUnmuteMusicMode: UnmuteMode
        get() = UnmuteMode.valueOf(sharedPrefs.getString(AUTO_UNMUTE_MUSIC_MODE_KEY, "ALWAYS"))
        set(value) = sharedPrefs.edit { putString(AUTO_UNMUTE_MUSIC_MODE_KEY, value.name) }
    
    var autoUnmuteMediaMode: UnmuteMode
        get() = UnmuteMode.valueOf(sharedPrefs.getString(AUTO_UNMUTE_MEDIA_MODE_KEY, "ASK"))
        set(value) = sharedPrefs.edit { putString(AUTO_UNMUTE_MEDIA_MODE_KEY, value.name) }
    
    var autoUnmuteAssistantMode: UnmuteMode
        get() = UnmuteMode.valueOf(sharedPrefs.getString(AUTO_UNMUTE_ASSISTANT_MODE_KEY, "ALWAYS"))
        set(value) = sharedPrefs.edit { putString(AUTO_UNMUTE_ASSISTANT_MODE_KEY, value.name) }
    
    var autoUnmuteGameMode: UnmuteMode
        get() = UnmuteMode.valueOf(sharedPrefs.getString(AUTO_UNMUTE_GAME_MODE_KEY, "NEVER"))
        set(value) = sharedPrefs.edit { putString(AUTO_UNMUTE_GAME_MODE_KEY, value.name) }
    
    fun setDefaultValues() {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
    }
}