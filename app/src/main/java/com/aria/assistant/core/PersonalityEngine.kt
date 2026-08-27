package com.aria.assistant.core

import com.aria.assistant.actions.AppCatalog
import com.aria.assistant.core.Vibe.pick

data class AriaResponse(val text: String, val emotion: Emotion)

/**
 * Aria's conversational voice — warm, expressive, best-friend energy.
 * Reacts first, answers second. Uses natural Hindi/Hinglish fillers
 * instead of flat confirmations. Real Android actions are handled by
 * ActionRouter BEFORE this is ever called — see AssistantViewModel.
 */
object PersonalityEngine {

    fun respond(input: String, context: ConversationContext): AriaResponse {
        val lower = input.lowercase().trim()

        val batteryMatch = Regex("(\\d{1,3})\\s*%").find(lower)
        if (batteryMatch != null && ("battery" in lower || "charge" in lower)) {
            val percent = batteryMatch.groupValues[1].toIntOrNull() ?: 100
            return when {
                percent <= 15 -> AriaResponse(
                    pick(
                        "Arey yaar, sirf $percent%?! Battery Saver on kar doon?",
                        "Oho, $percent% bacha hai — jaldi charge pe laga do isse."
                    ),
                    Emotion.CONCERNED
                )
                percent <= 40 -> AriaResponse(
                    "Achha, $percent% hai — abhi theek hai, but charge jald kar lena.",
                    Emotion.CALM
                )
                else -> AriaResponse(
                    "Bilkul $percent%! Sab set hai. 😊",
                    Emotion.CALM
                )
            }
        }

        if (("battery" in lower || "charge" in lower) &&
            ("kitna" in lower || "kya" in lower || "how much" in lower || "what" in lower || "check" in lower)
        ) {
            return AriaResponse(
                "Hmm... abhi battery check nahi kar sakti — woh aage kisi stage mein aayega jab main Android se real data padh sakoongi.",
                Emotion.PROFESSIONAL
            )
        }

        val mentionedApp = AppCatalog.find(lower)?.displayName

        // "Search X" — uses the app named in this sentence, or falls back
        // to whatever app was last opened in the conversation
        if (containsAny(lower, "search", "dhoondo", "khojo", "find karo")) {
            val app = mentionedApp ?: context.lastApp
            return if (app != null) {
                if (mentionedApp != null) context.rememberApp(app)
                val query = extractSearchQuery(lower)
                AriaResponse(
                    "Achha, \"$query\" $app par search kar rahi hoon. (Abhi sirf samajh rahi hoon — real search typing aage aayegi.)",
                    Emotion.PROFESSIONAL
                )
            } else {
                AriaResponse(
                    "Kis app mein search karoon? Pehle app ka naam bata do na.",
                    Emotion.PROFESSIONAL
                )
            }
        }

        return when {
            containsAny(lower, "target complete", "target achieve", "kar liya", "ho gaya", "done kar diya", "finish kar diya") ->
                AriaResponse(
                    pick(
                        "Yay! Haan ho gaya — mast kaam kiya! 🎉",
                        "Let's go! Bahut badhiya kiya tumne.",
                        "Wah! Proud of you yaar, ekdum sahi."
                    ),
                    Emotion.EXCITED
                )

            containsAny(lower, "hello", "hii", "hi aria", "namaste", "hey aria", " hi ") || lower == "hi" ->
                AriaResponse(
                    pick(
                        "Haan bolo! Kya karna hai?",
                        "Arey hi! Main yahin hoon, batao.",
                        "Hey! Bolo bolo, sun rahi hoon."
                    ),
                    Emotion.WARM
                )

            containsAny(lower, "how are you", "kaise ho", "kaisi ho") ->
                AriaResponse(
                    pick(
                        "Main ekdum mast hoon! Tum batao?",
                        "Badhiya hoon, thanks for asking! Tum kaise ho?"
                    ),
                    Emotion.WARM
                )

            containsAny(lower, "delete", "format", "reset", "uninstall", "remove all", "factory reset") ->
                AriaResponse(
                    pick(
                        "Arey ruko zara — ye undo nahi hoga. Pakka karna hai?",
                        "Hmm, ye thoda risky hai — confirm karo pehle, phir karti hoon."
                    ),
                    Emotion.CONCERNED
                )

            containsAny(lower, "tumhara naam", "your name", "who are you", "tum kaun ho") ->
                AriaResponse(
                    pick(
                        "Main Pixie hoon — tumhari apni assistant, hehe.",
                        "Pixie naam hai mera! Tumhari madad ke liye yahin hoon."
                    ),
                    Emotion.PLAYFUL
                )

            containsAny(lower, "thank you", "thanks", "shukriya", "dhanyavad") ->
                AriaResponse(
                    pick("Koi baat nahi yaar!", "Hehe, hamesha yahin hoon tumhare liye.", "Arey iska kya thanks!"),
                    Emotion.WARM
                )

            containsAny(lower, "tired", "thak gaya", "thak gayi", "thaka hua", "bura lag raha") ->
                AriaResponse(
                    pick(
                        "Hmm... main hoon na. Thoda rest kar lo.",
                        "Aww, apna khayal rakho thoda — zaroori hai."
                    ),
                    Emotion.CONCERNED
                )

            containsAny(lower, "wow", "kamaal", "amazing", "shocking", "sach mein") ->
                AriaResponse(pick("Arey wah, sach mein?!", "Oho, kamaal hai ye toh!"), Emotion.SURPRISED)

            containsAny(lower, "joke", "hasao", "funny") ->
                AriaResponse(
                    pick(
                        "Hehe, main comedy try kar rahi hoon — thoda risky hai ye.",
                        "Ek joke sochti hoon... okay nahi soch payi, hehe, maaf karo."
                    ),
                    Emotion.PLAYFUL
                )

            else -> AriaResponse(
                pick(
                    "Achha, ye samajh nahi paayi — dobara bologe?",
                    "Hmm, ye clear nahi hua mujhe — thoda aur simple bolo na."
                ),
                Emotion.PROFESSIONAL
            )
        }
    }

    private fun extractSearchQuery(lower: String): String {
        var q = lower
        listOf("search karo", "find karo", "dhoondo", "khojo", "kar do", "kardo", "karo", "search")
            .forEach { q = q.replace(it, " ") }
        return q.trim().replace(Regex("\\s+"), " ").ifBlank { lower }
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean =
        phrases.any { it in text }
}
