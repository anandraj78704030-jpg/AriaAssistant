package com.aria.assistant.actions

import android.content.Context
import com.aria.assistant.accessibility.ScreenReader
import com.aria.assistant.core.AriaResponse
import com.aria.assistant.core.ConversationContext
import com.aria.assistant.core.Emotion
import com.aria.assistant.core.Vibe.pick
import com.aria.assistant.memory.MemoryCategory
import com.aria.assistant.memory.MemoryStore

/**
 * Intercepts recognized speech BEFORE PersonalityEngine to check if
 * it's a real action command (app control, on-screen selection, or a
 * memory command). Returns null when it isn't, so the ViewModel falls
 * back to conversational responses unchanged.
 */
object ActionRouter {

    private val ordinals = mapOf(
        "pehla" to 0, "pehle" to 0, "first" to 0,
        "doosra" to 1, "dusra" to 1, "second" to 1,
        "teesra" to 2, "tisra" to 2, "third" to 2,
        "chautha" to 3, "fourth" to 3,
        "paanchwa" to 4, "fifth" to 4
    )

    fun tryExecute(
        text: String,
        context: Context,
        conversationContext: ConversationContext,
        memoryStore: MemoryStore
    ): AriaResponse? {
        val lower = text.lowercase().trim()

        val opensIntent = containsAny(lower, "kholo", "open", "start karo", "chalu karo")
        if (opensIntent) {
            val resolvedApp = AppCatalog.find(lower) ?: InstalledAppFinder.find(context, lower)
            return if (resolvedApp != null) {
                conversationContext.rememberApp(resolvedApp.displayName)
                AppLauncher.open(context, resolvedApp).toResponse()
            } else {
                AriaResponse("Hmm, ye app mujhe nahi mila — naam thoda clearly bologe?", Emotion.CONCERNED)
            }
        }

        if (containsAny(lower, "flashlight", "torch")) {
            val turnOff = containsAny(lower, "band karo", "off karo", "off kar", "bandh")
            return FlashlightAction.toggle(context, turnOn = !turnOff).toResponse()
        }

        if ("volume" in lower) {
            return when {
                containsAny(lower, "mute", "silent", "band karo", "bandh karo") -> VolumeAction.mute(context).toResponse()
                containsAny(lower, "kam", "down", "low", "ghatao", "halka", "decrease") ->
                    VolumeAction.adjust(context, raise = false).toResponse()
                containsAny(lower, "badha", "up", "high", "zyada", "increase", "loud", "tez") ->
                    VolumeAction.adjust(context, raise = true).toResponse()
                else -> AriaResponse("Achha, volume badhau ya kam karoon?", Emotion.PROFESSIONAL)
            }
        }

        if ("brightness" in lower) {
            val percentMatch = Regex("(\\d{1,3})\\s*%").find(lower)
            return when {
                percentMatch != null ->
                    BrightnessAction.setBrightness(context, percentMatch.groupValues[1].toInt()).toResponse()
                containsAny(lower, "kam", "down", "low", "ghatao") ->
                    BrightnessAction.adjustRelative(context, -20).toResponse()
                containsAny(lower, "badhao", "up", "high", "zyada") ->
                    BrightnessAction.adjustRelative(context, +20).toResponse()
                else -> AriaResponse("Achha, brightness kitna percent karoon? Jaise 'brightness 50% kar do'.", Emotion.PROFESSIONAL)
            }
        }

        if ("alarm" in lower) {
            val time = extractTime(lower)
                ?: return AriaResponse("Achha, alarm ke liye time bata do — jaise '7 baje alarm laga do'.", Emotion.PROFESSIONAL)
            return AlarmAction.setAlarm(context, time.first, time.second).toResponse()
        }

        val digits = Regex("\\d{6,}").find(lower.replace(" ", ""))
        if (digits != null && containsAny(lower, "call", "dial", "phone karo", "call karo")) {
            return CallAction.dial(context, digits.value).toResponse()
        }

        if (containsAny(lower, "option", "select karo", "select kar do", "choose karo", "select kar")) {
            return handleScreenCommand(lower, context)
        }

        val memoryResponse = handleMemoryCommand(lower, context, conversationContext, memoryStore)
        if (memoryResponse != null) return memoryResponse

        return null
    }

