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

package xyz.sommd.automute.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import xyz.sommd.automute.R
import xyz.sommd.automute.settings.SettingsActivity
import xyz.sommd.automute.utils.isVolumeOff
import xyz.sommd.automute.utils.log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifications @Inject constructor(
    private val context: Context,
    private val notifManager: NotificationManager,
    private val audioManager: AudioManager
) {
    companion object {
        const val STATUS_CHANNEL = "status"
        
        const val STATUS_ID = 1
    }
    
    private val res = context.resources
    
    fun createChannels() {
        val statusChannel = NotificationChannel(
            STATUS_CHANNEL,
            res.getText(R.string.notif_channel_status_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        
        notifManager.createNotificationChannel(statusChannel)
    }
    
    fun updateStatusNotification() {
        if (notifManager.areNotificationsEnabled()) {
            log { "Updating status notification" }
            
            notifManager.notify(STATUS_ID, createStatusNotification())
        } else {
            log { "Notifications disabled, not updating status notification" }
        }
    }
    
    fun createStatusNotification(): Notification {
        val playbackConfigs = audioManager.activePlaybackConfigurations
        val totalStreams = playbackConfigs.size
        val typeCounts = countAudioTypes(playbackConfigs)
        val totalTypes = typeCounts.size
        
        val muted = audioManager.isVolumeOff()
        
        return NotificationCompat.Builder(context, STATUS_CHANNEL).apply {
            setSmallIcon(
                when {
                    muted -> R.drawable.ic_audio_mute
                    totalStreams == 0 -> R.drawable.ic_audio_unmute
                    else -> R.drawable.ic_audio_playing
                }
            )
            
            // Always the total number of streams playing
            setContentTitle(
                res.getQuantityString(
                    R.plurals.notif_status_total_streams,
                    totalStreams,
                    totalStreams
                )
            )
            
            if (totalTypes == 1) {
                setContentText(getTypeCountText(typeCounts.keys.first(), typeCounts.values.first()))
            } else {
                // Number of different audio types
                setContentText(
                    res.getQuantityString(
                        R.plurals.notif_status_total_types,
                        totalTypes, totalTypes
                    )
                )
                
                if (totalTypes > 0) {
                    val style = NotificationCompat.InboxStyle()
                    // Counts of each audio type
                    for ((type, count) in typeCounts) {
                        style.addLine(getTypeCountText(type, count))
                    }
                    
                    setStyle(style)
                }
            }
            
            // Open SettingsActivity when clicked
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, SettingsActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            
            // Mute/unmute action
            addAction(
                if (muted) {
                    buildAction(
                        R.drawable.ic_audio_unmute,
                        R.string.notif_status_action_unmute,
                        AutoMuteService.ACTION_UNMUTE
                    )
                } else {
                    buildAction(
                        R.drawable.ic_audio_mute,
                        R.string.notif_status_action_mute,
                        AutoMuteService.ACTION_MUTE
                    )
                }
            )
            
            // Show volume action
            addAction(
                buildAction(
                    R.drawable.ic_audio_show,
                    R.string.notif_status_action_show,
                    AutoMuteService.ACTION_SHOW
                )
            )
            
            setCategory(NotificationCompat.CATEGORY_STATUS)
            setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            setOngoing(true)
        }.build()
    }
    
    private fun countAudioTypes(playbackConfigs: List<AudioPlaybackConfiguration>): Map<AudioType?, Int> {
        val counts = playbackConfigs.groupingBy { it.audioAttributes.audioType }.eachCount()
        
        val typeCounts = mutableMapOf<AudioType?, Int>()
        AudioType.entries.forEach { type -> counts[type]?.let { typeCounts[type] = it } }
        counts[null]?.let { typeCounts[null] = it }
        
        return typeCounts
    }
    
    private fun getTypeCountText(type: AudioType?, count: Int): CharSequence {
        return res.getQuantityString(
            when (type) {
                AudioType.MUSIC -> R.plurals.notif_status_music_count
                AudioType.MEDIA -> R.plurals.notif_status_media_count
                AudioType.ASSISTANT -> R.plurals.notif_status_assistant_count
                AudioType.GAME -> R.plurals.notif_status_game_count
                null -> R.plurals.notif_status_other_count
            },
            count,
            count
        )
    }
    
    private fun buildAction(icon: Int, title: Int, action: String): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, icon),
            res.getText(title),
            PendingIntent.getService(
                context, 0, Intent(
                    action, null, context, AutoMuteService::class.java
                ), PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
    }
}