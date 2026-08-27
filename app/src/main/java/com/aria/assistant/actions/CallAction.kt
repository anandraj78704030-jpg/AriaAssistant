package com.aria.assistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.aria.assistant.core.Vibe.pick

object CallAction {
    fun dial(context: Context, number: String): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success(pick(
                "Ek sec... $number ke liye dialer khol diya — Call button dabao.",
                "Done! $number dial pad pe hai, bas Call dabana."
            ))
        } catch (e: Exception) {
            ActionResult.Failure("Hmm, dialer open nahi kar payi.")
        }
    }
}
