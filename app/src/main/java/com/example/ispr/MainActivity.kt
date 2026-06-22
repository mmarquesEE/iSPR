package com.example.ispr

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ispr.logic.camera.CameraPermissionRequester
import com.example.ispr.ui.components.tabs.GraphTab
import com.example.ispr.ui.components.tabs.InfoTab
import com.example.ispr.ui.components.tabs.SourceConfigurationTab
import com.example.ispr.ui.drawings.CameraPerimeterDrawing
import com.example.ispr.ui.drawings.ScaleReferenceDrawing
import com.example.ispr.ui.layouts.AdjustableSplitLayout
import com.example.ispr.ui.layouts.TabItem
import com.example.ispr.ui.layouts.TabsLayout
import com.example.ispr.ui.theme.ISPRTheme
import com.example.ispr.ui.widgets.CameraPreview

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        viewModel.resume()
    }

    override fun onStop() {
        super.onStop()
        // Only pause hardware if we are NOT changing configurations (e.g. rotation)
        // This prevents camera/screen flickering during orientation changes.
        if (!isChangingConfigurations) {
            viewModel.pause()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Configure full-screen mode and drawing into the camera cutout area
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Initialize brightness via the aggregated screen manager
        viewModel.screenManager.initializeBrightness(this)

        setContent {
            ISPRTheme {
                // Handle camera permissions and startup.
                // The ViewModel also attempts to resume in its init block to survive configuration changes.
                CameraPermissionRequester { granted ->
                    if (granted) {
                        viewModel.resumeCameraIfPossible()
                    }
                }

                // Use the aggregated screen manager for UI dimming factor
                val dimmingAlpha by viewModel.screenManager.uiDimmingAlpha.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets()
                ) { innerPadding ->
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
                                            1.4f, 1.2f, 0.6f
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
                            val sourceConfig = viewModel.screenSourceConfiguration
                            val processingParams = viewModel.processingParameters
                            val result by viewModel.processingResult.collectAsState()

                            TabsLayout(
                                tabs = listOf(
                                    TabItem(Icons.Default.CameraAlt) {
                                        CameraPreview(
                                            cameraHardwareManager = viewModel.cameraManager,
                                            cameraHardwareInfo = viewModel.cameraManager.getCameraInfo()
                                        )
                                    },
                                    TabItem(Icons.Default.Tune) {
                                        SourceConfigurationTab(
                                            sourceConfig = sourceConfig,
                                            onSourceConfigChange = { viewModel.updateSourceConfiguration(it) }
                                        )
                                    },
                                    TabItem(Icons.Default.ShowChart) {
                                        GraphTab(
                                            result = result,
                                            params = processingParams,
                                            onParamsChange = { viewModel.updateProcessingParameters(it) },
                                            onReferenceToggle = { isChecked ->
                                                if (isChecked) viewModel.captureReference()
                                                else viewModel.clearReference()
                                            }
                                        )
                                    },
                                    TabItem(Icons.Default.Info) {
                                        InfoTab(
                                            cameraInfo = viewModel.cameraManager.getCameraInfo(),
                                            screenInfo = viewModel.screenManager.getScreenInfo()
                                        )
                                    }
                                )
                            )
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

    override fun onDestroy() {
        super.onDestroy()
        // Final cleanup of hardware state
        viewModel.pause()
    }
}
