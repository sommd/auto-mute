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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.content.systemService
import xyz.sommd.automute.App
import xyz.sommd.automute.R
import xyz.sommd.automute.settings.SettingsActivity

class Notifications(private val context: Context) {
    companion object {
        const val STATUS_CHANNEL = "status"
        
        const val STATUS_ID = 1
        
        fun from(context: Context) = App.from(context).notifications
    }
    
    private val notifManager = context.systemService<NotificationManager>()
    private val res = context.resources
    
    fun createChannels() {
        val statusChannel = NotificationChannel(
                STATUS_CHANNEL,
                res.getText(R.string.notif_channel_status_name),
                NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        
        notifManager.createNotificationChannel(statusChannel)
    }
    
    fun createStatusNotification(): Notification {
        return Notification.Builder(context, STATUS_CHANNEL).apply {
            setSmallIcon(R.drawable.ic_notif_status)
            setContentTitle(res.getText(R.string.notif_status_content_title))
            setContentText(res.getText(R.string.notif_status_content_text))
            setContentIntent(PendingIntent.getActivity(context, 0, Intent(
                    context, SettingsActivity::class.java
            ), 0))
        }.build()
    }
}