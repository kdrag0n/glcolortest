package dev.kdrag0n.glcolortest

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class TestSurfaceView(context: Context) : GLSurfaceView(context), GLSurfaceView.Renderer {
    private var mainProgram = 0
    private var mainVertexArray = 0

    private var mousePosX = 0.0f
    private var mousePosY = 0.0f
    private var isMouseDown = false

    // Rendering state
    private var startTime = System.currentTimeMillis()
    private var glWidth = 0
    private var glHeight = 0

    init {
        setEGLContextClientVersion(3)
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

    /*
     * Blur implementation
     * (as close to C++ as possible)
     */

    private fun init() {
        val vertShader = resources.openRawResource(R.raw.shader_vert).readBytes().decodeToString()
        val fragShader = resources.openRawResource(R.raw.shader_frag).readBytes().decodeToString()
        val fullFragShader = FRAG_SHADER_PREFIX + fragShader + FRAG_SHADER_SUFFIX

        mainProgram = GLUtils.createProgram(vertShader, fullFragShader)
        mainVertexArray = GLUtils.createVertexArray()
    }

    private fun drawMesh(vertexArray: Int) {
        GLES31.glBindVertexArray(vertexArray)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLES, 0, 3)
        GLES31.glBindVertexArray(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        init()
        GLES31.glClearColor(0.0f, 0f, 0f, 1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Render background
        GLES31.glUseProgram(mainProgram)
        GLES31.glViewport(0, 0, glWidth, glHeight)

        GLES31.glUniform2f(0, glWidth.toFloat(), glHeight.toFloat())
        GLES31.glUniform1f(1, (System.currentTimeMillis() - startTime).toFloat() / 1000.0f)
        val clickState = if (isMouseDown) 1.0f else 0.0f
        GLES31.glUniform4f(2, mousePosX, mousePosY, clickState, clickState)

        drawMesh(mainVertexArray)
        GLUtils.checkErrors()
        GLES31.glFinish()
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
        
        out vec4 fragColor;
        """

        private const val FRAG_SHADER_SUFFIX = """
        void main() {
            mainImage(fragColor, gl_FragCoord.xy);
        }
        """
    }
}