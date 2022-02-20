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

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertWithMessage
import xyz.sommd.automute.BuildConfig
import xyz.sommd.automute.service.AutoMuteService

const val TIMEOUT: Long = 5000
const val PACKAGE_NAME = BuildConfig.APPLICATION_ID

val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!
val context = ApplicationProvider.getApplicationContext<Context>()!!

object App {
    val serviceEnabledSwitch get() = getSwitchPref("Enable Service")
    
    private fun getSwitchPref(label: String) = device
        .findObject(By.hasChild(By.hasChild(By.text(label))))
        .findObject(By.checkable(true))!!
    
    fun open() {
        context.startActivity(context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME))
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT)
    }
    
    fun assertServiceEnabled() =
        assertWithMessage("Expected 'Enable Service' to be checked")
            .that(serviceEnabledSwitch.isChecked).isTrue()
}

object Service {
    val isRunning get() = info != null
    
    @Suppress("DEPRECATION")
    val info
        get() = context.getSystemService<ActivityManager>()!!.getRunningServices(Int.MAX_VALUE)
            .find { it.service.className == AutoMuteService::class.qualifiedName }
    
    fun assertRunning() =
        assertWithMessage("Expected AutoMuteService to be running").that(isRunning).isTrue()
    
    fun assertStopped() =
        assertWithMessage("Expected AutoMuteService to be stopped").that(isRunning).isFalse()
}

object Prefs {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    
    fun set(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }
    
    fun reset() {
        prefs.edit { clear() }
    }
}