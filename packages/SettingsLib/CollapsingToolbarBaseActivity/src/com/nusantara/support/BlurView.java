/*
 * Copyright (C) 2022 Nusantara Project
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
package com.nusantara.support;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.view.WindowManager;
import android.view.Display;
import android.util.DisplayMetrics;
import android.os.Handler;


public class BlurView extends ImageView {

    private WallpaperManager mWallpaperManager;
    private Drawable mDrawable;
    private static Bitmap mBitmapWallpaper;
    private static Context mContext;
    private Handler mHandler = new Handler();

    public BlurView(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;
    }

    public static Bitmap renderScriptBlur(Bitmap bitmap, int radius) {
        RenderScript renderScript = RenderScript.create(mContext);
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        scriptIntrinsicBlur.setRadius(radius);
        Allocation input = Allocation.createFromBitmap(renderScript, bitmap);
        scriptIntrinsicBlur.setInput(input);
        Allocation output = Allocation.createTyped(renderScript, input.getType());
        scriptIntrinsicBlur.forEach(output);
        output.copyTo(bitmap);
        return bitmap;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Context ctx = mContext;
        ContentResolver contentResolver = ctx.getContentResolver();

        int scale = Settings.System.getInt(contentResolver, "blur_scale", 10);
        if (scale==0)
            scale=1;

        int radius = Settings.System.getInt(contentResolver, "blur_radius", 5);
        if (radius==0) 
            radius=1;

        int[] dis = getRealDimensionDisplay();

        mWallpaperManager = WallpaperManager.getInstance(getContext());
        mDrawable = mWallpaperManager.getFastDrawable();
        Bitmap bmp = mBitmapWallpaper = drawableToBitmap(mDrawable);
        Bitmap blur = renderScriptBlur(Bitmap.createScaledBitmap(bmp, dis[0] / scale, dis[1] / scale, false), radius);

        if (Settings.System.getInt(mContext.getContentResolver(), "blur_style", 0) == 1) {
        	mHandler.post(() -> {
        	    BitmapDrawable background = new BitmapDrawable(getResources(), blur);
/*           	 if (combinedBlur) {
           	    background.setColorFilter(getResources().getColor(
                                                 com.android.internal.R.attr.colorSurfaceHeader));
           	    background.setAlpha(30)
         	   }
*/         	   setImageBitmap(background.getBitmap());
           	 invalidate();
            });
        }
    }

    public static int[] getRealDimensionDisplay() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);

        return new int[] { metrics.widthPixels, metrics.heightPixels };
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

}