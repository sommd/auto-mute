package xyz.sommd.automute.settings

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.preference.PreferenceManager
import androidx.content.edit
import xyz.sommd.automute.App
import xyz.sommd.automute.R

class Settings(private val context: Context): SharedPreferences.OnSharedPreferenceChangeListener {
    interface ChangeListener {
        fun onSettingsChanged(settings: Settings, key: String)
    }
    
    enum class UnmuteMode {
        NEVER,
        ASK,
        ALWAYS;
    }
    
    companion object {
        const val SERVICE_ENABLED_KEY = "service_enabled"
        const val AUTO_MUTE_ENABLED_KEY = "auto_mute_enabled"
        const val AUTO_MUTE_DELAY_KEY = "auto_mute_delay"
        const val AUTO_MUTE_TOAST_KEY = "auto_mute_toast"
        const val AUTO_UNMUTE_DEFAULT_VOLUME_KEY = "auto_unmute_default_volume"
        const val AUTO_UNMUTE_MUSIC_MODE_KEY = "auto_unmute_music_mode"
        const val AUTO_UNMUTE_MEDIA_MODE_KEY = "auto_unmute_media_mode"
        const val AUTO_UNMUTE_ASSISTANT_MODE_KEY = "auto_unmute_assistant_mode"
        const val AUTO_UNMUTE_GAME_MODE_KEY = "auto_unmute_game_mode"
        
        fun from(context: Context) = App.from(context).settings
    }
    
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val listeners = mutableSetOf<ChangeListener>()
    
    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }
    
    fun setDefaultValues() {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
    }
    
    fun addChangeListener(listener: ChangeListener) {
        listeners.add(listener)
    }
    
    inline fun addChangeListener(crossinline listener: (Settings, String) -> Any?) {
        addChangeListener(object: ChangeListener {
            override fun onSettingsChanged(settings: Settings, key: String) {
                listener(settings, key)
            }
        })
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
    
    var autoUnmuteDefaultVolume: Float
        get() = sharedPrefs.getInt(AUTO_UNMUTE_DEFAULT_VOLUME_KEY, 50) / 100f
        set(value) = sharedPrefs.edit { putInt(AUTO_UNMUTE_DEFAULT_VOLUME_KEY, (value * 100).toInt()) }
    
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
}