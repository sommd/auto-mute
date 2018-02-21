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