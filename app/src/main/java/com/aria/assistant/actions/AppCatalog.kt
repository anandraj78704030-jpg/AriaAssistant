package com.aria.assistant.actions

data class AppInfo(val displayName: String, val packageName: String)

/**
 * Apps Aria can actually open via Android's launch intent, plus two
 * special cases (Camera, Settings) handled with their own system
 * intents in AppLauncher rather than a package name.
 */
object AppCatalog {
    val apps: Map<String, AppInfo> = mapOf(
        "youtube" to AppInfo("YouTube", "com.google.android.youtube"),
        "you tube" to AppInfo("YouTube", "com.google.android.youtube"),
        "whatsapp" to AppInfo("WhatsApp", "com.whatsapp"),
        "whats app" to AppInfo("WhatsApp", "com.whatsapp"),
        "instagram" to AppInfo("Instagram", "com.instagram.android"),
        "insta" to AppInfo("Instagram", "com.instagram.android"),
        "chrome" to AppInfo("Chrome", "com.android.chrome"),
        "gmail" to AppInfo("Gmail", "com.google.android.gm"),
        "camera" to AppInfo("Camera", ""),
        "gallery" to AppInfo("Gallery", "com.google.android.apps.photos"),
        "spotify" to AppInfo("Spotify", "com.spotify.music"),
        "maps" to AppInfo("Maps", "com.google.android.apps.maps"),
        "settings" to AppInfo("Settings", "")
    )

    fun find(lower: String): AppInfo? = apps.entries.firstOrNull { (key, _) -> key in lower }?.value
}
