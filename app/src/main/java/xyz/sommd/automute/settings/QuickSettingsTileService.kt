/*
 * Copyright (C) 2022 David Sommerich
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

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import xyz.sommd.automute.R
import xyz.sommd.automute.di.Injection
import xyz.sommd.automute.service.AutoMuteService
import javax.inject.Inject

class QuickSettingsTileService: TileService(), Settings.ChangeListener {
    @Inject
    lateinit var settings: Settings
    
    override fun onCreate() {
        Injection.inject(this)
    }
    
    override fun onStartListening() {
        settings.addChangeListener(this)
        updateState()
    }
    
    override fun onStopListening() {
        settings.removeChangeListener(this)
    }
    
    override fun onSettingsChanged(settings: Settings, key: String) {
        when (key) {
            Settings.SERVICE_ENABLED_KEY -> updateState()
        }
    }
    
    private fun updateState() {
        qsTile.apply {
            val enabled = settings.serviceEnabled
            
            state = if (enabled) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            
            if (Build.VERSION.SDK_INT >= 29) {
                subtitle = resources.getString(
                    if (enabled) {
                        R.string.qs_tile_enable_subtitle_enabled
                    } else {
                        R.string.qs_tile_enable_subtitle_disabled
                    }
                )
            }
            
            if (Build.VERSION.SDK_INT >= 30) {
                stateDescription = resources.getString(
                    if (enabled) {
                        R.string.qs_tile_enable_state_enabled
                    } else {
                        R.string.qs_tile_enable_state_disabled
                    }
                )
            }
        }.updateTile()
    }
    
    override fun onClick() {
        settings.serviceEnabled = !settings.serviceEnabled
        if (settings.serviceEnabled) {
            AutoMuteService.start(this)
        } else {
            AutoMuteService.stop(this)
        }
    }
}