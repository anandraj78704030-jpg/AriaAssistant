package com.aria.assistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.aria.assistant.core.Vibe.pick

object BrightnessAction {
    fun setBrightness(context: Context, percent: Int): ActionResult {
        if (!Settings.System.canWrite(context)) return requestPermission(context)
        return try {
            val value = percent.coerceIn(0, 100) * 255 / 100
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            ActionResult.Success(pick("Done! Brightness $percent% kar diya.", "Achha, $percent% ho gaya."))
        } catch (e: Exception) {
            ActionResult.Failure("Hmm, brightness set nahi kar payi.")
        }
    }

    fun adjustRelative(context: Context, deltaPercent: Int): ActionResult {
        if (!Settings.System.canWrite(context)) return requestPermission(context)
        return try {
            val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            val currentPercent = current * 100 / 255
            val newPercent = (currentPercent + deltaPercent).coerceIn(0, 100)
            val value = newPercent * 255 / 100
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            ActionResult.Success(pick("Done! Brightness $newPercent% kar diya.", "Ek sec... $newPercent% ho gaya."))
        } catch (e: Exception) {
            ActionResult.Failure("Hmm, brightness adjust nahi kar payi.")
        }
    }

    private fun requestPermission(context: Context): ActionResult {
        return try {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            ActionResult.Failure(
                "Achha, brightness badalne ke liye mujhe 'Modify system settings' permission chahiye — Settings khol di hai, wahan Aria ko allow kar dena, phir dobara try karna."
            )
        } catch (e: Exception) {
            ActionResult.Failure("Hmm, permission settings nahi khul payi.")
        }
    }
}
