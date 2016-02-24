package us.kulakov.cubewear;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.TimeUnit;

public class FrameRateComponent {
    private final Bitmap mBitmap;
    private final Canvas mCanvas;
    private final String mFpsStringFormat;
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mTextBounds = new Rect();
    private final int mBitmapHeight;
    private final int mBitmapWidth;
    private FloatBuffer mBufferPositions;
    private ShortBuffer mBufferIndices;
    private FloatBuffer mBufferUVCoords;
    private int[] mTextures = new int[1];

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mVPMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];
    private final float[] mModelMatrix = new float[16];

    private float mPositionX = 0f;
    private float mPositionY = 0f;

    private int mProgram;

    private long mLastFPSReadingTime = System.nanoTime();
    public int mFrameRateShown = 0;
    public int mFrameAccumulator = 0;


    short[] mVertexIndices = new short[] {
            0, 1,
            2, 0,
            2, 3
    };

    public static final String mVertexShader =
                    "uniform mat4 u_MVPMatrix;" +
                    "attribute vec4 a_Position;" +
                    "attribute vec2 a_TexCoord;" +
                    "varying vec2 v_TexCoord;" +
                    "void main() {" +
                    "  gl_Position = u_MVPMatrix * a_Position;" +
                    "  v_TexCoord = a_TexCoord;" +
                    "}";

    public static final String mFragmentShader =
            "precision mediump float;" +
                    "varying vec2 v_TexCoord;" +
                    "uniform sampler2D s_Texture;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D( s_Texture, v_TexCoord );" +
                    "}";


    public FrameRateComponent(@Nullable String fpsStringFormat) {
        // Set up bitmap
        mTextPaint.setTextSize(24);
        mTextPaint.setColor(0xFFFFFFFF);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mFpsStringFormat = (fpsStringFormat == null) ? "%d fps" : fpsStringFormat;
        String fpsText = createFpsText(999);
        mTextPaint.getTextBounds(fpsText, 0, fpsText.length(), mTextBounds);

        mBitmapWidth = mTextBounds.width();
        mBitmapHeight = mTextBounds.height();

        mBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        mCanvas.drawColor(0x00000000);

        // Set up view matrix
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Set up vertex data for our sprite
        float[] textureUVCoords = new float[] {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };

        float[] vertexPositions = new float[] {
                0, mBitmapHeight, 0.0f,
                0, 0, 0.0f,
                mBitmapWidth, 0, 0.0f,
                mBitmapWidth, mBitmapHeight, 0.0f,
        };

        // Allocate buffers
        mBufferUVCoords = ByteBuffer.allocateDirect(textureUVCoords.length * Constants.FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBufferUVCoords.put(textureUVCoords);
        mBufferUVCoords.position(0);

        mBufferPositions = ByteBuffer.allocateDirect(vertexPositions.length * Constants.FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBufferPositions.put(vertexPositions);
        mBufferPositions.position(0);

        mBufferIndices = ByteBuffer.allocateDirect(mVertexIndices.length * Constants.SHORT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asShortBuffer();
        mBufferIndices.put(mVertexIndices);
        mBufferIndices.position(0);

        GLES20.glGenTextures(1, mTextures, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, Utils.loadShader(GLES20.GL_VERTEX_SHADER, mVertexShader));
        GLES20.glAttachShader(mProgram, Utils.loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShader));
        GLES20.glLinkProgram(mProgram);

        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

    }

    private String createFpsText(int fps) {
        return String.format(mFpsStringFormat, fps);
    }

    public void setSurface(int width, int height, int positionX, int positionY) {

        mPositionX = positionX;
        mPositionY = positionY;

        Matrix.orthoM(mProjectionMatrix, 0, 0, width, 0, height, -1, 1);
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    public void setFrameRate(int fps) {
        mBitmap.eraseColor(0x00000000);

        String text = createFpsText(fps);
        mCanvas.drawText(text, mBitmapWidth / 2, mBitmapHeight - mTextPaint.descent(), mTextPaint);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mBitmap);
    }


    public void draw() {
        long currentTime = System.nanoTime();
        long timeSinceLastReading = (currentTime - mLastFPSReadingTime);
        mFrameAccumulator++;

        if(timeSinceLastReading > TimeUnit.SECONDS.toNanos(1)) {
            if(mFrameRateShown != mFrameAccumulator) {
                mFrameRateShown = mFrameAccumulator;
                setFrameRate(mFrameRateShown);
            }
            mFrameAccumulator = 1;
            mLastFPSReadingTime = currentTime;
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, mPositionX - mBitmapWidth/2, mPositionY - mBitmapHeight/2, 0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mVPMatrix, 0, mModelMatrix, 0);

        GLES20.glUseProgram(mProgram);

        int positionLoc = GLES20.glGetAttribLocation(mProgram, "a_Position");
        GLES20.glEnableVertexAttribArray(positionLoc);
        GLES20.glVertexAttribPointer(positionLoc, 3, GLES20.GL_FLOAT, false, 0, mBufferPositions);

        int texCoordLoc = GLES20.glGetAttribLocation(mProgram, "a_TexCoord" );
        GLES20.glEnableVertexAttribArray (texCoordLoc);
        GLES20.glVertexAttribPointer(texCoordLoc, 2, GLES20.GL_FLOAT, false, 0, mBufferUVCoords);

        int mvpMatrixLoc = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mMVPMatrix, 0);

        int texSampleLoc = GLES20.glGetUniformLocation (mProgram, "s_Texture" );
        GLES20.glUniform1i(texSampleLoc, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mVertexIndices.length,
                GLES20.GL_UNSIGNED_SHORT, mBufferIndices);

        GLES20.glDisableVertexAttribArray(positionLoc);
        GLES20.glDisableVertexAttribArray(texCoordLoc);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}
