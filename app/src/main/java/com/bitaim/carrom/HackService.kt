package com.bitaim.carrom

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import androidx.core.app.NotificationCompat

class HackService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: CarromOverlayView
    private lateinit var mediaProjection: MediaProjection
    private lateinit var imageReader: ImageReader
    private lateinit var virtualDisplay: VirtualDisplay

    private val handler = Handler(Looper.getMainLooper())
    private var screenWidth = 1080
    private var screenHeight = 2400

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        val dm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = dm.currentWindowMetrics.bounds
        screenWidth = metrics.width()
        screenHeight = metrics.height()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = CarromOverlayView(this, screenWidth, screenHeight)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(overlayView, params)

        setupScreenCapture()
        startAnalysisLoop()
    }

    private fun setupScreenCapture() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(
            MainActivity.projectionResultCode,
            MainActivity.projectionData!!
        )

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "CarromCapture",
            screenWidth, screenHeight,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
    }

    private fun startAnalysisLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                analyzeScreen()
                handler.postDelayed(this, 150)
            }
        }, 500)
    }

    private fun analyzeScreen() {
        val image = imageReader.acquireLatestImage() ?: return
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            val result = CarromAnalyzer.analyze(bitmap, screenWidth, screenHeight)
            handler.post { overlayView.updateAnalysis(result) }

            bitmap.recycle()
        } finally {
            image.close()
        }
    }

    private fun startForegroundNotification() {
        val channelId = "bitaim_channel"
        val channel = NotificationChannel(channelId, "BiTaim Carrom", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("BiTaim Carrom Hack")
            .setContentText("Aim assist running...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::virtualDisplay.isInitialized) virtualDisplay.release()
        if (::mediaProjection.isInitialized) mediaProjection.stop()
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
    }
}
