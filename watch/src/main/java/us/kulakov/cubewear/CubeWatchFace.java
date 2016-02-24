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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Thanks to the following authors and materials:
 * OpenGL ES 2 for Android: A Quick-Start Guide - http://www.learnopengles.com/android-lesson-one-getting-started/
 * Alain Vongsouvanh: https://github.com/googleglass/gdk-apidemo-sample/tree/master/app/src/main/java/com/google/android/glass/sample/apidemo/opengl
 * WatchFace Sample from Google: http://developer.android.com/samples/WatchFace/index.html
 * http://developer.android.com/reference/android/support/wearable/watchface/Gles2WatchFaceService.Engine.html
 * http://developer.android.com/training/graphics/opengl/draw.html
 *
 */
public class CubeWatchFace extends Gles2WatchFaceService implements PlatformContext {
    private static final String TAG = CubeWatchFace.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends Gles2WatchFaceService.Engine {
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];
        private final float[] mModelMatrix = new float[16];
        private final float[] mMVPMatrix = new float[16];

        private Calendar mCalendar = Calendar.getInstance();
        private Cube mCube = null;
        private FrameRateComponent mFPS = null;
        private float mCubeRotationDegrees = 0f;

        /** Whether we've registered {@link #mTimeZoneReceiver}. */
        private boolean mRegisteredTimeZoneReceiver;

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(CubeWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .build());
        }

        @Override
        public void onGlContextCreated() {
            Log.d(TAG, "onGlContextCreated");
            super.onGlContextCreated();

            mCube = new Cube(CubeWatchFace.this);
            mFPS = new FrameRateComponent(null);
        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            Log.d(TAG, "onGlSurfaceCreated: " + width + " x " + height);
            super.onGlSurfaceCreated(width, height);

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

            GLES20.glViewport(0, 0, width, height);

            float aspectRatio = (float) width / height;
            // Create projection matrix based on viewport
            Matrix.frustumM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, 1.0f, 10.0f);

            mFPS.setSurface(width, height, width / 4, height / 4);
        }

        @Override
        public EGLConfig chooseEglConfig(EGLDisplay display) {
            int[] eglAttribList = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_STENCIL_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SAMPLE_BUFFERS, 1,
                    EGL14.EGL_SAMPLES, 2,  // This is for 4x MSAA.
                    EGL14.EGL_NONE, 0,
                    EGL14.EGL_NONE
            };

            EGLConfig[] configs = new EGLConfig[1];

            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(display, eglAttribList, 0, configs, 0, configs.length,
                    numConfigs, 0)) {
                Log.w(TAG, "unable to find EGLConfig");
                // TODO: Improve initialization, maybe add fallback EGL configs
                throw new RuntimeException("Unable to find desired ES2 EGL config");
            }

            return configs[0];
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we were detached.
                mCalendar.setTimeZone(TimeZone.getDefault());

                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            CubeWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            CubeWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            invalidate();
        }

        private float[] mMVMatrix = new float[16];

        @Override
        public void onDraw() {
            super.onDraw();
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            float seconds =
                    mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;

            boolean isAmbient = isInAmbientMode();

            //TODO: Draw actual time using the cube
            // Just a test, for now: in ambient mode, rotate cube with passing of minutes
            // In interactive, rotate with seconds
            if (isAmbient) {
                mCubeRotationDegrees = (minutes / 60f) * 360f;
            } else {
                mCubeRotationDegrees = (seconds / 60f) * 360f;
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Update cube model matrix
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, 2.0f);
            Matrix.rotateM(mModelMatrix, 0, mCubeRotationDegrees, 0.0f, 1.0f, 0.0f);

            Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);

            mCube.draw(mMVPMatrix, mMVMatrix);

            mFPS.draw();
            if (isVisible() && !isAmbient) {
                invalidate();
            }
        }
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }
}