package com.aria.assistant.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * AppCatalog only covers ~13 well-known apps. This searches every
 * launchable app actually installed on the phone by its display name
 * (e.g. "Movie Box", "MX Player", any random app) — so opening isn't
 * limited to a small hardcoded list.
 */
object InstalledAppFinder {

    fun find(context: Context, spokenLower: String): AppInfo? {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)

        val candidates = try {
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (e: Exception) {
            return null
        }

        return candidates
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { (_, label) -> label.isNotBlank() }
            .firstOrNull { (_, label) -> spokenLower.contains(label.lowercase()) }
            ?.let { (pkg, label) -> AppInfo(label, pkg) }
    }
}
