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

package xyz.sommd.automute.service

import android.media.AudioAttributes

enum class AudioType {
    MUSIC,
    MEDIA,
    ASSISTANT,
    GAME,
    UNKNOWN;
    
    companion object {
        fun from(audioAttributes: AudioAttributes) = when (audioAttributes.usage) {
            AudioAttributes.USAGE_GAME -> GAME
            AudioAttributes.USAGE_ASSISTANT,
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> ASSISTANT
            AudioAttributes.USAGE_MEDIA -> when (audioAttributes.contentType) {
                AudioAttributes.CONTENT_TYPE_MUSIC -> MUSIC
                else -> MEDIA
            }
            AudioAttributes.USAGE_UNKNOWN -> when (audioAttributes.contentType) {
                AudioAttributes.CONTENT_TYPE_MUSIC -> MUSIC
                AudioAttributes.CONTENT_TYPE_MOVIE -> MEDIA
                else -> UNKNOWN
            }
            else -> UNKNOWN
        }
    }
}