    private fun handleMemoryCommand(
        lower: String,
        context: Context,
        conversationContext: ConversationContext,
        memoryStore: MemoryStore
    ): AriaResponse? {

        if ("yaad rakho" in lower && containsAny(lower, "favorite app", "favourite app", "usual app", "pasandida app")) {
            val app = AppCatalog.find(lower) ?: InstalledAppFinder.find(context, lower)
            return if (app != null) {
                memoryStore.add(MemoryCategory.PREFERENCE, "favorite_app", "${app.displayName}::${app.packageName}")
                AriaResponse(
                    pick(
                        "Achha, yaad rakh liya — ${app.displayName} tumhara favorite app hai!",
                        "Done! ${app.displayName} yaad rakh liya maine."
                    ),
                    Emotion.WARM
                )
            } else {
                AriaResponse("Kaunsa app yaad rakhoon? App ka naam bhi bolo na.", Emotion.PROFESSIONAL)
            }
        }

        if (containsAny(lower, "usual playlist", "mera favorite app kholo", "meri favorite app kholo", "usual app chalao", "favorite app chalao")) {
            val remembered = memoryStore.find(MemoryCategory.PREFERENCE, "favorite_app")
            return if (remembered != null) {
                val parts = remembered.value.split("::")
                val info = AppInfo(parts.getOrElse(0) { "App" }, parts.getOrElse(1) { "" })
                conversationContext.rememberApp(info.displayName)
                AppLauncher.open(context, info).toResponse()
            } else {
                AriaResponse(
                    "Hmm, maine abhi tak koi favorite app yaad nahi rakha. Pehle bolo 'yaad rakho mera favorite app <naam> hai'.",
                    Emotion.PROFESSIONAL
                )
            }
        }

        if (containsAny(lower, "sab bhula do", "puri memory clear karo", "sab memory delete karo", "memory clear karo")) {
            memoryStore.clearAll()
            return AriaResponse(pick("Achha, sab kuch bhula diya.", "Done! Saaf kar diya sab."), Emotion.CALM)
        }

        if ("bhula do" in lower) {
            val note = lower.replace("bhula do", "").trim()
            val match = memoryStore.getAll().firstOrNull { note.isNotBlank() && it.value.contains(note, ignoreCase = true) }
            return if (match != null) {
                memoryStore.delete(match.id)
                AriaResponse("Achha, \"${match.value}\" bhula diya.", Emotion.CALM)
            } else {
                AriaResponse("Hmm, ye mujhe yaad nahi mila jo bhulana hai.", Emotion.CONCERNED)
            }
        }

        if (containsAny(lower, "kya yaad hai", "meri memory dikhao", "memory dikhao", "saari baatein batao")) {
            val entries = memoryStore.getAll()
            return if (entries.isEmpty()) {
                AriaResponse("Abhi tak maine kuch yaad nahi rakha.", Emotion.CALM)
            } else {
                val list = entries.takeLast(5).joinToString("; ") { it.value }
                AriaResponse("Achha, ye yaad rakha hai maine: $list", Emotion.CALM)
            }
        }

        if ("yaad rakho" in lower) {
            val note = lower.replace("yaad rakho", "").trim()
            return if (note.isNotBlank()) {
                memoryStore.add(MemoryCategory.PREFERENCE, "note-${System.currentTimeMillis()}", note)
                AriaResponse(pick("Achha, yaad rakh liya: \"$note\".", "Done! Ye note kar liya: \"$note\"."), Emotion.WARM)
            } else {
                AriaResponse("Kya yaad rakhoon? Bolo 'yaad rakho ...'.", Emotion.PROFESSIONAL)
            }
        }

        return null
    }

    private fun handleScreenCommand(lower: String, context: Context): AriaResponse {
        if (!ScreenReader.isEnabled()) {
            ScreenReader.requestEnable(context)
            return AriaResponse(
                "Achha, screen samajhne ke liye mujhe Accessibility permission chahiye — maine Settings khol di hai. Wahan 'Aria' dhoondh ke ON kar dena, phir dobara try karna.",
                Emotion.PROFESSIONAL
            )
        }

        val priceMatch = Regex("(\\d{2,6})\\s*(rupee|rupees|rs)").find(lower)
            ?: Regex("(rupee|rupees|rs)\\.?\\s*(\\d{2,6})").find(lower)
        if (priceMatch != null) {
            val digits = priceMatch.groupValues.drop(1).firstOrNull { it.toIntOrNull() != null } ?: ""
            val found = ScreenReader.clickContaining(digits)
            return if (found != null) {
                AriaResponse(pick("Ek sec... \"${found.text}\" select kar diya!", "Done! \"${found.text}\" ho gaya."), Emotion.CALM)
            } else {
                AriaResponse("Hmm, screen par $digits rupees wala option nahi mila.", Emotion.CONCERNED)
            }
        }

        val ordinalKey = ordinals.keys.firstOrNull { it in lower }
        if (ordinalKey != null) {
            val index = ordinals.getValue(ordinalKey)
            val target = ScreenReader.clickByIndex(index)
            return if (target != null) {
                AriaResponse(pick("Ek sec... \"${target.text}\" select kar diya!", "Done! \"${target.text}\" ho gaya."), Emotion.CALM)
            } else {
                AriaResponse("Hmm, screen par itne options nahi mile.", Emotion.CONCERNED)
            }
        }

        return AriaResponse(
            "Achha, kaunsa option? Jaise 'doosra option' ya 'jisme 499 rupees likha hai'.",
            Emotion.PROFESSIONAL
        )
    }

    private fun ActionResult.toResponse(): AriaResponse = when (this) {
        is ActionResult.Success -> AriaResponse(message, Emotion.CALM)
        is ActionResult.Failure -> AriaResponse(message, Emotion.CONCERNED)
    }

    private fun extractTime(lower: String): Pair<Int, Int>? {
        val hm = Regex("(\\d{1,2})[:.](\\d{2})").find(lower)
        if (hm != null) {
            var h = hm.groupValues[1].toInt()
            val m = hm.groupValues[2].toInt()
            if ("pm" in lower && h < 12) h += 12
            return h to m
        }
        val hOnly = Regex("(\\d{1,2})\\s*(baje|pm|am)?").find(lower)
        val hourText = hOnly?.groupValues?.get(1)
        if (!hourText.isNullOrBlank()) {
            var h = hourText.toIntOrNull() ?: return null
            if (h !in 0..23) return null
            if ("pm" in lower && h < 12) h += 12
            if (h < 12 && containsAny(lower, "shaam", "evening", "raat", "night")) h += 12
            return h to 0
        }
        return null
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean = phrases.any { it in text }
}
