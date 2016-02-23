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
import java.util.concurrent.TimeUnit;

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
        private final float[] mVPMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];
        private final float[] mRotationMatrix = new float[16];
        private final float[] mMVPMatrix = new float[16];

        private Calendar mCalendar = Calendar.getInstance();
        private Cube mCube = null;
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

            // Set the fixed camera position (View matrix).
            Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, -4.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

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
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlContextCreated");
            }
            super.onGlContextCreated();

        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlSurfaceCreated: " + width + " x " + height);
            }
            super.onGlSurfaceCreated(width, height);

            GLES20.glClearDepthf(1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            GLES20.glEnable(GLES20.GL_BLEND);

            mCube = new Cube(CubeWatchFace.this);

            float aspectRatio = (float) width / height;
            // Create projection matrix based on viewport
            Matrix.frustumM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, 3.0f, 7.0f);
            // Create MVP matrix based on projection and view
            Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
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
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

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

            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Apply cube angle to rotation matrix, and create MVP matrix
            Matrix.setRotateM(mRotationMatrix, 0, mCubeRotationDegrees, 1.0f, 1.0f, 1.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, mVPMatrix, 0, mRotationMatrix, 0);
            mCube.draw(mMVPMatrix);

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