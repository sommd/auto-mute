package xyz.sommd.automute.utils

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** A [BroadcastReceiver] that initialises the [Application] by doing nothing. */
class AutoStartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}