package dev.kdrag0n.blurtest

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.opengl.GLES31
import android.util.Half
import timber.log.Timber
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

object GLUtils {
    fun checkErrors() {
        while (true) {
            val error = GLES31.glGetError()
            if (error == GLES31.GL_NO_ERROR)
                break

            throw IllegalStateException("GL error: $error")
        }
    }

    private fun checkCompileErrors(shader: Int) {
        val isCompiled = IntArray(1)
        GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, isCompiled, 0)
        if (isCompiled[0] == GLES31.GL_FALSE) {
            val errors = GLES31.glGetShaderInfoLog(shader)
            GLES31.glDeleteShader(shader)
            throw IllegalArgumentException("Error compiling shader: $errors")
        }
    }

    fun createProgram(vertexCode: String, fragCode: String): Int {
        val vertexShader = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER).also { shader ->
            checkErrors()
            GLES31.glShaderSource(shader, vertexCode)
            GLES31.glCompileShader(shader)
            checkCompileErrors(shader)
        }

        val fragShader = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER).also { shader ->
            checkErrors()
            GLES31.glShaderSource(shader, fragCode)
            GLES31.glCompileShader(shader)
            checkCompileErrors(shader)
        }

        return GLES31.glCreateProgram().also {
            checkErrors()
            GLES31.glAttachShader(it, vertexShader)
            GLES31.glAttachShader(it, fragShader)
            GLES31.glLinkProgram(it)
            checkErrors()
        }
    }

    fun createVertexBuffer(data: FloatArray): Int {
        val buf = IntArray(1)
        GLES31.glGenBuffers(1, buf, 0)
        val buffer = buf[0]

        val dataBuf = ByteBuffer.allocateDirect(data.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(data)
                position(0)
            }
        }

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, buffer)
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, data.size * 4, dataBuf, GLES31.GL_STATIC_DRAW)
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0)

        return buffer
    }

    fun createVertexArray(vertexBuffer: Int, position: Int, uv: Int): Int {
        val buf = IntArray(1)
        GLES31.glGenVertexArrays(1, buf, 0)
        val vertexArray = buf[0]
        GLES31.glBindVertexArray(vertexArray)
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vertexBuffer)

        GLES31.glEnableVertexAttribArray(position)
        GLES31.glVertexAttribPointer(position, 2, GLES31.GL_FLOAT, false, 4 * 4, 0)

        GLES31.glEnableVertexAttribArray(uv)
        GLES31.glVertexAttribPointer(uv, 2, GLES31.GL_FLOAT, false, 4 * 4, 2 * 4)

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0)
        GLES31.glBindVertexArray(0)
        return vertexArray
    }

    @SuppressLint("HalfFloat")
    fun bitmapToRgb16Buffer(bitmap: Bitmap, filter: (Float) -> Double = { it.toDouble() }): ShortBuffer {
        return ByteBuffer.allocateDirect(bitmap.width * bitmap.height * 3 * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                for (y in (bitmap.height - 1) downTo 0) {
                    for (x in 0 until bitmap.width) {
                        val color = bitmap.getColor(x, y)
                        val r = filter(color.red())
                        val g = filter(color.green())
                        val b = filter(color.blue())

                        put(Half(r).halfValue())
                        put(Half(g).halfValue())
                        put(Half(b).halfValue())
                    }
                }
                position(0)
            }
        }
    }
}