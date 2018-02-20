package xyz.sommd.automute

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceManager

class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        
        supportFragmentManager.beginTransaction()
                .add(android.R.id.content, SettingsFragment())
                .commit()
    }
    
    class SettingsFragment: PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }
}
