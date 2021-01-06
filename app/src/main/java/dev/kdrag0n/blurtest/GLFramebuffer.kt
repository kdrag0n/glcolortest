package dev.kdrag0n.blurtest

import android.opengl.GLES31
import java.lang.IllegalStateException
import java.nio.Buffer

class GLFramebuffer(val width: Int, val height: Int, data: Buffer? = null, filter: Int = GLES31.GL_LINEAR, wrap: Int = GLES31.GL_CLAMP_TO_EDGE, internalformat: Int = GLES31.GL_RGB, format: Int = GLES31.GL_RGB, type: Int = GLES31.GL_UNSIGNED_BYTE) {
    val texture: Int
    private val fb: Int

    init {
        val buf = IntArray(2)
        GLES31.glGenTextures(1, buf, 0)
        GLES31.glGenFramebuffers(1, buf, 1)
        texture = buf[0]
        fb = buf[1]

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, internalformat, width, height, 0, format, type, data)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, filter)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, filter)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, wrap)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, wrap)
        GLUtils.checkErrors()

        bind()
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, texture, 0)
        val status = GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER)
        unbind()
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)

        if (status != GLES31.GL_FRAMEBUFFER_COMPLETE) {
            throw IllegalStateException("Framebuffer is not complete: error $status")
        }
    }

    fun bind() {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fb)
    }

    fun bindAsReadBuffer() {
        GLES31.glBindFramebuffer(GLES31.GL_READ_FRAMEBUFFER, fb)
    }

    fun bindAsDrawBuffer() {
        GLES31.glBindFramebuffer(GLES31.GL_DRAW_FRAMEBUFFER, fb)
    }

    fun unbind() {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
    }
}