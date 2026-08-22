package com.aria.assistant.actions

import android.content.Context
import android.media.AudioManager
import com.aria.assistant.core.Vibe.pick

object VolumeAction {
    fun adjust(context: Context, raise: Boolean): ActionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            ActionResult.Success(
                if (raise) pick("Haan, volume badha diya!", "Done! Thoda loud kar diya.")
                else pick("Achha, volume kam kar diya.", "Done! Halka kar diya.")
            )
        } catch (e: Exception) {
            ActionResult.Failure("Hmm, volume adjust nahi kar payi.")
        }
    }

    fun mute(context: Context): ActionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            ActionResult.Success(pick("Done! Mute kar diya.", "Achha, chup kar diya sabko."))
        } catch (e: Exception) {
            ActionResult.Failure("Hmm, mute nahi kar payi.")
        }
    }
}
