package com.android.systemui.derp;
/*
 * Copyright (C) serajr
 * Copyright (C) Xperia Open Source Project
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
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.graphics.Rect;
import android.os.IBinder;
import android.graphics.Point;

public class DisplayUtils {

    public static int getPxFromDp(Resources res, int size) {
        return (int) (size * res.getDisplayMetrics().density + 0.5f);
    }

    public static int getDominantColorByPixelsSampling(Bitmap bitmap, int rows, int cols) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int xPortion = width / cols;
        int yPortion = height / rows;
        int maxBin = -1;
        float[] hsv = new float[3];
        int[] colorBins = new int[36];
        float[] sumHue = new float[36];
        float[] sumSat = new float[36];
        float[] sumVal = new float[36];

        for (int row = 0; row <= rows; row++) {
            for (int col = 0; col <= cols; col++) {
                int pixel = bitmap.getPixel(
                        col > 0 ? (xPortion * col) - 1 : 0,
                        row > 0 ? (yPortion * row) - 1 : 0);

                Color.colorToHSV(pixel, hsv);

                int bin = (int) Math.floor(hsv[0] / 10.0f);

                sumHue[bin] = sumHue[bin] + hsv[0];
                sumSat[bin] = sumSat[bin] + hsv[1];
                sumVal[bin] = sumVal[bin] + hsv[2];

                colorBins[bin]++;

                if (maxBin < 0 || colorBins[bin] > colorBins[maxBin])
                    maxBin = bin;
            }
        }

        if (maxBin < 0)
            return Color.argb(255, 255, 255, 255);

        hsv[0] = sumHue[maxBin] / colorBins[maxBin];
        hsv[1] = sumSat[maxBin] / colorBins[maxBin];
        hsv[2] = sumVal[maxBin] / colorBins[maxBin];

        return Color.HSVToColor(hsv);

    }

    public static double getColorLightness(int color) {
        return 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
    }

    public static int[] getRealDimensionDisplay(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);

        return new int[] { metrics.widthPixels, metrics.heightPixels };
    }

    public static Bitmap TakeScreenshotSurface(Context context) {
        final IBinder displayToken = SurfaceControl.getInternalDisplayToken();
        Point point = new Point();
        Rect crop = new Rect(0, 0, point.x, point.y);

        final SurfaceControl.DisplayCaptureArgs captureArgs =
            new SurfaceControl.DisplayCaptureArgs.Builder(displayToken)
            .setSize(crop.width(), crop.height())
            .build();
        final SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer =
            SurfaceControl.captureDisplay(captureArgs);
        final Bitmap bitmap = screenshotBuffer == null ? null : screenshotBuffer.asBitmap();
        if (bitmap == null) {
            Log.e("Blurred", "Blurr error bitmap is null");
            return null;
        }
        bitmap.prepareToDraw();
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }
}
