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

package xyz.sommd.automute.utils

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SeekBarPreference
import xyz.sommd.automute.R
import kotlin.math.roundToInt

/**
 * A [SeekBarPreference] that persists it's value as the float distance between [getMin] and
 * [getMax].
 *
 * E.g. if [getMin] is `10`, [getMax] is `20` and [getValue] is `15`, then [floatValue] will be
 * `0.5`.
 */
class FloatSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.seekBarPreferenceStyle,
    defStyleRes: Int = 0
): SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {
    var floatValue: Float
        get() = toFloat(value)
        set(value) = setValue(fromFloat(value))
    
    override fun getPersistedInt(defaultReturnValue: Int): Int {
        val defaultFloat = toFloat(defaultReturnValue)
        val floatValue = getPersistedFloat(defaultFloat)
        return fromFloat(floatValue)
    }
    
    override fun persistInt(value: Int): Boolean {
        val floatValue = toFloat(value)
        return persistFloat(floatValue)
    }
    
    private fun toFloat(value: Int) = (value.toFloat() - min) / (max - min)
    
    private fun fromFloat(value: Float) = (value * (max - min) + min).roundToInt()
}