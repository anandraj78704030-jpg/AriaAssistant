package com.aria.assistant.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.aria.assistant.core.Vibe.pick

object AlarmAction {
    fun setAlarm(context: Context, hour: Int, minute: Int, message: String = "Aria alarm"): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) == null) {
                return ActionResult.Failure(
                    "Hmm, is device mein koi Clock/Alarm app nahi mila jo alarm set kar sake. Play Store se 'Clock' app install karke dekho."
                )
            }
            context.startActivity(intent)
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            val ampm = if (hour < 12) "AM" else "PM"
            ActionResult.Success(
                pick(
                    "Achha, alarm laga rahi hoon $h12:${minute.toString().padStart(2, '0')} $ampm ke liye — confirm kar dena!",
                    "Ek sec... $h12:${minute.toString().padStart(2, '0')} $ampm ka alarm set kar diya, bas confirm karna."
                )
            )
        } catch (e: Exception) {
            ActionResult.Failure("Hmm, alarm set nahi kar payi.")
        }
    }
}
