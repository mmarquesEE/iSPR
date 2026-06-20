package com.example.ispr.ui.components.tabs

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ispr.logic.camera.CameraHardwareManager

@Composable
fun CameraPreviewTab(
    cameraManager: CameraHardwareManager
) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        cameraManager.setPreviewSurface(holder.surface)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        cameraManager.setPreviewSurface(null)
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
