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

package xyz.sommd.automute.it

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.sommd.automute.settings.Settings

@RunWith(AndroidJUnit4::class)
@LargeTest
class AutoUnmuteIT {
    @Before
    fun before() {
        Audio.mute()
        Prefs.reset()
        Prefs.disableAutoMute()
        Service.start()
        SystemUi.hideVolumeDialog()
    }
    
    @After
    fun after() {
        Service.stop()
    }
    
    @Test
    fun autoUnmutesAndShowsVolumeDialogWhenConfiguredToAlwaysUnmuteAndShowUi() {
        Prefs.set(Settings.AUTO_UNMUTE_MUSIC_MODE_KEY, Settings.UnmuteMode.ALWAYS)
        Prefs.set(Settings.AUTO_UNMUTE_SHOW_UI_KEY, true)
        
        Audio.play {
            sleep(EPSILON)
            Audio.assertUnmuted()
            SystemUi.assertVolumeDialogShown()
        }
    }
    
    @Test
    fun autoUnmutesOnlyWhenConfiguredToAlwaysUnmuteButNotShowUi() {
        Prefs.set(Settings.AUTO_UNMUTE_MUSIC_MODE_KEY, Settings.UnmuteMode.ALWAYS)
        Prefs.set(Settings.AUTO_UNMUTE_SHOW_UI_KEY, false)
        
        Audio.play {
            sleep(EPSILON)
            Audio.assertUnmuted()
            SystemUi.assertVolumeDialogNotShown()
        }
    }
    
    @Test
    fun showsVolumeDialogOnlyWhenConfiguredToShowUi() {
        Prefs.set(Settings.AUTO_UNMUTE_MUSIC_MODE_KEY, Settings.UnmuteMode.SHOW_UI)
        
        Audio.play {
            sleep(EPSILON)
            Audio.assertMuted()
            SystemUi.assertVolumeDialogShown()
        }
    }
    
    @Test
    fun doesNothingWhenConfiguredToNeverUnmute() {
        Prefs.set(Settings.AUTO_UNMUTE_MUSIC_MODE_KEY, Settings.UnmuteMode.NEVER)
        
        Audio.play {
            sleep(EPSILON)
            Audio.assertMuted()
            SystemUi.assertVolumeDialogNotShown()
        }
    }
}