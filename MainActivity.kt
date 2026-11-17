package com.bitaim.carrom

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {

    private val OVERLAY_REQ = 1001
    private val PROJECTION_REQ = 1002

    companion object {
        var projectionResultCode = 0
        var projectionData: Intent? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                    OVERLAY_REQ
                )
            } else {
                requestScreenCapture()
            }
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, HackService::class.java))
            Toast.makeText(this, "BiTaim Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestScreenCapture() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), PROJECTION_REQ)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            OVERLAY_REQ -> {
                if (Settings.canDrawOverlays(this)) requestScreenCapture()
                else Toast.makeText(this, "Overlay permission needed", Toast.LENGTH_SHORT).show()
            }
            PROJECTION_REQ -> {
                if (resultCode == RESULT_OK && data != null) {
                    projectionResultCode = resultCode
                    projectionData = data
                    startService(Intent(this, HackService::class.java))
                    Toast.makeText(this, "BiTaim Started!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
