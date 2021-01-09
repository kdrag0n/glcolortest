package dev.kdrag0n.blurtest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.Toast
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.pow

// Do not enable for profiling
private const val DEBUG = false

private fun logDebug(msg: String) {
    if (DEBUG) {
        Timber.d(msg)
    }
}

/**
 * This is an implementation of dual-filtered Kawase blur, as described in here:
 * https://community.arm.com/cfs-file/__key/communityserver-blogs-components-weblogfiles/00-00-00-20-66/siggraph2015_2D00_mmg_2D00_marius_2D00_notes.pdf
 */
@SuppressLint("ViewConstructor")
class BlurSurfaceView(context: Context, private val bgBitmap: Bitmap, private val noiseBitmap: Bitmap) : GLSurfaceView(context) {
    private val renderer = BlurRenderer()
    @Volatile private var renderOffscreen = false
    @Volatile private var listenTouch = true
    private var totalTaps = 0

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (listenTouch) {
            when (event?.action) {
                // Middle 1/3 only for profiling double-tap
                MotionEvent.ACTION_UP -> {
                    if (event.y > height / 3 && event.y < height * 2/3) {
                        totalTaps++

                        if (totalTaps == 2) {
                            renderer.startProfiling()
                        } else {
                            //renderOffscreen = !renderOffscreen
                            Timber.i("Toggle render offscreen => $renderOffscreen")
                        }
                    }
                }
            }
        }

