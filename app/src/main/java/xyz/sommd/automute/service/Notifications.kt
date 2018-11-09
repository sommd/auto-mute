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
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.util.SparseIntArray
import androidx.core.content.getSystemService
import androidx.core.util.getOrDefault
import androidx.core.util.set
import androidx.core.util.size
import xyz.sommd.automute.R
import xyz.sommd.automute.settings.SettingsActivity
import xyz.sommd.automute.utils.isVolumeOff
import xyz.sommd.automute.utils.log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifications @Inject constructor(
    private val context: Context,
    private val notifManager: NotificationManager = context.getSystemService()!!,
    private val audioManager: AudioManager = context.getSystemService()!!
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
        log { "Updating status notification" }
        
        notifManager.notify(STATUS_ID, createStatusNotification())
    }
    
    fun createStatusNotification(): Notification {
        val playbackConfigs = audioManager.activePlaybackConfigurations
        val typeCounts = countAudioTypes(playbackConfigs)
        val totalStreams = playbackConfigs.size
        val totalTypes = typeCounts.size
        
        val muted = audioManager.isVolumeOff()
        
        return Notification.Builder(context, STATUS_CHANNEL).apply {
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
                // Count of only audio type
                val typeOrdinal = typeCounts.keyAt(0)
                val typeCount = typeCounts.valueAt(0)
                setContentText(getTypeCountText(typeOrdinal, typeCount))
            } else {
                // Number of different audio types
                setContentText(
                    res.getQuantityString(
                        R.plurals.notif_status_total_types,
                        totalTypes, totalTypes
                    )
                )
                
                if (totalTypes > 0) {
                    val style = Notification.InboxStyle()
                    
                    // Counts of each audio type
                    for (i in 0 until typeCounts.size) {
                        val typeOrdinal = typeCounts.keyAt(i)
                        val typeCount = typeCounts.valueAt(i)
                        style.addLine(getTypeCountText(typeOrdinal, typeCount))
                    }
                    
                    setStyle(style)
                }
            }
            
            // Open SettingsActivity when clicked
            setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, SettingsActivity::class.java), 0
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
        }.build()
    }
    
    private fun countAudioTypes(playbackConfigs: List<AudioPlaybackConfiguration>): SparseIntArray {
        // Count number of streams of each type
        val typeCounts = SparseIntArray(0)
        for (config in playbackConfigs) {
            val ordinal = config.audioAttributes.audioType.ordinal
            typeCounts[ordinal] = typeCounts.getOrDefault(ordinal, 0) + 1
        }
        
        return typeCounts
    }
    
    private fun getTypeCountText(typeOrdinal: Int, typeCount: Int): CharSequence {
        val typeName = res.getStringArray(R.array.audio_type_names)[typeOrdinal]
        return res.getQuantityString(
            R.plurals.notif_status_type_count, typeCount,
            typeCount, typeName
        )
    }
    
    private fun buildAction(icon: Int, title: Int, action: String): Notification.Action {
        return Notification.Action.Builder(
            Icon.createWithResource(context, icon), res.getText(title),
            PendingIntent.getService(
                context, 0, Intent(
                    action, null, context, AutoMuteService::class.java
                ), 0
            )
        ).build()
    }
}