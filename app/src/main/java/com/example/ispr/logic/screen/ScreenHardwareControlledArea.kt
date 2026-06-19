package com.example.ispr.logic.screen

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.concurrent.thread

/**
 * Manages low-level, high-frequency drawing to a dedicated hardware surface.
 * Bypasses the standard Android View hierarchy for precise pixel control.
 */
class ScreenHardwareControlledArea(private val surfaceView: SurfaceView) : SurfaceHolder.Callback {

    private var drawingThread: Thread? = null
    private var isRunning = false
    
    private var probeBounds: Rect? = null
    private var probeColor: Int = Color.WHITE
    
    init {
        surfaceView.holder.addCallback(this)
        // Ensure the surface is translucent to see the UI behind it if needed,
        // or opaque for maximum performance.
        surfaceView.setZOrderOnTop(true)
        surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    /**
     * Updates the physical location and dimensions of the probe area.
     */
    fun updateBounds(bounds: Rect) {
        probeBounds = bounds
    }

    /**
     * Updates the color/intensity of the probe area.
     * In WCG/HDR modes, this would ideally use higher bit-depth values.
     */
    fun updateColor(color: Int) {
        probeColor = color
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        drawingThread = thread(name = "ProbeDrawingThread") {
            renderLoop()
        }
    }

    fun stop() {
        isRunning = false
        drawingThread?.join()
        drawingThread = null
    }

    private fun renderLoop() {
        val holder = surfaceView.holder
        val paint = Paint()
        
        while (isRunning) {
            val bounds = probeBounds ?: continue
            val canvas: Canvas? = holder.lockHardwareCanvas()
            
            if (canvas != null) {
                try {
                    // Clear the surface (keep it transparent outside the probe)
                    canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                    
                    // Draw the probe rectangle
                    paint.color = probeColor
                    canvas.drawRect(bounds, paint)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            
            // Control the refresh rate independently of the UI thread
            Thread.sleep(8) // ~120fps or adjust for specific modulation needs
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stop()
    }
}
