package dev.kdrag0n.glcolortest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.EGL14
import android.opengl.EGL15
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class TestSurfaceView(
    context: Context,
    private val noiseBitmap: Bitmap,
) : GLSurfaceView(context), GLSurfaceView.Renderer, GLSurfaceView.EGLWindowSurfaceFactory {
    private var mainProgram = 0
    private var mainVertexArray = 0
    private lateinit var ditherFbo: GLFramebuffer

    private var mousePosX = 0.0f
    private var mousePosY = 0.0f
    private var isMouseDown = false

    // Rendering state
    private var startTime = System.currentTimeMillis()
    private var glWidth = 0
    private var glHeight = 0

    init {
        holder.setFormat(PixelFormat.RGBA_F16)
        setEGLContextClientVersion(3)
        setEGLWindowSurfaceFactory(this)
        setRenderer(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> isMouseDown = true
            MotionEvent.ACTION_UP -> isMouseDown = false
        }

        mousePosX = event.x
        mousePosY = event.y
        return true
    }

    private fun init() {
        val vertShader = resources.openRawResource(R.raw.shader_vert).readBytes().decodeToString()
        val fragShader = resources.openRawResource(R.raw.shader_frag).readBytes().decodeToString()
        val fullFragShader = FRAG_SHADER_PREFIX + fragShader + FRAG_SHADER_SUFFIX

        mainProgram = GLUtils.createProgram(vertShader, fullFragShader)
        mainVertexArray = GLUtils.createVertexArray()

        ditherFbo = GLFramebuffer(
            noiseBitmap.width, noiseBitmap.height,
            GLUtils.bitmapToRgb8Buffer(noiseBitmap),
            GLES31.GL_NEAREST, GLES31.GL_REPEAT,
            GLES31.GL_RGB8, GLES31.GL_RGB, GLES31.GL_UNSIGNED_BYTE
        )
    }

    private fun drawMesh(vertexArray: Int) {
        GLES31.glBindVertexArray(vertexArray)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLES, 0, 3)
        GLES31.glBindVertexArray(0)
    }

    override fun createWindowSurface(egl: EGL10, display: EGLDisplay, config: EGLConfig, nativeWindow: Any): EGLSurface =
        egl.eglCreateWindowSurface(display, config, nativeWindow, intArrayOf(
            //
            EGL15.EGL_GL_COLORSPACE, 0x3490, // Display-P3 passthrough
            EGL14.EGL_NONE,
        ))

    override fun destroySurface(egl: EGL10, display: EGLDisplay, surface: EGLSurface) {
        egl.eglDestroySurface(display, surface)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        init()
        GLES31.glClearColor(0.0f, 0f, 0f, 1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES31.glUseProgram(mainProgram)
        GLES31.glViewport(0, 0, glWidth, glHeight)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, ditherFbo.texture)

        GLES31.glUniform2f(0, glWidth.toFloat(), glHeight.toFloat())
        GLES31.glUniform1f(1, (System.currentTimeMillis() - startTime).toFloat() / 1000.0f)
        val clickState = if (isMouseDown) 1.0f else 0.0f
        GLES31.glUniform4f(2, mousePosX, mousePosY, clickState, clickState)
        GLES31.glUniform1i(3, 0)

        drawMesh(mainVertexArray)
        GLUtils.checkErrors()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glWidth = width
        glHeight = height
    }

    companion object {
        private const val FRAG_SHADER_PREFIX = """#version 310 es
        precision highp float;
        
        layout(location = 0) uniform vec2 iResolution;
        layout(location = 1) uniform float iTime;
        layout(location = 2) uniform vec4 iMouse;
        layout(location = 3) uniform sampler2D iChannel0;
        
        out vec4 fragColor;
        """

        private const val FRAG_SHADER_SUFFIX = """
        void main() {
            mainImage(fragColor, gl_FragCoord.xy);
        }
        """
    }
}
