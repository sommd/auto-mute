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

package xyz.sommd.automute.utils

import android.media.AudioManager
import kotlin.math.roundToInt

/**
 * Default volume stream to use if unspecified.
 */
const val STREAM_DEFAULT = AudioManager.STREAM_MUSIC

/**
 * Check if the given audio stream is muted or the volume is 0.
 */
fun AudioManager.isVolumeOff(stream: Int = AudioManager.STREAM_MUSIC) =
        isStreamMute(stream) || getStreamVolume(stream) == 0

/**
 * Check if headphones (or an other external audio device) is plugged in.
 */
val AudioManager.areHeadphonesPluggedIn: Boolean
    get() = isWiredHeadsetOn || isBluetoothA2dpOn

private fun getFlags(show: Boolean) = if (show) AudioManager.FLAG_SHOW_UI else 0

/**
 * Set the volume of [stream] to [fraction]. The actual volume index will be calculated using
 * [AudioManager.getStreamMaxVolume].
 */
fun AudioManager.setVolume(fraction: Float, stream: Int = STREAM_DEFAULT, show: Boolean = false) {
    val volume = (fraction * getStreamMaxVolume(stream)).roundToInt()
    val flags = getFlags(show)
    setStreamVolume(volume, stream, flags)
}

/**
 * Get the volume of [stream] as a fraction. The fraction will be calculated using
 * [AudioManager.getStreamMaxVolume].
 */
fun AudioManager.getVolume(stream: Int = STREAM_DEFAULT) =
        getStreamVolume(stream).toFloat() / getStreamMaxVolume(stream)

/**
 * Unmute the given audio [stream].
 *
 * If unmuting left the stream volume at 0 (which is likely if the user manually muted via the
 * system volume controls), then set stream's volume to the [defaultVolume].
 */
fun AudioManager.unmute(defaultVolume: Float = 0f, stream: Int = STREAM_DEFAULT,
                        show: Boolean = false) {
    val flags = getFlags(show)
    
    // Unmute stream
    adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, flags)
    
    // Set to default volume if volume is 0
    if (getStreamVolume(stream) == 0) {
        log { "Unmuted to 0, setting default volume" }
        
        setVolume(defaultVolume, stream, show)
    }
}

/**
 * Mute the given audio stream.
 */
fun AudioManager.mute(stream: Int = STREAM_DEFAULT, show: Boolean = false) {
    adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, getFlags(show))
}

/**
 * Show the system volume control UI for the given audio stream.
 */
fun AudioManager.showVolumeControl(stream: Int = STREAM_DEFAULT) {
    adjustStreamVolume(stream, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
}
