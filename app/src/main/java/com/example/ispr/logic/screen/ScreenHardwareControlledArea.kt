package com.example.ispr.logic.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.Random
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

    private var isFlatSource: Boolean = true
    private var frameRate: Float = 30f
    private val random = Random()

    // Cache for noise bitmap and buffer to avoid allocations in the render loop
    private var noiseBitmap: Bitmap? = null
    private var noiseBuffer: IntArray? = null

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
        // Reset bitmap cache when bounds change to ensure it matches the new size
        noiseBitmap = null
        noiseBuffer = null
    }

    /**
     * Updates the color/intensity of the probe area.
     * In WCG/HDR modes, this would ideally use higher bit-depth values.
     */
    fun updateColor(color: Int) {
        probeColor = color
    }

    /**
     * Updates the source mode (flat color vs dynamic animation).
     */
    fun setFlatSource(isFlat: Boolean) {
        isFlatSource = isFlat
    }

    /**
     * Updates the refresh rate for dynamic modes.
     */
    fun setFrameRate(fps: Float) {
        frameRate = fps.coerceIn(1f, 120f)
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
                    
                    if (isFlatSource) {
                        // Draw the probe rectangle with flat color
                        paint.color = probeColor
                        canvas.drawRect(bounds, paint)
                    } else {
                        // Draw the dynamic content
                        drawDynamicContent(canvas, bounds, paint)
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            
            // Control the refresh rate independently of the UI thread
            val sleepTime = (1000 / frameRate).toLong()
            if (sleepTime > 0) {
                Thread.sleep(sleepTime)
            }
        }
    }

    /**
     * Placeholder for dynamic animations. Currently, draws 2D spatial white noise.
     */
    private fun drawDynamicContent(canvas: Canvas, bounds: Rect, paint: Paint) {
        generateWhiteNoise(canvas, bounds, paint)
    }

    /**
     * Generates a frame of spatial RGB white noise (TV static style).
     */
    private fun generateWhiteNoise(canvas: Canvas, bounds: Rect, paint: Paint) {
        val w = bounds.width().coerceAtLeast(1)
        val h = bounds.height().coerceAtLeast(1)

        // Scaling down the noise resolution to create "grains" and improve performance
        val scale = 2 
        val nw = w / scale
        val nh = h / scale

        if (noiseBitmap == null || noiseBitmap?.width != nw || noiseBitmap?.height != nh) {
            noiseBitmap = Bitmap.createBitmap(nw, nh, Bitmap.Config.ARGB_8888)
            noiseBuffer = IntArray(nw * nh)
        }

        val buffer = noiseBuffer ?: return
        val bitmap = noiseBitmap ?: return

        // Fill buffer with random ARGB noise (temporal change)
        for (i in buffer.indices) {
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            // 0xFF for alpha (opaque)
            buffer[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        bitmap.setPixels(buffer, 0, nw, 0, 0, nw, nh)
        
        // Draw the noise bitmap stretched to fill the probe bounds (spatial 2D)
        canvas.drawBitmap(bitmap, null, bounds, paint)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stop()
    }
}
