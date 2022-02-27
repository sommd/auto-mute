/*
 * Copyright (C) 2022 David Sommerich
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

package xyz.sommd.automute.it

import android.app.ActivityManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.google.common.truth.Truth.assertWithMessage
import xyz.sommd.automute.BuildConfig
import xyz.sommd.automute.service.AutoMuteService
import xyz.sommd.automute.settings.Settings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val TIMEOUT = 5.seconds
val AUTO_MUTE_DELAY = 1.seconds
val EPSILON = 100.milliseconds

val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!
val context = ApplicationProvider.getApplicationContext<Context>()!!

fun sleep(duration: Duration) = Thread.sleep(duration.inWholeMilliseconds)

fun <R> UiDevice.wait(condition: SearchCondition<R>, timeout: Duration): R =
    wait(condition, timeout.inWholeMilliseconds)

fun <R> UiObject2.wait(condition: UiObject2Condition<R>, timeout: Duration): R =
    wait(condition, timeout.inWholeMilliseconds)

object Audio {
    private val SILENCE = Uri.parse(
        "data:audio/ogg;base64,T2dnUwACAAAAAAAAAACpxlwyAAAAAP1e+xQBHgF2b3JiaXMAAAAAAUSsAAAAAAAAAHcBAAAAAAC4AU9nZ1MAAAAAAAAAAAAAqcZcMgEAAACnmGgREDz//////////////////8kDdm9yYmlzLAAAAFhpcGguT3JnIGxpYlZvcmJpcyBJIDIwMTUwMTA1ICjim4Tim4Tim4Tim4QpAAAAAAEFdm9yYmlzKUJDVgEACAAAADFMIMWA0JBVAAAQAABgJCkOk2ZJKaWUoSh5mJRISSmllMUwiZiUicUYY4wxxhhjjDHGGGOMIDRkFQAABACAKAmOo+ZJas45ZxgnjnKgOWlOOKcgB4pR4DkJwvUmY26mtKZrbs4pJQgNWQUAAAIAQEghhRRSSCGFFGKIIYYYYoghhxxyyCGnnHIKKqigggoyyCCDTDLppJNOOumoo4466ii00EILLbTSSkwx1VZjrr0GXXxzzjnnnHPOOeecc84JQkNWAQAgAAAEQgYZZBBCCCGFFFKIKaaYcgoyyIDQkFUAACAAgAAAAABHkRRJsRTLsRzN0SRP8ixREzXRM0VTVE1VVVVVdV1XdmXXdnXXdn1ZmIVbuH1ZuIVb2IVd94VhGIZhGIZhGIZh+H3f933f930gNGQVACABAKAjOZbjKaIiGqLiOaIDhIasAgBkAAAEACAJkiIpkqNJpmZqrmmbtmirtm3LsizLsgyEhqwCAAABAAQAAAAAAKBpmqZpmqZpmqZpmqZpmqZpmqZpmmZZlmVZlmVZlmVZlmVZlmVZlmVZlmVZlmVZlmVZlmVZlmVZlmVZQGjIKgBAAgBAx3Ecx3EkRVIkx3IsBwgNWQUAyAAACABAUizFcjRHczTHczzHczxHdETJlEzN9EwPCA1ZBQAAAgAIAAAAAABAMRzFcRzJ0SRPUi3TcjVXcz3Xc03XdV1XVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVYHQkFUAAAQAACGdZpZqgAgzkGEgNGQVAIAAAAAYoQhDDAgNWQUAAAQAAIih5CCa0JrzzTkOmuWgqRSb08GJVJsnuamYm3POOeecbM4Z45xzzinKmcWgmdCac85JDJqloJnQmnPOeRKbB62p0ppzzhnnnA7GGWGcc85p0poHqdlYm3POWdCa5qi5FJtzzomUmye1uVSbc84555xzzjnnnHPOqV6czsE54Zxzzonam2u5CV2cc875ZJzuzQnhnHPOOeecc84555xzzglCQ1YBAEAAAARh2BjGnYIgfY4GYhQhpiGTHnSPDpOgMcgppB6NjkZKqYNQUhknpXSC0JBVAAAgAACEEFJIIYUUUkghhRRSSCGGGGKIIaeccgoqqKSSiirKKLPMMssss8wyy6zDzjrrsMMQQwwxtNJKLDXVVmONteaec645SGultdZaK6WUUkoppSA0ZBUAAAIAQCBkkEEGGYUUUkghhphyyimnoIIKCA1ZBQAAAgAIAAAA8CTPER3RER3RER3RER3RER3P8RxREiVREiXRMi1TMz1VVFVXdm1Zl3Xbt4Vd2HXf133f141fF4ZlWZZlWZZlWZZlWZZlWZZlCUJDVgEAIAAAAEIIIYQUUkghhZRijDHHnINOQgmB0JBVAAAgAIAAAAAAR3EUx5EcyZEkS7IkTdIszfI0T/M00RNFUTRNUxVd0RV10xZlUzZd0zVl01Vl1XZl2bZlW7d9WbZ93/d93/d93/d93/d939d1IDRkFQAgAQCgIzmSIimSIjmO40iSBISGrAIAZAAABACgKI7iOI4jSZIkWZImeZZniZqpmZ7pqaIKhIasAgAAAQAEAAAAAACgaIqnmIqniIrniI4oiZZpiZqquaJsyq7ruq7ruq7ruq7ruq7ruq7ruq7ruq7ruq7ruq7ruq7ruq7rukBoyCoAQAIAQEdyJEdyJEVSJEVyJAcIDVkFAMgAAAgAwDEcQ1Ikx7IsTfM0T/M00RM90TM9VXRFFwgNWQUAAAIACAAAAAAAwJAMS7EczdEkUVIt1VI11VItVVQ9VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV1TRN0zSB0JCVAAAZAAAjQQYZhBCKcpBCbj1YCDHmJAWhOQahxBiEpxAzDDkNInSQQSc9uJI5wwzz4FIoFURMg40lN44gDcKmXEnlOAhCQ1YEAFEAAIAxyDHEGHLOScmgRM4xCZ2UyDknpZPSSSktlhgzKSWmEmPjnKPSScmklBhLip2kEmOJrQAAgAAHAIAAC6HQkBUBQBQAAGIMUgophZRSzinmkFLKMeUcUko5p5xTzjkIHYTKMQadgxAppRxTzinHHITMQeWcg9BBKAAAIMABACDAQig0ZEUAECcA4HAkz5M0SxQlSxNFzxRl1xNN15U0zTQ1UVRVyxNV1VRV2xZNVbYlTRNNTfRUVRNFVRVV05ZNVbVtzzRl2VRV3RZV1bZl2xZ+V5Z13zNNWRZV1dZNVbV115Z9X9ZtXZg0zTQ1UVRVTRRV1VRV2zZV17Y1UXRVUVVlWVRVWXZlWfdVV9Z9SxRV1VNN2RVVVbZV2fVtVZZ94XRVXVdl2fdVWRZ+W9eF4fZ94RhV1dZN19V1VZZ9YdZlYbd13yhpmmlqoqiqmiiqqqmqtm2qrq1bouiqoqrKsmeqrqzKsq+rrmzrmiiqrqiqsiyqqiyrsqz7qizrtqiquq3KsrCbrqvrtu8LwyzrunCqrq6rsuz7qizruq3rxnHrujB8pinLpqvquqm6um7runHMtm0co6rqvirLwrDKsu/rui+0dSFRVXXdlF3jV2VZ921fd55b94WybTu/rfvKceu60vg5z28cubZtHLNuG7+t+8bzKz9hOI6lZ5q2baqqrZuqq+uybivDrOtCUVV9XZVl3zddWRdu3zeOW9eNoqrquirLvrDKsjHcxm8cuzAcXds2jlvXnbKtC31jyPcJz2vbxnH7OuP2daOvDAnHjwAAgAEHAIAAE8pAoSErAoA4AQAGIecUUxAqxSB0EFLqIKRUMQYhc05KxRyUUEpqIZTUKsYgVI5JyJyTEkpoKZTSUgehpVBKa6GU1lJrsabUYu0gpBZKaS2U0lpqqcbUWowRYxAy56RkzkkJpbQWSmktc05K56CkDkJKpaQUS0otVsxJyaCj0kFIqaQSU0mptVBKa6WkFktKMbYUW24x1hxKaS2kEltJKcYUU20txpojxiBkzknJnJMSSmktlNJa5ZiUDkJKmYOSSkqtlZJSzJyT0kFIqYOOSkkptpJKTKGU1kpKsYVSWmwx1pxSbDWU0lpJKcaSSmwtxlpbTLV1EFoLpbQWSmmttVZraq3GUEprJaUYS0qxtRZrbjHmGkppraQSW0mpxRZbji3GmlNrNabWam4x5hpbbT3WmnNKrdbUUo0txppjbb3VmnvvIKQWSmktlNJiai3G1mKtoZTWSiqxlZJabDHm2lqMOZTSYkmpxZJSjC3GmltsuaaWamwx5ppSi7Xm2nNsNfbUWqwtxppTS7XWWnOPufVWAADAgAMAQIAJZaDQkJUAQBQAAEGIUs5JaRByzDkqCULMOSepckxCKSlVzEEIJbXOOSkpxdY5CCWlFksqLcVWaykptRZrLQAAoMABACDABk2JxQEKDVkJAEQBACDGIMQYhAYZpRiD0BikFGMQIqUYc05KpRRjzknJGHMOQioZY85BKCmEUEoqKYUQSkklpQIAAAocAAACbNCUWByg0JAVAUAUAABgDGIMMYYgdFQyKhGETEonqYEQWgutddZSa6XFzFpqrbTYQAithdYySyXG1FpmrcSYWisAAOzAAQDswEIoNGQlAJAHAEAYoxRjzjlnEGLMOegcNAgx5hyEDirGnIMOQggVY85BCCGEzDkIIYQQQuYchBBCCKGDEEIIpZTSQQghhFJK6SCEEEIppXQQQgihlFIKAAAqcAAACLBRZHOCkaBCQ1YCAHkAAIAxSjkHoZRGKcYglJJSoxRjEEpJqXIMQikpxVY5B6GUlFrsIJTSWmw1dhBKaS3GWkNKrcVYa64hpdZirDXX1FqMteaaa0otxlprzbkAANwFBwCwAxtFNicYCSo0ZCUAkAcAgCCkFGOMMYYUYoox55xDCCnFmHPOKaYYc84555RijDnnnHOMMeecc845xphzzjnnHHPOOeecc44555xzzjnnnHPOOeecc84555xzzgkAACpwAAAIsFFkc4KRoEJDVgIAqQAAABFWYowxxhgbCDHGGGOMMUYSYowxxhhjbDHGGGOMMcaYYowxxhhjjDHGGGOMMcYYY4wxxhhjjDHGGGOMMcYYY4wxxhhjjDHGGGOMMcYYY4wxxhhjjDHGGFtrrbXWWmuttdZaa6211lprrQBAvwoHAP8HG1ZHOCkaCyw0ZCUAEA4AABjDmHOOOQYdhIYp6KSEDkIIoUNKOSglhFBKKSlzTkpKpaSUWkqZc1JSKiWlllLqIKTUWkottdZaByWl1lJqrbXWOgiltNRaa6212EFIKaXWWostxlBKSq212GKMNYZSUmqtxdhirDGk0lJsLcYYY6yhlNZaazHGGGstKbXWYoy1xlprSam11mKLNdZaCwDgbnAAgEiwcYaVpLPC0eBCQ1YCACEBAARCjDnnnHMQQgghUoox56CDEEIIIURKMeYcdBBCCCGEjDHnoIMQQgghhJAx5hx0EEIIIYQQOucchBBCCKGEUkrnHHQQQgghlFBC6SCEEEIIoYRSSikdhBBCKKGEUkopJYQQQgmllFJKKaWEEEIIoYQSSimllBBCCKWUUkoppZQSQgghlFJKKaWUUkIIoZRQSimllFJKCCGEUkoppZRSSgkhhFBKKaWUUkopIYQSSimllFJKKaUAAIADBwCAACPoJKPKImw04cIDUGjISgCADAAAcdhq6ynWyCDFnISWS4SQchBiLhFSijlHsWVIGcUY1ZQxpRRTUmvonGKMUU+dY0oxw6yUVkookYLScqy1dswBAAAgCAAwECEzgUABFBjIAIADhAQpAKCwwNAxXAQE5BIyCgwKx4Rz0mkDABCEyAyRiFgMEhOqgaJiOgBYXGDIB4AMjY20iwvoMsAFXdx1IIQgBCGIxQEUkICDE2544g1PuMEJOkWlDgIAAAAAAAEAHgAAkg0gIiKaOY4Ojw+QEJERkhKTE5QAAAAAAOABgA8AgCQFiIiIZo6jw+MDJERkhKTE5AQlAAAAAAAAAAAACAgIAAAAAAAEAAAACAhPZ2dTAARErAAAAAAAAKnGXDICAAAAPwIDXS0BAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEACg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4ODg4="
    )
    
    private val manager = context.getSystemService<AudioManager>()!!
    
    fun assertUnmuted(streamType: Int = AudioManager.STREAM_MUSIC) {
        assertWithMessage("Expected audio stream $streamType to be unmuted")
            .that(manager.isStreamMute(streamType)).isFalse()
    }
    
    fun assertMuted(streamType: Int = AudioManager.STREAM_MUSIC) {
        assertWithMessage("Expected audio stream $streamType to be muted")
            .that(manager.isStreamMute(streamType)).isTrue()
    }
    
    inline fun play(
        usage: Int = AudioAttributes.USAGE_MEDIA,
        contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC,
        block: (MediaPlayer) -> Unit
    ) {
        val player = start(usage, contentType)
        try {
            block(player)
        } finally {
            player.stop()
            player.release()
        }
    }
    
    fun start(
        usage: Int = AudioAttributes.USAGE_MEDIA,
        contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC
    ) = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build()
        )
        setDataSource(context, SILENCE)
        isLooping = true
        
        prepare()
        start()
    }
    
    fun mute(streamType: Int = AudioManager.STREAM_MUSIC) {
        manager.adjustStreamVolume(streamType, AudioManager.ADJUST_MUTE, 0)
    }
    
    fun unmute(streamType: Int = AudioManager.STREAM_MUSIC) {
        manager.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, 0)
        manager.setStreamVolume(streamType, manager.getStreamMaxVolume(streamType) / 2, 0)
    }
}

object Prefs {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    
    fun reset() {
        prefs.edit {
            clear()
            putString(Settings.AUTO_MUTE_DELAY_KEY, AUTO_MUTE_DELAY.inWholeSeconds.toString())
        }
    }
    
    fun disableAutoMute() {
        set(Settings.AUTO_MUTE_ENABLED_KEY, false)
        set(Settings.AUTO_MUTE_HEADPHONES_UNPLUGGED_KEY, false)
    }
    
    fun disableAutoUnmute() {
        set(Settings.AUTO_UNMUTE_MUSIC_MODE_KEY, Settings.UnmuteMode.NEVER)
        set(Settings.AUTO_UNMUTE_MEDIA_MODE_KEY, Settings.UnmuteMode.NEVER)
        set(Settings.AUTO_UNMUTE_ASSISTANT_MODE_KEY, Settings.UnmuteMode.NEVER)
        set(Settings.AUTO_UNMUTE_GAME_MODE_KEY, Settings.UnmuteMode.NEVER)
    }
    
    fun set(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
    fun set(key: String, value: Enum<*>) = set(key, value.toString())
    fun set(key: String, value: String) = prefs.edit { putString(key, value) }
}

object Service {
    @Suppress("DEPRECATION")
    private val isRunning
        get() = context.getSystemService<ActivityManager>()!!
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == AutoMuteService::class.qualifiedName }
    
    fun assertRunning() =
        assertWithMessage("Expected AutoMuteService to be running").that(isRunning).isTrue()
    
    fun assertStopped() =
        assertWithMessage("Expected AutoMuteService to be stopped").that(isRunning).isFalse()
    
    fun start() {
        AutoMuteService.start(context)
        sleep(EPSILON) // give service a chance to start
    }
    
    fun stop() {
        AutoMuteService.stop(context)
        sleep(EPSILON) // give service a chance to stop
    }
}

object SettingsUi {
    private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    
    val serviceEnabledSwitch get() = getSwitchPref("Enable Service")
    
    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun getSwitchPref(label: String) = device
        .findObject(By.hasChild(By.hasChild(By.text(label))))
        .findObject(By.checkable(true))
        ?: assertWithMessage("Could not find '$label' pref").fail() as Nothing
    
    fun assertServiceEnabled() =
        assertWithMessage("Expected 'Enable Service' to be checked")
            .that(serviceEnabledSwitch.isChecked).isTrue()
    
    fun open() {
        context.startActivity(context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME))
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT)
    }
}

object SystemUi {
    private const val PACKAGE_NAME = "com.android.systemui"
    
    private val volumeDialogSelector = By.res(PACKAGE_NAME, "volume_dialog")
    
    fun assertVolumeDialogShown() =
        assertWithMessage("Expected system volume dialog to be shown")
            .that(device.hasObject(volumeDialogSelector)).isTrue()
    
    fun assertVolumeDialogNotShown() =
        assertWithMessage("Expected system volume dialog not to be shown")
            .that(device.hasObject(volumeDialogSelector)).isFalse()
    
    fun hideVolumeDialog() {
        device.pressHome()
        device.wait(Until.gone(volumeDialogSelector), TIMEOUT)
    }
}
