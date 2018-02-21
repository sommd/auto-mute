package xyz.sommd.automute.utils

import android.util.Log

inline fun <reified T> T.log(message: Any?, level: Int = Log.DEBUG) {
    Log.println(level, T::class.java.simpleName, message.toString())
}