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

package xyz.sommd.automute.utils.monitors

import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import xyz.sommd.automute.utils.log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for monitoring [AudioPlaybackConfiguration]s.
 *
 * Unlike [AudioManager.AudioPlaybackCallback], [AudioPlaybackMonitor] uses an interface and
 * notifies it when an [AudioPlaybackConfiguration] is started or stopped.
 *
 * @param audioManager The [AudioManager] to use.
 * @param handler The [Handler] for the thread on which to execute listeners.
 */
@Singleton
class AudioPlaybackMonitor @Inject constructor(
    private val audioManager: AudioManager,
    private val handler: Handler
): AbstractMonitor<AudioPlaybackMonitor.Listener>() {
    companion object {
        /** Interval to run [recheckRunnable] in milliseconds. */
        const val RECHECK_INTERVAL = 30_000L
    }
    
    interface Listener {
        /**
         * Called when a new [AudioPlaybackConfiguration] is added.
         */
        fun onAudioPlaybackStarted(config: AudioPlaybackConfiguration) {}
        
        /**
         * Called when an [AudioPlaybackConfiguration] is removed.
         */
        fun onAudioPlaybackStopped(config: AudioPlaybackConfiguration) {}
        
        /**
         * Called after [onAudioPlaybackStarted] and [onAudioPlaybackStopped] with all
         * [AudioPlaybackConfiguration]s.
         */
        fun onAudioPlaybacksChanged(configs: Set<AudioPlaybackConfiguration>) {}
    }
    
    /** [MutableSet] to keep track of current [AudioPlaybackConfiguration]s. */
    private val _playbackConfigs = mutableSetOf<AudioPlaybackConfiguration>()
    
    /** [AudioManager.AudioPlaybackCallback] to monitor for [AudioPlaybackConfiguration] changes. */
    private val playbackConfigCallback = object: AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            updatePlaybackConfigurations(configs)
        }
    }
    
    /** [Runnable] to continuously check [AudioManager.getActivePlaybackConfigurations]. */
    private val recheckRunnable = Runnable { recheck() }
    
    /** [Set] of all current [AudioPlaybackConfiguration]s. */
    val playbackConfigs: Set<AudioPlaybackConfiguration> = _playbackConfigs
    
    /**
     * Register this [AudioPlaybackMonitor] to start monitoring [AudioPlaybackConfiguration]
     * changes.
     */
    override fun start() {
        // Add playback configs to track only future changes
        _playbackConfigs.addAll(audioManager.activePlaybackConfigurations)
        
        // Register playbackConfigCallback on handler thread
        audioManager.registerAudioPlaybackCallback(playbackConfigCallback, handler)
    }
    
    /**
     * Stop monitoring [AudioPlaybackConfiguration] changes.
     */
    override fun stop() {
        // Stop rechecking
        stopRechecking()
        
        // Stop listening and clear current playback configs
        audioManager.unregisterAudioPlaybackCallback(playbackConfigCallback)
        _playbackConfigs.clear()
    }
    
    /** Update [playbackConfigs] and notify [listeners] of any changes. */
    private fun updatePlaybackConfigurations(newConfigs: List<AudioPlaybackConfiguration>) {
        // Add new playback configs and call Listener.onAudioPlaybackStarted for each
        for (config in newConfigs) {
            if (config !in _playbackConfigs) {
                _playbackConfigs.add(config)
                listeners.forEach { it.onAudioPlaybackStarted(config) }
            }
        }
        
        // Remove old playback configs and call Listener.onAudioPlaybackStopped for each
        val iter = _playbackConfigs.iterator()
        for (config in iter) {
            if (config !in newConfigs) {
                iter.remove()
                listeners.forEach { it.onAudioPlaybackStopped(config) }
            }
        }
        
        // Notify audio playbacks changed
        listeners.forEach { it.onAudioPlaybacksChanged(playbackConfigs) }
        
        if (playbackConfigs.isNotEmpty()) {
            // Start rechecking now that audio is playing
            startRechecking()
        } else {
            // Stop rechecking since no more audio is playing
            stopRechecking()
        }
    }
    
    /**
     * Recheck [AudioManager.getActivePlaybackConfigurations] to make sure audio is still playing.
     * This is a workaround for [AOSP issue 93227199](https://issuetracker.google.com/issues/93227199),
     * which was [fixed in Android 9](https://android.googlesource.com/platform/frameworks/base/+/2a28126af931554a8621341149b86cc54773c71a).
     */
    private fun recheck() {
        log { "Rechecking audio playback configurations" }
        
        // Recheck playback configs
        val playbackConfigs = audioManager.activePlaybackConfigurations
        updatePlaybackConfigurations(playbackConfigs)
        
        // Continue rechecking if audio is still playing
        if (playbackConfigs.isNotEmpty()) {
            handler.postDelayed(recheckRunnable, RECHECK_INTERVAL)
        }
    }
    
    /**
     * Schedule [recheckRunnable] to be run after [RECHECK_INTERVAL].
     */
    private fun startRechecking() {
        // Fixed in Android 9
        if (Build.VERSION.SDK_INT < 28) {
            // Cancel the current recheck if scheduled
            handler.removeCallbacks(recheckRunnable)
            
            handler.postDelayed(recheckRunnable, RECHECK_INTERVAL)
        }
    }
    
    /**
     * Cancel [recheckRunnable] if it is scheduled.
     */
    private fun stopRechecking() {
        handler.removeCallbacks(recheckRunnable)
    }
}