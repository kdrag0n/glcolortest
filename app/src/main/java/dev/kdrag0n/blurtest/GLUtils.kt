package dev.kdrag0n.blurtest

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.opengl.GLES31
import android.util.Half
import java.lang.IllegalStateException
import java.nio.Buffer
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

    fun createVertexBuffer(data: ByteArray): Int {
        val buf = IntArray(1)
        GLES31.glGenBuffers(1, buf, 0)
        val buffer = buf[0]

        val dataBuf = ByteBuffer.allocateDirect(data.size).run {
            order(ByteOrder.nativeOrder())
            put(data)
            position(0)
        }

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, buffer)
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, data.size, dataBuf, GLES31.GL_STATIC_DRAW)
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0)

        return buffer
    }

    fun createVertexArray(): Int {
        val buf = IntArray(1)
        GLES31.glGenVertexArrays(1, buf, 0)
        return buf[0]
    }

    @SuppressLint("HalfFloat")
    fun bitmapToRgb16fBuffer(bitmap: Bitmap, filter: (Float) -> Float = { it }): ShortBuffer {
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

    fun bitmapToRgb8Buffer(bitmap: Bitmap): Buffer {
        return ByteBuffer.allocateDirect(bitmap.width * bitmap.height * 3).run {
            order(ByteOrder.nativeOrder())
            for (y in (bitmap.height - 1) downTo 0) {
                for (x in 0 until bitmap.width) {
                    val color = bitmap.getColor(x, y)
                    put((color.red() * 255.0).toByte())
                    put((color.green() * 255.0).toByte())
                    put((color.blue() * 255.0).toByte())
                }
            }
            position(0)
        }
    }
}