        return true
    }

    inner class BlurRenderer : Renderer {
        private var mPassthroughProgram = 0
        private var mPTextureLoc = 0
        private var mPVertexArray = 0

        private var mMixProgram = 0
        private var mMTexScaleLoc = 0
        private var mMCompositionTextureLoc = 0
        private var mMBlurredTextureLoc = 0
        private var mMDitherTextureLoc = 0
        private var mMBlurOpacityLoc = 0
        private var mMVertexArray = 0

        private var mDitherMixProgram = 0
        private var mDMTexScaleLoc = 0
        private var mDMCompositionTextureLoc = 0
        private var mDMBlurredTextureLoc = 0
        private var mDMDitherTextureLoc = 0
        private var mDMBlurOpacityLoc = 0
        private var mDMNoiseUVScaleLoc = 0
        private var mDMVertexArray = 0

        private var mDownsampleProgram = 0
        private var mDTexScaleLoc = 0
        private var mDTextureLoc = 0
        private var mDHalfPixelLoc = 0
        private var mDVertexArray = 0

        private var mUpsampleProgram = 0
        private var mUTexScaleLoc = 0
        private var mUTextureLoc = 0
        private var mUHalfPixelLoc = 0
        private var mUVertexArray = 0

        private lateinit var mFinalFbo: GLFramebuffer
        private lateinit var mBackgroundFbo: GLFramebuffer
        private lateinit var mDitherFbo: GLFramebuffer
        private lateinit var mCompositionFbo: GLFramebuffer
        private lateinit var mPassFbos: List<GLFramebuffer>
        private lateinit var mLastDrawTarget: GLFramebuffer

        // Blur state
        private var mRadius = 0
        private var mPasses = 0
        private var mOffset = 1.0f

        // Misc state
        private var mWidth = 0
        private var mHeight = 0

        // Profiling
        private var framesRenderedDisplay = 0
        @Volatile private var monitorFpsBg = true
        @Volatile private var totalRenderNanos = 0L
        @Volatile private var totalRenderFrames = 0

        /*
         * Blur implementation
         * (as close to C++ as possible)
         */

        private fun init() {
            mPassthroughProgram = GLUtils.createProgram(PASSTHROUGH_VERT_SHADER, PASSTHROUGH_FRAG_SHADER)
            mPTextureLoc = GLES31.glGetUniformLocation(mPassthroughProgram, "uTexture")
            mPVertexArray = GLUtils.createVertexArray()

            mMixProgram = GLUtils.createProgram(MIX_VERT_SHADER, MIX_FRAG_SHADER)
            mMTexScaleLoc = GLES31.glGetUniformLocation(mMixProgram, "uTexScale")
            mMCompositionTextureLoc = GLES31.glGetUniformLocation(mMixProgram, "uCompositionTexture")
            mMBlurredTextureLoc = GLES31.glGetUniformLocation(mMixProgram, "uBlurredTexture")
            mMDitherTextureLoc = GLES31.glGetUniformLocation(mMixProgram, "uDitherTexture")
            mMBlurOpacityLoc = GLES31.glGetUniformLocation(mMixProgram, "uBlurOpacity")
            mMVertexArray = GLUtils.createVertexArray()

            mDitherMixProgram = GLUtils.createProgram(DITHER_MIX_VERT_SHADER, DITHER_MIX_FRAG_SHADER)
            mDMTexScaleLoc = GLES31.glGetUniformLocation(mDitherMixProgram, "uTexScale")
            mDMCompositionTextureLoc = GLES31.glGetUniformLocation(mDitherMixProgram, "uCompositionTexture")
            mDMBlurredTextureLoc = GLES31.glGetUniformLocation(mDitherMixProgram, "uBlurredTexture")
            mDMDitherTextureLoc = GLES31.glGetUniformLocation(mDitherMixProgram, "uDitherTexture")
            mDMBlurOpacityLoc = GLES31.glGetUniformLocation(mDitherMixProgram, "uBlurOpacity")
            mDMNoiseUVScaleLoc = GLES31.glGetUniformLocation(mDitherMixProgram, "uNoiseUVScale")
            mDMVertexArray = GLUtils.createVertexArray()

            mDownsampleProgram = GLUtils.createProgram(DOWNSAMPLE_VERT_SHADER, DOWNSAMPLE_FRAG_SHADER)
            mDTexScaleLoc = GLES31.glGetUniformLocation(mDownsampleProgram, "uTexScale")
            mDTextureLoc = GLES31.glGetUniformLocation(mDownsampleProgram, "uTexture")
            mDHalfPixelLoc = GLES31.glGetUniformLocation(mDownsampleProgram, "uHalfPixel")
            mDVertexArray = GLUtils.createVertexArray()

            mUpsampleProgram = GLUtils.createProgram(UPSAMPLE_VERT_SHADER, UPSAMPLE_FRAG_SHADER)
            mUTexScaleLoc = GLES31.glGetUniformLocation(mUpsampleProgram, "uTexScale")
            mUTextureLoc = GLES31.glGetUniformLocation(mUpsampleProgram, "uTexture")
            mUHalfPixelLoc = GLES31.glGetUniformLocation(mUpsampleProgram, "uHalfPixel")
            mUVertexArray = GLUtils.createVertexArray()

            mDitherFbo = GLFramebuffer(
                noiseBitmap.width, noiseBitmap.height,
                GLUtils.bitmapToRgb8Buffer(noiseBitmap),
                GLES31.GL_NEAREST, GLES31.GL_REPEAT,
                GLES31.GL_RGB8, GLES31.GL_RGB, GLES31.GL_UNSIGNED_BYTE
            )

            val bgBuffer = ByteBuffer.allocateDirect(bgBitmap.rowBytes * bgBitmap.height).run {
                order(ByteOrder.nativeOrder())
            }
            bgBitmap.copyPixelsToBuffer(bgBuffer)
            bgBuffer.position(0)
            mBackgroundFbo = GLFramebuffer(
                bgBitmap.width, bgBitmap.height, bgBuffer,
                GLES31.GL_NEAREST, GLES31.GL_REPEAT,
                GLES31.GL_RGBA8, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE
            )

            GLES31.glUseProgram(mPassthroughProgram)
            GLES31.glUniform1i(mPTextureLoc, 0)

            GLES31.glUseProgram(mDownsampleProgram)
            GLES31.glUniform1i(mDTextureLoc, 0)

            GLES31.glUseProgram(mUpsampleProgram)
            GLES31.glUniform1i(mUTextureLoc, 0)

            GLES31.glUseProgram(mDitherMixProgram)
            GLES31.glUniform1i(mDMCompositionTextureLoc, 0)
            GLES31.glUniform1i(mDMBlurredTextureLoc, 1)
            GLES31.glUniform1i(mDMDitherTextureLoc, 2)

            GLES31.glUseProgram(mMixProgram)
            GLES31.glUniform1i(mMCompositionTextureLoc, 0)
            GLES31.glUniform1i(mMBlurredTextureLoc, 1)
            GLES31.glUniform1i(mMDitherTextureLoc, 2)

            GLES31.glUseProgram(0)

            startFpsMonitor()
        }

        private fun prepareBuffers(width: Int, height: Int) {
            mCompositionFbo = GLFramebuffer(width, height)

            val sourceFboWidth = (width * kFboScale).toInt()
            val sourceFboHeight = (height * kFboScale).toInt()
            val fbos = mutableListOf<GLFramebuffer>()
            for (i in 0 until (kMaxPasses + 1)) {
                fbos.add(
                    GLFramebuffer(sourceFboWidth shr i, sourceFboHeight shr i, null,
                        GLES31.GL_LINEAR, GLES31.GL_CLAMP_TO_EDGE,
                        // 2-10-10-10 reversed is the only 10-bpc format in GLES 3.1
                        GLES31.GL_RGB10_A2, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_INT_2_10_10_10_REV
                    )
                )
            }

            mPassFbos = fbos
            mWidth = width
            mHeight = height
        }

        private fun convertGaussianRadius(radius: Int): Pair<Int, Float> {
            for (i in 0 until kMaxPasses) {
                val offsetRange = kOffsetRanges[i]
                val offset = (radius * kFboScale / (2.0).pow(i + 1)).toFloat()
                if (offset in offsetRange) {
                    return (i + 1) to offset
                }
            }

            return 1 to (radius * kFboScale / (2.0).pow(1)).toFloat()
        }

        private fun drawMesh(vertexArray: Int) {
            GLES31.glBindVertexArray(vertexArray)
            GLES31.glDrawArrays(GLES31.GL_TRIANGLES, 0, 3)
            GLES31.glBindVertexArray(0)
        }

        // Execute blur passes, rendering to offscreen texture.
        private fun renderPass(read: GLFramebuffer, draw: GLFramebuffer, texScaleLoc: Int, halfPixelLoc: Int, vertexArray: Int) {
            logDebug("blur to ${draw.width}x${draw.height}")

            GLES31.glViewport(0, 0, draw.width, draw.height)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, read.texture)
            draw.bind()

            // 1/2 pixel offset in texture coordinate (UV) space
            // Note that this is different from NDC!
            GLES31.glUniform2f(halfPixelLoc, (0.5 / draw.width * mOffset).toFloat(), (0.5 / draw.height * mOffset).toFloat())
            GLES31.glUniform1f(texScaleLoc, 1.0f)
            drawMesh(vertexArray)
        }

        // Set up render targets, redirecting output to offscreen texture.
        private fun setAsDrawTarget(width: Int, height: Int, radius: Int) {
            if (width > mWidth || height > mHeight) {
                prepareBuffers(width, height)
            }

            if (radius != mRadius) {
                mRadius = radius
                val (passes, offset) = convertGaussianRadius(radius)
                mPasses = passes
                mOffset = offset
            }

            mCompositionFbo.bind()
            GLES31.glViewport(0, 0, width, height)
        }

        private fun prepare() {
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0)

            var read = mCompositionFbo
            var draw = mPassFbos[0]
            read.bindAsReadBuffer()
            draw.bindAsDrawBuffer()
            // This initial downscaling blit makes the first pass correct and improves performance.
            GLES31.glBlitFramebuffer(
                0, 0, read.width, read.height,
                0, 0, draw.width, draw.height,
                GLES31.GL_COLOR_BUFFER_BIT, GLES31.GL_LINEAR
            )

            logDebug("Prepare - initial dims ${draw.width}x${draw.height}")

            // Downsample
            GLES31.glUseProgram(mDownsampleProgram)
            for (i in 0 until mPasses) {
                read = mPassFbos[i]
                draw = mPassFbos[i + 1]
                renderPass(read, draw, mDTexScaleLoc, mDHalfPixelLoc, mDVertexArray)
            }

            // Upsample
            GLES31.glUseProgram(mUpsampleProgram)
            for (i in 0 until mPasses) {
                // Upsampling uses buffers in the reverse direction
                read = mPassFbos[mPasses - i]
                draw = mPassFbos[mPasses - i - 1]
                renderPass(read, draw, mUTexScaleLoc, mUHalfPixelLoc, mUVertexArray)
            }

            mLastDrawTarget = draw
        }

        // Render blur to the bound framebuffer (screen).
        private fun render(layers: Int, currentLayer: Int) {
            // Now let's scale our blur up. It will be interpolated with the larger composited
            // texture for the first frames, to hide downscaling artifacts.
            val opacity = min(1.0f, mRadius / kMaxCrossFadeRadius)

            // Crossfade using mix shader
            if (currentLayer == layers - 1) {
                GLES31.glUseProgram(mDitherMixProgram)
                GLES31.glUniform1f(mDMBlurOpacityLoc, opacity)
                GLES31.glUniform2f(mDMNoiseUVScaleLoc, (1.0 / 64.0 * mWidth).toFloat(), (1.0 / 64.0 * mHeight).toFloat())
                GLES31.glUniform1f(mDMTexScaleLoc, 1.0f)
            } else {
                GLES31.glUseProgram(mMixProgram)
                GLES31.glUniform1f(mMBlurOpacityLoc, opacity)
                GLES31.glUniform1f(mMTexScaleLoc, 1.0f)
            }
            logDebug("render - layers=$layers current=$currentLayer dither=${currentLayer == layers - 1}")

            GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mCompositionFbo.texture)

            GLES31.glActiveTexture(GLES31.GL_TEXTURE1)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mLastDrawTarget.texture)

            GLES31.glActiveTexture(GLES31.GL_TEXTURE2)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mDitherFbo.texture)

            drawMesh(mMVertexArray)

            // Clean up to avoid breaking further composition
            GLES31.glUseProgram(0)
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
            GLUtils.checkErrors()
        }

        private fun drawFrame(fbId: Int): Long {
            // Render background
            setAsDrawTarget(mWidth, mHeight, kRadius)
            GLES31.glUseProgram(mPassthroughProgram)
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mBackgroundFbo.texture)
            drawMesh(mPVertexArray)
            GLUtils.checkErrors()
            GLES31.glFinish()

            // Blur
            val beforeBlur = SystemClock.elapsedRealtimeNanos()
            for (i in 0 until kLayers) {
                prepare()

                if (i == kLayers - 1) {
                    // Dither out to display
                    GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fbId)
                    GLES31.glViewport(0, 0, mWidth, mHeight)
                } else {
                    // Next blur pass
                    setAsDrawTarget(mWidth, mHeight, kRadius)
                }
                render(kLayers, i)
            }
            GLES31.glFinish()
            return SystemClock.elapsedRealtimeNanos() - beforeBlur
        }

        /*
         * Surface renderer implementation
         */

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            init()
            GLES31.glClearColor(0.0f, 0f, 0f, 1f)
        }

        override fun onDrawFrame(gl: GL10?) {
            if (!renderOffscreen) {
                drawFrame(0)
                framesRenderedDisplay++
            } else {
                // Render off-screen after this for profiling
                // We never return after this point as we're in a tight FPS measurement loop.
                while (renderOffscreen) {
                    val delta = drawFrame(mFinalFbo.framebuffer)
                    totalRenderNanos += delta
                    totalRenderFrames++
                }
            }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            setAsDrawTarget(width, height, 120)
            mFinalFbo = GLFramebuffer(
                width, height, null,
                GLES31.GL_LINEAR, GLES31.GL_CLAMP_TO_EDGE,
                GLES31.GL_RGB8, GLES31.GL_RGB, GLES31.GL_UNSIGNED_BYTE
            )
        }

        /*
         * Profiling
         */

        private fun calcFrameTimeMs(): Double {
            // TODO: moving average
            return (totalRenderNanos.toDouble() / 1e6) / totalRenderFrames.toDouble()
        }

        private fun resetFrameProfiling() {
            totalRenderNanos = 0L
            totalRenderFrames = 0
        }

        private fun startFpsMonitor() {
            thread(name = "Blur FPS Monitor", isDaemon = true) {
                while (monitorFpsBg) {
                    Thread.sleep(1000)
                    if (renderOffscreen) {
                        Timber.i("Off-screen avg frame time: ${calcFrameTimeMs()} ms, $totalRenderFrames FPS")
                    }
                    resetFrameProfiling()
                }
            }
        }

        private fun autoProfile() {
            // Stop background monitor and wait
            monitorFpsBg = false
            renderOffscreen = true
            listenTouch = false

            Timber.i("autoProfile: Preparing to profile")
            systemBoost {
                Thread.sleep(15000)
                Timber.i("Starting auto-profile rendering")

                // Sample
                Thread.sleep(30000)
                val frameTimeMs = calcFrameTimeMs()
                val formattedMs = String.format("%.3f", frameTimeMs)

                Timber.i("================ PROFILING FINISHED ================")
                Timber.i("Average frame time: $formattedMs ms")
                Timber.i("================ PROFILING FINISHED ================")
                Toast.makeText(context, "Frame time: $formattedMs ms", Toast.LENGTH_SHORT).show()

                // Restart background monitor
                monitorFpsBg = true
                listenTouch = true
                systemUnboost {
                    Timber.i("Cleaned up system profiling state")
                    startFpsMonitor()
                }
            }
        }

        fun startProfiling() {
            thread(name = "Auto Profile", isDaemon = true) {
                autoProfile()
            }
        }
    }

    companion object {
        /* Testing constants not in C++ version */
        private const val kRadius = 120
        private const val kLayers = 3

        // Downsample FBO to improve performance
        private const val kFboScale = 0.2f
        // We allocate FBOs for this many passes to avoid the overhead of dynamic allocation.
        // If you change this, be sure to update kOffsetRanges as well.
        private const val kMaxPasses = 5
        // To avoid downscaling artifacts, we interpolate the blurred fbo with the full composited
        // image, up to this radius.
        private const val kMaxCrossFadeRadius = 40.0f

        // Minimum and maximum sampling offsets for each pass count, determined empirically.
        // Too low: bilinear downsampling artifacts
        // Too high: diagonal sampling artifacts
        private val kOffsetRanges = listOf(
            1.00f.. 2.50f, // pass 1
            1.25f.. 4.25f, // pass 2
            1.50f..11.25f, // pass 3
            1.75f..18.00f, // pass 4
            2.00f..20.00f  // pass 5
            /* limited by kMaxPasses */
        )

        private const val PASSTHROUGH_FRAG_SHADER = """#version 310 es
        precision mediump float;

        uniform sampler2D uTexture;

        in highp vec2 vUV;
        out vec4 fragColor;

        void main() {
            fragColor = texture(uTexture, vUV);
        }
        """

        private const val PASSTHROUGH_VERT_SHADER = """#version 310 es
        precision mediump float;

        uniform vec2 uNoiseUVScale;

        out highp vec2 vUV;

        void main() {
            vUV.x = (gl_VertexID == 2) ? 2.0 : 0.0;
            vUV.y = (gl_VertexID == 1) ? 2.0 : 0.0;
            gl_Position = vec4(vUV * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 1.0, 1.0);
        }
        """

        private const val DOWNSAMPLE_VERT_SHADER = """#version 310 es
        precision mediump float;

        uniform vec2 uHalfPixel;
        uniform float uTexScale;

        out highp vec2 vUV;
        out vec4 vDownTaps[2];

        void main() {
            vUV = vec2((gl_VertexID == 2) ? 2.0 : 0.0, (gl_VertexID == 1) ? 2.0 : 0.0) / 2.0 * uTexScale * 2.0;
            gl_Position = vec4(vUV * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 1.0, 1.0);

            vDownTaps[0] = vec4(vUV - uHalfPixel.xy, vUV + uHalfPixel.xy);
            vDownTaps[1] = vec4(vUV + vec2(uHalfPixel.x, -uHalfPixel.y), vUV - vec2(uHalfPixel.x, -uHalfPixel.y));
        }
        """

        private const val DOWNSAMPLE_FRAG_SHADER = """#version 310 es
        precision mediump float;

        uniform sampler2D uTexture;

        in highp vec2 vUV;
        in vec4 vDownTaps[2];
        out vec4 fragColor;

        void main() {
            vec3 sum = texture(uTexture, vUV).rgb * 4.0;
            for (int i = 0; i < 2; ++i) {
                sum += texture(uTexture, vDownTaps[i].xy).rgb;
                sum += texture(uTexture, vDownTaps[i].zw).rgb;
            }
            fragColor = vec4(sum * 0.125, 1.0);
        }
        """

        private const val UPSAMPLE_VERT_SHADER = """#version 310 es
        precision mediump float;

        uniform vec2 uHalfPixel;
        uniform float uTexScale;

        out highp vec2 vUV;
        out vec4 vUpTaps[4];

        void main() {
            vUV = vec2((gl_VertexID == 2) ? 2.0 : 0.0, (gl_VertexID == 1) ? 2.0 : 0.0) / 2.0 * uTexScale * 2.0;
            gl_Position = vec4(vUV * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 1.0, 1.0);

            vUpTaps[0] = vec4(vUV + vec2(-uHalfPixel.x * 2.0, 0.0), vUV + vec2(-uHalfPixel.x,  uHalfPixel.y));
            vUpTaps[1] = vec4(vUV + vec2(0.0,  uHalfPixel.y * 2.0), vUV + vec2( uHalfPixel.x,  uHalfPixel.y));
            vUpTaps[2] = vec4(vUV + vec2( uHalfPixel.x * 2.0, 0.0), vUV + vec2( uHalfPixel.x, -uHalfPixel.y));
            vUpTaps[3] = vec4(vUV + vec2(0.0, -uHalfPixel.y * 2.0), vUV + vec2(-uHalfPixel.x, -uHalfPixel.y));
        }
        """

        private const val UPSAMPLE_FRAG_SHADER = """#version 310 es
        precision mediump float;

        uniform sampler2D uTexture;

        in highp vec2 vUV;
        in vec4 vUpTaps[4];
        out vec4 fragColor;

        void main() {
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 4; ++i) {
                sum += texture(uTexture, vUpTaps[i].xy).rgb;
                sum += texture(uTexture, vUpTaps[i].zw).rgb * 2.0;
            }
            fragColor = vec4(sum * 0.08333333333333333, 1.0);
        }
        """

        private const val MIX_VERT_SHADER = """#version 310 es
        precision mediump float;

        uniform float uTexScale;

        out highp vec2 vUV;

        void main() {
            vUV = vec2((gl_VertexID == 2) ? 2.0 : 0.0, (gl_VertexID == 1) ? 2.0 : 0.0) / 2.0 * uTexScale * 2.0;
            gl_Position = vec4(vUV * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 1.0, 1.0);
        }
        """

        private const val MIX_FRAG_SHADER = """#version 310 es
        precision mediump float;

        uniform sampler2D uCompositionTexture;
        uniform sampler2D uBlurredTexture;
        uniform sampler2D uDitherTexture;
        uniform float uBlurOpacity;

        in highp vec2 vUV;
        out vec4 fragColor;

        void main() {
            vec3 blurred = texture(uBlurredTexture, vUV).rgb;
            vec3 composition = texture(uCompositionTexture, vUV).rgb;
            fragColor = vec4(mix(composition, blurred, 1.0), 1.0);
        }
        """

        private const val DITHER_MIX_VERT_SHADER = """#version 310 es
        precision mediump float;

        uniform vec2 uNoiseUVScale;
        uniform float uTexScale;

        out highp vec2 vUV;
        out vec2 vNoiseUV;

        void main() {
            vUV = vec2((gl_VertexID == 2) ? 2.0 : 0.0, (gl_VertexID == 1) ? 2.0 : 0.0) / 2.0 * uTexScale * 2.0;
            gl_Position = vec4(vUV * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 1.0, 1.0);

            vNoiseUV = vUV * uNoiseUVScale;
        }
        """

        private const val DITHER_MIX_FRAG_SHADER = """#version 310 es
        precision mediump float;

        uniform sampler2D uCompositionTexture;
        uniform sampler2D uBlurredTexture;
        uniform sampler2D uDitherTexture;
        uniform float uBlurOpacity;

        in highp vec2 vUV;
        in vec2 vNoiseUV;
        out vec4 fragColor;

        void main() {
            vec3 dither = (texture(uDitherTexture, vNoiseUV).rgb - 0.5) * 0.015625;
            vec3 blurred = texture(uBlurredTexture, vUV).rgb + dither;
            vec3 composition = texture(uCompositionTexture, vUV).rgb;
            fragColor = vec4(mix(composition, blurred, 1.0), 1.0);
        }
        """
    }
}