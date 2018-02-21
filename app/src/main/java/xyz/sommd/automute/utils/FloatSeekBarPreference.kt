package xyz.sommd.automute.utils

import android.content.Context
import android.support.v7.preference.SeekBarPreference
import android.util.AttributeSet
import kotlin.math.roundToInt

/**
 * A [SeekBarPreference] that persists it's value as the float distance between [getMin] and
 * [getMax].
 *
 * E.g. if [getMin] is `10`, [getMax] is `20` and [getValue] is `15`, then [floatValue] will be
 * `0.5`.
 */
class FloatSeekBarPreference: SeekBarPreference {
    constructor(context: Context): super(context)
    
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):
            super(context, attrs, defStyleAttr)
    
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int):
            super(context, attrs, defStyleAttr, defStyleRes)
    
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