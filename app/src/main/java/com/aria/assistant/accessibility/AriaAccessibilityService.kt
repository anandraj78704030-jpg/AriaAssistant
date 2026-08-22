package com.aria.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Stage 5's screen-understanding service. Doesn't react to every screen
 * event — ScreenReader pulls the current screen tree on demand only
 * when a voice command actually needs it (keeps this lightweight and
 * avoids constantly processing background noise from every app).
 */
class AriaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty — see class doc.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    companion object {
        var instance: AriaAccessibilityService? = null
            private set
    }
}
