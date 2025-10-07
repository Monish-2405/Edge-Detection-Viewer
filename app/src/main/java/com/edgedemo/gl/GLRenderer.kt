package com.edgedemo.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var textureId = 0
    private var frameWidth = 0
    private var frameHeight = 0
    private var pendingPixels: ByteArray? = null
    private var grayMode: Boolean = false

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texBuffer: FloatBuffer

    private val vertexShader = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main(){
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShader = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform int uGray;
        void main(){
            vec4 c = texture2D(uTexture, vTexCoord);
            if (uGray == 1) {
                float g = dot(c.rgb, vec3(0.299, 0.587, 0.114));
                gl_FragColor = vec4(g, g, g, c.a);
            } else {
                gl_FragColor = c;
            }
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vb = floatArrayOf(
            -1f, -1f, 1f, -1f, -1f, 1f,
            -1f, 1f, 1f, -1f, 1f, 1f
        )
        val tb = floatArrayOf(
            0f, 1f, 1f, 1f, 0f, 0f,
            0f, 0f, 1f, 1f, 1f, 0f
        )
        vertexBuffer = ByteBuffer.allocateDirect(vb.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vb)
        vertexBuffer.position(0)
        texBuffer = ByteBuffer.allocateDirect(tb.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(tb)
        texBuffer.position(0)

        program = createProgram(vertexShader, fragmentShader)
        textureId = createTexture()
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val pixels = synchronized(this) { pendingPixels } ?: return
        if (frameWidth > 0 && frameHeight > 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            val bb = ByteBuffer.wrap(pixels)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, frameWidth, frameHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb)
        }

        GLES20.glUseProgram(program)
        val aPos = GLES20.glGetAttribLocation(program, "aPosition")
        val aTc = GLES20.glGetAttribLocation(program, "aTexCoord")
        val uTex = GLES20.glGetUniformLocation(program, "uTexture")
        val uGray = GLES20.glGetUniformLocation(program, "uGray")
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aTc)
        GLES20.glVertexAttribPointer(aTc, 2, GLES20.GL_FLOAT, false, 0, texBuffer)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTex, 0)
        GLES20.glUniform1i(uGray, if (grayMode) 1 else 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
    }

    fun updateFrame(width: Int, height: Int, rgba: ByteArray) {
        synchronized(this) {
            frameWidth = width
            frameHeight = height
            pendingPixels = rgba
        }
    }

    fun setGray(enabled: Boolean) {
        grayMode = enabled
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createProgram(vs: String, fs: String): Int {
        val vsId = loadShader(GLES20.GL_VERTEX_SHADER, vs)
        val fsId = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vsId)
        GLES20.glAttachShader(prog, fsId)
        GLES20.glLinkProgram(prog)
        return prog
    }

    private fun createTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val id = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return id
    }
}


