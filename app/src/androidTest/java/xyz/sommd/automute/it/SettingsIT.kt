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

package xyz.sommd.automute.it

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.sommd.automute.settings.Settings

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsIT {
    @Before
    fun before() {
        Service.stop()
        Prefs.reset()
    }
    
    @After
    fun after() {
        Service.stop()
    }
    
    @Test
    fun startsServiceOnStartupIfEnabled() {
        Prefs.set(Settings.SERVICE_ENABLED_KEY, true)
        SettingsUi.open()
        Service.assertRunning()
    }
    
    @Test
    fun doesntStartServiceOnStartupIfDisabled() {
        Prefs.set(Settings.SERVICE_ENABLED_KEY, false)
        SettingsUi.open()
        Service.assertStopped()
    }
    
    @Test
    fun disablingAndEnabledServiceStopsAndStartsService() {
        SettingsUi.open()
        SettingsUi.assertServiceEnabled()
        
        // turn off
        SettingsUi.serviceEnabledSwitch.click()
        SettingsUi.serviceEnabledSwitch.wait(Until.checked(false), TIMEOUT)
        Service.assertStopped()
        
        // turn on
        SettingsUi.serviceEnabledSwitch.click()
        SettingsUi.serviceEnabledSwitch.wait(Until.checked(true), TIMEOUT)
        Service.assertRunning()
    }
    
    @Test
    fun disablingAndEnabledServiceExternallyUpdatesUi() {
        SettingsUi.open()
        SettingsUi.assertServiceEnabled()
        
        // turn off
        Prefs.set(Settings.SERVICE_ENABLED_KEY, false)
        SettingsUi.serviceEnabledSwitch.wait(Until.checked(false), TIMEOUT)
        
        // turn on
        Prefs.set(Settings.SERVICE_ENABLED_KEY, true)
        SettingsUi.serviceEnabledSwitch.wait(Until.checked(true), TIMEOUT)
    }
}