package com.yafla.textureprojectiontest;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.yafla.textureprojectiontest.extras.GLHelpers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    final AssetManager assetManager;
    public GLRenderer(Context context) {
        assetManager = context.getAssets();
    }

    private int[] textureIds = new int[1];
    int shaderUniformTex0;
    int shaderUniformMVPMatrix;
    int shaderAttributeTexturePosition;
    int shaderAttributePosition;
    int shaderProgramId;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glGenTextures(1, textureIds, 0);
        GLHelpers.loadBitmap(textureIds[0], "images/checkerboard.png", assetManager);

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        try {
            String vertexShader = GLHelpers.toString(assetManager.open("shaders/vertex.txt"), "UTF-8", 1024);
            String fragmentShader = GLHelpers.toString(assetManager.open("shaders/fragment.txt"), "UTF-8", 1024);
            shaderProgramId = GLHelpers.createProgram(vertexShader, fragmentShader);
        } catch (IOException ioeex) {
            Log.e(this.getClass().getName(), ioeex.toString());
        }

        // retrieve the uniform and attribute locations
        shaderUniformTex0 = GLES20.glGetUniformLocation(shaderProgramId, "uTex0");
        shaderUniformMVPMatrix = GLES20.glGetUniformLocation(shaderProgramId, "uMVPMatrix");
        shaderAttributePosition = GLES20.glGetAttribLocation(shaderProgramId, "aPosition");
        shaderAttributeTexturePosition = GLES20.glGetAttribLocation(shaderProgramId, "aTexturePos");
    }

    float heightScale;
    private float[] mMVPMatrix = new float[16];
    float yCeiling, yCenterLow, yCenterHigh, yFloor;
    float xCeiling, xCenterLow, xCenterHigh, xFloor;

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        // the presumption is that width > height given that landscape is forced
        heightScale = (float) height / (float) width;

        float activeHeightScale = heightScale * 0.85f;
        activeQuad[ACTIVE_TOP_RIGHT] = 0.85f;
        activeQuad[ACTIVE_TOP_RIGHT+1] = activeHeightScale;
        activeQuad[ACTIVE_TOP_RIGHT+2] = getRandomMovement();
        activeQuad[ACTIVE_TOP_RIGHT+3] = getRandomMovement();
        activeQuad[ACTIVE_TOP_LEFT] = -0.85f;
        activeQuad[ACTIVE_TOP_LEFT+1] = activeHeightScale;
        activeQuad[ACTIVE_TOP_LEFT+2] = getRandomMovement();
        activeQuad[ACTIVE_TOP_LEFT+3] = getRandomMovement();
        activeQuad[ACTIVE_BOTTOM_LEFT] = -0.85f;
        activeQuad[ACTIVE_BOTTOM_LEFT+1] = -activeHeightScale;
        activeQuad[ACTIVE_BOTTOM_LEFT+2] = getRandomMovement();
        activeQuad[ACTIVE_BOTTOM_LEFT+3] = getRandomMovement();
        activeQuad[ACTIVE_BOTTOM_RIGHT] = 0.85f;
        activeQuad[ACTIVE_BOTTOM_RIGHT+1] = -activeHeightScale;
        activeQuad[ACTIVE_BOTTOM_RIGHT+2] = getRandomMovement();
        activeQuad[ACTIVE_BOTTOM_RIGHT+3] = getRandomMovement();
        startTime = System.currentTimeMillis() + 2000; // start moving after two seconds

        Matrix.orthoM(mMVPMatrix, 0, -1.0f, 1.0f, -heightScale, heightScale, -1.0f, 1.0f);

        // set the values used as boundaries for the four corners (e.g. top left stays between
        // yCenter and yCeiling, and xFloor and xCenter.

        yCeiling = heightScale-0.1f;
        yCenterLow = -0.1f;
        yCenterHigh = 0.1f;
        yFloor = -heightScale+0.1f;
        xCeiling = 0.9f;
        xCenterLow = -0.1f;
        xCenterHigh = 0.1f;
        xFloor = -0.9f;

        // prepare the fixed geometry elements
        activeIndices = ByteBuffer.allocateDirect(indexes.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        activeIndices.put(indexes);
        activeIndices.position(0);
        
        activeGeometry = ByteBuffer.allocateDirect(geometryQuad.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        activeTexture = ByteBuffer.allocateDirect(basicTexture.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    long startTime, lastFrameTime;

    @Override
    public void onDrawFrame(GL10 gl) {
        long currentFrameTime = System.currentTimeMillis();
        if (currentFrameTime > startTime) {
            float fraction = ( (currentFrameTime - lastFrameTime) / 1000.0f);
            doMovement(ACTIVE_TOP_RIGHT, fraction, xCenterHigh, xCeiling);
            doMovement(ACTIVE_TOP_RIGHT+1, fraction, yCenterHigh, yCeiling);
            doMovement(ACTIVE_TOP_LEFT, fraction, xFloor, xCenterLow);
            doMovement(ACTIVE_TOP_LEFT+1, fraction, yCenterHigh, yCeiling);
            doMovement(ACTIVE_BOTTOM_RIGHT, fraction, xCenterHigh, xCeiling);
            doMovement(ACTIVE_BOTTOM_RIGHT+1, fraction, yFloor, yCenterLow);
            doMovement(ACTIVE_BOTTOM_LEFT, fraction, xFloor, xCenterLow);
            doMovement(ACTIVE_BOTTOM_LEFT+1, fraction, yFloor, yCenterLow);

        }

        GLES20.glClearColor(0.2f, 0.4f, 0.4f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        prepareGeometry();

        GLES20.glUseProgram(shaderProgramId);
        GLES20.glUniformMatrix4fv(shaderUniformMVPMatrix, 1, false, mMVPMatrix, 0);

        GLES20.glVertexAttribPointer(shaderAttributePosition, 3, GLES20.GL_FLOAT, false, 12, activeGeometry);
        GLES20.glEnableVertexAttribArray(shaderAttributePosition);

        GLES20.glVertexAttribPointer(shaderAttributeTexturePosition, 3, GLES20.GL_FLOAT, false, 12, activeTexture);
        GLES20.glEnableVertexAttribArray(shaderAttributeTexturePosition);

        GLES20.glUniform1i(shaderUniformTex0, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, activeIndices);

        lastFrameTime = currentFrameTime;
    }

    // top right, top left, bottom left, bottom right
    static final short indexes[] = {
            0, 1, 2,
            0, 2, 3,
    };
    ShortBuffer activeIndices;

    final int TOP_RIGHT = 0, TOP_LEFT = 3, BOTTOM_LEFT = 6, BOTTOM_RIGHT = 9;
    final float[] geometryQuad = new float[12];

    final int ACTIVE_TOP_RIGHT = 0, ACTIVE_TOP_LEFT = 4, ACTIVE_BOTTOM_LEFT = 8, ACTIVE_BOTTOM_RIGHT = 12;
    // activeQuad is used to calculate the moving vertices, then populating into the geometryQuad
    final float[] activeQuad = new float[16];

    FloatBuffer activeGeometry;

    final float[] basicTexture = new float[]{
            1.0f, 0.0f, 1.0f,  // top right
            0.0f, 0.0f, 1.0f,  // top left
            0.0f, 1.0f, 1.0f,  // bottom left
            1.0f, 1.0f, 1.0f,  // bottom right
    };
    FloatBuffer activeTexture;

    void prepareGeometry() {
        // move the activeQuad according to the speeds. On hitting a boundary, reset speed to a
        // random inverse amount.

        geometryQuad[TOP_RIGHT] = activeQuad[ACTIVE_TOP_RIGHT];
        geometryQuad[TOP_RIGHT+1] = activeQuad[ACTIVE_TOP_RIGHT+1];
        geometryQuad[TOP_LEFT] = activeQuad[ACTIVE_TOP_LEFT];
        geometryQuad[TOP_LEFT+1] = activeQuad[ACTIVE_TOP_LEFT+1];
        geometryQuad[BOTTOM_LEFT] = activeQuad[ACTIVE_BOTTOM_LEFT];
        geometryQuad[BOTTOM_LEFT+1] = activeQuad[ACTIVE_BOTTOM_LEFT+1];
        geometryQuad[BOTTOM_RIGHT] = activeQuad[ACTIVE_BOTTOM_RIGHT];
        geometryQuad[BOTTOM_RIGHT+1] = activeQuad[ACTIVE_BOTTOM_RIGHT+1];

        activeGeometry.position(0);
        activeGeometry.put(geometryQuad);
        activeGeometry.position(0);

        activeTexture.position(0);

        float halfHeightScale = (heightScale / 2);

        // reset all of the values, using the heightscale to draw an appropriate portion of the
        // source graphic -- always draw 100% of the width, and the appropriate portion of the
        // height, middle out, to match the perspective ratio of the screen and avoid raw
        // distortion.

        basicTexture[TOP_LEFT] = 0.0f;
        basicTexture[TOP_LEFT+1] = 0.5f - halfHeightScale;
        basicTexture[TOP_LEFT+2] = 1.0f;

        basicTexture[TOP_RIGHT] = 1.0f;
        basicTexture[TOP_RIGHT+1] = 0.5f - halfHeightScale;
        basicTexture[TOP_RIGHT+2] = 1.0f;

        basicTexture[BOTTOM_RIGHT] = 1.0f;
        basicTexture[BOTTOM_RIGHT+1] = 0.5f + halfHeightScale;
        basicTexture[BOTTOM_RIGHT+2] = 1.0f;

        basicTexture[BOTTOM_LEFT] = 0.0f;
        basicTexture[BOTTOM_LEFT+1] = 0.5f + halfHeightScale;
        basicTexture[BOTTOM_LEFT+2] = 1.0f;

        if (perspectiveCorrection.get()) {
            // calculate the intersection between the two opposite corner lines of the quadrilateral
            float diffTopLeftBottomRightX, diffTopLeftBottomRightY, diffBottomLeftTopRightX, diffBottomLeftTopRightY;
            diffTopLeftBottomRightX = geometryQuad[BOTTOM_RIGHT] - geometryQuad[TOP_LEFT];
            diffTopLeftBottomRightY = geometryQuad[BOTTOM_RIGHT + 1] - geometryQuad[TOP_LEFT + 1];
            diffBottomLeftTopRightX = geometryQuad[TOP_RIGHT] - geometryQuad[BOTTOM_LEFT];
            diffBottomLeftTopRightY = geometryQuad[TOP_RIGHT + 1] - geometryQuad[BOTTOM_LEFT + 1];

            float s, t;
            s = (-diffTopLeftBottomRightY * (geometryQuad[TOP_LEFT] - geometryQuad[BOTTOM_LEFT]) + diffTopLeftBottomRightX * (geometryQuad[TOP_LEFT + 1] - geometryQuad[BOTTOM_LEFT + 1])) / (-diffBottomLeftTopRightX * diffTopLeftBottomRightY + diffTopLeftBottomRightX * diffBottomLeftTopRightY);
            t = (diffBottomLeftTopRightX * (geometryQuad[TOP_LEFT + 1] - geometryQuad[BOTTOM_LEFT + 1]) - diffBottomLeftTopRightY * (geometryQuad[TOP_LEFT] - geometryQuad[BOTTOM_LEFT])) / (-diffBottomLeftTopRightX * diffTopLeftBottomRightY + diffTopLeftBottomRightX * diffBottomLeftTopRightY);

            if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
                // calculate the intersection point of the two lines.
                float x = geometryQuad[TOP_LEFT] + (t * diffTopLeftBottomRightX);
                float y = geometryQuad[TOP_LEFT + 1] + (t * diffTopLeftBottomRightY);

                // Calculate the distance from the center point to each of the corners.
                float distanceX, distanceY;
                distanceX = x - geometryQuad[TOP_LEFT];
                distanceY = y - geometryQuad[TOP_LEFT + 1];
                float distanceTopLeft = (float) Math.sqrt((distanceX * distanceX) + (distanceY * distanceY));

                distanceX = x - geometryQuad[BOTTOM_LEFT];
                distanceY = y - geometryQuad[BOTTOM_LEFT + 1];
                float distanceBottomLeft = (float) Math.sqrt((distanceX * distanceX) + (distanceY * distanceY));

                distanceX = x - geometryQuad[TOP_RIGHT];
                distanceY = y - geometryQuad[TOP_RIGHT + 1];
                float distanceTopRight = (float) Math.sqrt((distanceX * distanceX) + (distanceY * distanceY));

                distanceX = x - geometryQuad[BOTTOM_RIGHT];
                distanceY = y - geometryQuad[BOTTOM_RIGHT + 1];
                float distanceBottomRight = (float) Math.sqrt((distanceX * distanceX) + (distanceY * distanceY));

                // get the relative distance scale of opposing corners.
                float scalingFactor;
                scalingFactor = (distanceTopLeft + distanceBottomRight) / distanceBottomRight;
                basicTexture[TOP_LEFT] = basicTexture[TOP_LEFT] * scalingFactor;
                basicTexture[TOP_LEFT + 1] = basicTexture[TOP_LEFT + 1] * scalingFactor;
                basicTexture[TOP_LEFT + 2] = scalingFactor;

                scalingFactor = (distanceTopLeft + distanceBottomRight) / distanceTopLeft;
                basicTexture[BOTTOM_RIGHT] = basicTexture[BOTTOM_RIGHT] * scalingFactor;
                basicTexture[BOTTOM_RIGHT + 1] = basicTexture[BOTTOM_RIGHT + 1] * scalingFactor;
                basicTexture[BOTTOM_RIGHT + 2] = scalingFactor;

                scalingFactor = (distanceBottomLeft + distanceTopRight) / distanceTopRight;
                basicTexture[BOTTOM_LEFT] = basicTexture[BOTTOM_LEFT] * scalingFactor;
                basicTexture[BOTTOM_LEFT + 1] = basicTexture[BOTTOM_LEFT + 1] * scalingFactor;
                basicTexture[BOTTOM_LEFT + 2] = scalingFactor;

                scalingFactor = (distanceBottomLeft + distanceTopRight) / distanceBottomLeft;
                basicTexture[TOP_RIGHT] = basicTexture[TOP_RIGHT] * scalingFactor;
                basicTexture[TOP_RIGHT + 1] = basicTexture[TOP_RIGHT + 1] * scalingFactor;
                basicTexture[TOP_RIGHT + 2] = scalingFactor;

            }
        }

        activeTexture.position(0);
        activeTexture.put(basicTexture);
        activeTexture.position(0);
    }

    AtomicBoolean perspectiveCorrection = new AtomicBoolean(true);
    public void setPerspectiveCorrection(boolean newSetting) {
        perspectiveCorrection.set(newSetting);
    }

    final Random randomGenerator = new Random(System.nanoTime());
    public float getRandomMovement() {
        return getRandomMovement(randomGenerator.nextBoolean());
    }

    public float getRandomMovement(boolean positive) {
        float value = (randomGenerator.nextFloat() * 0.15f) + 0.075f;
        return positive ? value : -value;
    }

    public void doMovement(int offset, float seconds, float floor, float ceiling) {
        float newValue = activeQuad[offset] + (activeQuad[offset+2] * seconds);
        if (newValue > ceiling) {
            newValue = ceiling;
            activeQuad[offset+2] = getRandomMovement(false);
        } else if (newValue < floor) {
            newValue = floor;
            activeQuad[offset+2] = getRandomMovement(true);
        }
        activeQuad[offset] = newValue;
    }
}
