package com.aria.assistant.accessibility

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo

object ScreenReader {

    fun isEnabled(): Boolean = AriaAccessibilityService.instance != null

    fun requestEnable(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Every labeled, clickable element on screen, in traversal order.
     * Text often lives on a child (e.g. a TextView) while the actual
     * clickable target is a parent row — this walks up to find the real
     * clickable ancestor rather than tapping the unclickable label itself.
     */
    fun findClickableElements(): List<ScreenElement> {
        val service = AriaAccessibilityService.instance ?: return emptyList()
        val root = service.rootInActiveWindow ?: return emptyList()

        val seen = LinkedHashMap<Int, ScreenElement>() // key: identity of the clickable ancestor
        collectLabeled(root) { label, node ->
            val clickable = nearestClickableAncestor(node) ?: return@collectLabeled
            val key = System.identityHashCode(clickable)
            val existing = seen[key]
            seen[key] = if (existing == null) {
                ScreenElement(label, clickable)
            } else if (label in existing.text) {
                existing
            } else {
                ScreenElement("${existing.text} $label".trim(), clickable)
            }
        }
        return seen.values.toList()
    }

    private fun collectLabeled(
        node: AccessibilityNodeInfo,
        onLabel: (String, AccessibilityNodeInfo) -> Unit
    ) {
        val label = (node.text ?: node.contentDescription)?.toString()?.trim()
        if (!label.isNullOrBlank()) onLabel(label, node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectLabeled(child, onLabel)
        }
    }

    private fun nearestClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    fun clickByIndex(index: Int): ScreenElement? {
        val target = findClickableElements().getOrNull(index) ?: return null
        return if (target.node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) target else null
    }

    fun clickContaining(query: String): ScreenElement? {
        val target = findClickableElements().firstOrNull { it.text.contains(query, ignoreCase = true) }
            ?: return null
        return if (target.node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) target else null
    }
}
