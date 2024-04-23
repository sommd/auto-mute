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

@RunWith(AndroidJUnit4::class)
@LargeTest
class AutoMuteIT {
    @Before
    fun before() {
        Audio.unmute()
        Prefs.reset()
        Prefs.disableAutoUnmute()
        Service.start()
    }
    
    @After
    fun after() {
        Service.stop()
    }
    
    @Test
    fun autoMutesAfterDelayWhenAudioStopsPlaying() {
        Audio.play { sleep(EPSILON) }
        
        sleep(AUTO_MUTE_DELAY - EPSILON)
        Audio.assertUnmuted()
        
        sleep(EPSILON * 2)
        Audio.assertMuted()
    }
}