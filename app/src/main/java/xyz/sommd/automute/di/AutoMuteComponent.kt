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

package xyz.sommd.automute.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import xyz.sommd.automute.service.AutoMuteService
import xyz.sommd.automute.settings.Settings
import xyz.sommd.automute.settings.SettingsActivity
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidModule::class
])
interface AutoMuteComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder
        
        fun build(): AutoMuteComponent
    }
    
    // Globals
    val settings: Settings
    
    // Injectors
    fun inject(target: AutoMuteService)
    fun inject(target: SettingsActivity.SettingsFragment)
}