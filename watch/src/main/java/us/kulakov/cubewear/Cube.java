/*
 * Copyright (C) 2014 The Android Open Source Project
 * Modifications copyright (C) 2016 Vasiliy Kulakov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package us.kulakov.cubewear;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Cube program
 * This source code is a modification of "ApiDemo", published under the Apache 2.0 license:
 * https://github.com/googleglass/gdk-apidemo-sample/blob/master/app/src/main/java/com/google/android/glass/sample/apidemo/opengl/Cube.java
 */
public class Cube {
    private static final String TAG = Cube.class.getSimpleName();


    private static final String VERTEX_SHADER = "shaders/cube.vert";
    private static final String FRAGMENT_SHADER = "shaders/cube.frag";

    private final FloatBuffer mCubePositions;
    private final FloatBuffer mCubeColors;
    private final FloatBuffer mCubeNormals;

    private final PlatformContext mPlatformContext;

    private int mProgramHandle;
    
    private int mPositionHandle;
    private int mNormalHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mLightPosHandle;

    public Cube(PlatformContext platformContext) {
        mPlatformContext = platformContext;


        // Initialize the buffers.
        mCubePositions = ByteBuffer.allocateDirect(CubeModel.VERTEX_POSITIONS.length * Constants.FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubePositions.put(CubeModel.VERTEX_POSITIONS);
        mCubePositions.position(0);

        mCubeColors = ByteBuffer.allocateDirect(CubeModel.VERTEX_COLORS.length * Constants.FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeColors.put(CubeModel.VERTEX_COLORS);
        mCubeColors.position(0);

        mCubeNormals = ByteBuffer.allocateDirect(CubeModel.VERTEX_NORMALS.length * Constants.FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeNormals.put(CubeModel.VERTEX_NORMALS);
        mCubeNormals.position(0);


        mProgramHandle = GLES20.glCreateProgram();

        if(mProgramHandle != 0) {
            GLES20.glAttachShader(mProgramHandle, Utils.loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER, mPlatformContext.getContext()));
            GLES20.glAttachShader(mProgramHandle, Utils.loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER, mPlatformContext.getContext()));

            GLES20.glLinkProgram(mProgramHandle);

            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program:");
                Log.e(TAG, GLES20.glGetProgramInfoLog(mProgramHandle));
                GLES20.glDeleteProgram(mProgramHandle);
                mProgramHandle = 0;
            }

            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
            mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
            mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");

            mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
            mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
            mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        }

        if(mProgramHandle == 0) {
            throw new RuntimeException("Failed to create or link program");
        }
    }

    public void setTimeLightOrigin(float[] timeLightOrigin) {

    }

    public void draw(float[] mvpMatrix, float[] mvMatrix) {
        GLES20.glUseProgram(mProgramHandle);

        mCubePositions.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, CubeModel.POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubePositions);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, CubeModel.COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);

        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, CubeModel.NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mCubeNormals);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, 0f, 0f, 0f);

        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }


}