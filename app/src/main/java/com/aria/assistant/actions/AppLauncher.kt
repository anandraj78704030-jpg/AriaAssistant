package com.aria.assistant.actions

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings
import com.aria.assistant.core.Vibe.pick

object AppLauncher {
    fun open(context: Context, app: AppInfo): ActionResult {
        return try {
            val intent: Intent = when (app.displayName) {
                "Camera" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                "Settings" -> Intent(Settings.ACTION_SETTINGS)
                else -> context.packageManager.getLaunchIntentForPackage(app.packageName)
                    ?: return ActionResult.Failure("Hmm, ${app.displayName} is phone mein installed nahi hai.")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult.Success(
                pick(
                    "Haan haan, ${app.displayName} khol rahi hoon!",
                    "Ek sec... ${app.displayName} ready!",
                    "Done! ${app.displayName} khul gaya."
                )
            )
        } catch (e: Exception) {
            ActionResult.Failure("Arey, ${app.displayName} open nahi kar payi — kuch gadbad hui.")
        }
    }
}
