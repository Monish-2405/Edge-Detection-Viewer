package com.edgedemo.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

class CameraController(
    private val context: Context
) {
    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var previewSize: Size? = null
    private val isOpening = AtomicBoolean(false)
    private var imageReader: ImageReader? = null

    interface FrameListener {
        fun onFrame(width: Int, height: Int, rgba: ByteArray)
    }
    private var frameListener: FrameListener? = null

    fun setFrameListener(listener: FrameListener?) {
        frameListener = listener
    }

    fun start(textureView: TextureView) {
        if (cameraDevice != null || isOpening.get()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ensureBackgroundThread()
        if (textureView.isAvailable) {
            openCamera(textureView)
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    openCamera(textureView)
                }
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { return true }
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
    }

    fun stop() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        quitBackgroundThread()
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(textureView: TextureView) {
        if (isOpening.getAndSet(true)) return
        val cameraId = selectBackCameraId() ?: run {
            isOpening.set(false)
            return
        }

        previewSize = choosePreviewSize(cameraId)
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                isOpening.set(false)
                cameraDevice = device
                startPreviewSession(textureView)
            }
            override fun onDisconnected(device: CameraDevice) {
                isOpening.set(false)
                device.close()
            }
            override fun onError(device: CameraDevice, error: Int) {
                isOpening.set(false)
                device.close()
            }
        }, backgroundHandler)
    }

    private fun startPreviewSession(textureView: TextureView) {
        val device = cameraDevice ?: return
        val size = previewSize
        val surfaceTexture = textureView.surfaceTexture ?: return
        if (size != null) {
            surfaceTexture.setDefaultBufferSize(size.width, size.height)
        }
        val previewSurface = Surface(surfaceTexture)
        if (size != null) {
            imageReader?.close()
            imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        val rgba = yuv420ToRgba(image)
                        frameListener?.onFrame(image.width, image.height, rgba)
                    } finally {
                        image.close()
                    }
                }, backgroundHandler)
            }
        }
        val readerSurface = imageReader?.surface
        try {
            val surfaces = if (readerSurface != null) listOf(previewSurface, readerSurface) else listOf(previewSurface)
            device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(previewSurface)
                        if (readerSurface != null) addTarget(readerSurface)
                        set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    }
                    session.setRepeatingRequest(request.build(), null, backgroundHandler)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) { }
            }, backgroundHandler)
        } catch (_: CameraAccessException) {}
    }

    private fun selectBackCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull()
        } catch (_: CameraAccessException) {
            null
        }
    }

    private fun choosePreviewSize(cameraId: String): Size? {
        return try {
            val chars = cameraManager.getCameraCharacteristics(cameraId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return null
            val choices = map.getOutputSizes(SurfaceTexture::class.java) ?: return null
            // Prefer 1280x720 if available, else smallest available for perf
            choices.firstOrNull { it.width == 1280 && it.height == 720 }
                ?: choices.sortedBy { it.width * it.height }.firstOrNull()
        } catch (_: Exception) { null }
    }

    private fun ensureBackgroundThread() {
        if (backgroundThread != null) return
        backgroundThread = HandlerThread("Camera2Background").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun quitBackgroundThread() {
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }

    private fun yuv420ToRgba(image: Image): ByteArray {
        val w = image.width
        val h = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val out = ByteArray(w * h * 4)
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yArray = ByteArray(yBuffer.remaining())
        yBuffer.get(yArray)
        val uArray = ByteArray(uBuffer.remaining())
        uBuffer.get(uArray)
        val vArray = ByteArray(vBuffer.remaining())
        vBuffer.get(vArray)

        var outIdx = 0
        for (row in 0 until h) {
            val yRow = row * yStride
            val uvRow = (row / 2) * uvRowStride
            for (col in 0 until w) {
                val y = (yArray[yRow + col].toInt() and 0xFF)
                val uvCol = (col / 2) * uvPixelStride
                val u = (uArray[uvRow + uvCol].toInt() and 0xFF) - 128
                val v = (vArray[uvRow + uvCol].toInt() and 0xFF) - 128

                // YUV to RGB conversion
                var r = (y + 1.402f * v).toInt()
                var g = (y - 0.344136f * u - 0.714136f * v).toInt()
                var b = (y + 1.772f * u).toInt()
                if (r < 0) r = 0 else if (r > 255) r = 255
                if (g < 0) g = 0 else if (g > 255) g = 255
                if (b < 0) b = 0 else if (b > 255) b = 255

                out[outIdx++] = r.toByte()
                out[outIdx++] = g.toByte()
                out[outIdx++] = b.toByte()
                out[outIdx++] = 0xFF.toByte()
            }
        }
        return out
    }
}


