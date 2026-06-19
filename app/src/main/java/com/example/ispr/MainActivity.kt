package com.example.ispr

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ispr.ui.drawings.CameraPerimeterDrawing
import com.example.ispr.ui.drawings.ScaleReferenceDrawing
import com.example.ispr.ui.layouts.AdjustableSplitLayout
import com.example.ispr.ui.layouts.TabsLayout
import com.example.ispr.ui.theme.ISPRTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize brightness via the aggregated screen manager
        viewModel.screenManager.initializeBrightness(this)
        
        // Override window brightness to maximum for iSPR hardware performance
        viewModel.screenManager.setHighIntensityMode(true)

        enableEdgeToEdge()

        // Configure full-screen mode and drawing into the camera cutout area
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            ISPRTheme {
                // Use the aggregated screen manager for UI dimming factor
                val dimmingAlpha = remember { viewModel.screenManager.getUiDimmingAlpha() }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box {
                        AdjustableSplitLayout(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            topContent = {
                                CameraPerimeterDrawing(cutout = viewModel.screenManager.getScreenInfo().cameraRawCutout)
                                // 1. Low-level Hardware Surface (Bypasses UI Model)
                                AndroidView(
                                    factory = { context ->
                                        android.view.SurfaceView(context).apply {
                                            viewModel.screenManager.attachControlledArea(this)
                                            // Example: Position a 2cm x 2cm area 5cm below camera
                                            val screenInfo = viewModel.screenManager.getScreenInfo()
                                            screenInfo.calculateRectangleBounds(
                                                5f, 2f, 2f
                                            )?.let { bounds ->
                                                viewModel.screenManager.updateControlledAreaBounds(bounds)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                ScaleReferenceDrawing(
                                    xDpi = viewModel.screenManager.getScreenInfo().rawXDpi,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            },
                            bottomContent = {
                                Text(text = "opa")
                            }
                        )
                        // 3. Global Dimming Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = dimmingAlpha))
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Restore control to the system via the aggregator
        viewModel.screenManager.setHighIntensityMode(false)
    }
}
