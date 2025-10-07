package com.edgedemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.TextureView
import android.opengl.GLSurfaceView
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.edgedemo.camera.CameraController
import com.edgedemo.nativebridge.NativeBridge
import com.edgedemo.gl.GLRenderer

class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var glView: GLSurfaceView
    private lateinit var glRenderer: GLRenderer
    private lateinit var fpsText: TextView
    private lateinit var btnToggle: Button
    private var useCanny: Boolean = true
    private var lastFpsTs = 0L
    private var frames = 0
    private lateinit var cameraController: CameraController

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById(R.id.texture_view)
        glView = findViewById(R.id.gl_view)
        fpsText = findViewById(R.id.fps_text)
        btnToggle = findViewById(R.id.btn_toggle)
        glRenderer = GLRenderer()
        glView.setEGLContextClientVersion(2)
        glView.setRenderer(glRenderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        cameraController = CameraController(this)
        cameraController.setFrameListener(object : CameraController.FrameListener {
            override fun onFrame(width: Int, height: Int, rgba: ByteArray) {
                // JNI process
                val processed = try {
                    NativeBridge.processEdgesRgba(rgba, width, height, useCanny)
                } catch (_: Throwable) { rgba }
                glRenderer.updateFrame(width, height, processed)
                updateFps()
            }
        })

        btnToggle.setOnClickListener {
            useCanny = !useCanny
            btnToggle.text = if (useCanny) "Edges" else "Gray"
            glRenderer.setGray(!useCanny)
        }
    }

    override fun onResume() {
        super.onResume()
        ensureCameraPermissionAndStart()
    }

    override fun onPause() {
        super.onPause()
        cameraController.stop()
    }

    private fun ensureCameraPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) startCamera() else requestPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        cameraController.start(textureView)
    }

    private fun updateFps() {
        frames++
        val now = System.currentTimeMillis()
        if (lastFpsTs == 0L) {
            lastFpsTs = now
            frames = 0
        } else if (now - lastFpsTs >= 1000L) {
            val fps = frames
            frames = 0
            lastFpsTs = now
            runOnUiThread { fpsText.text = "FPS: $fps" }
        }
    }
}


