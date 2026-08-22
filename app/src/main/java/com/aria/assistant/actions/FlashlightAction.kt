package com.aria.assistant.actions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.aria.assistant.core.Vibe.pick

object FlashlightAction {
    fun toggle(context: Context, turnOn: Boolean): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult.Failure("Arey, is phone mein flashlight nahi mili.")
            cameraManager.setTorchMode(cameraId, turnOn)
            ActionResult.Success(
                if (turnOn) pick("Done! Flashlight on kar di.", "Haan, roshni ho gayi!")
                else pick("Achha, flashlight off kar di.", "Done! Band kar di.")
            )
        } catch (e: Exception) {
            ActionResult.Failure("Hmm, flashlight control nahi kar payi.")
        }
    }
}
