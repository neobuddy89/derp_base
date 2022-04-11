/*
 * Copyright (C) 2021 Arif JeNong
 * Copyright (C) 2021 The Android Open Source Project
 * Copyright (C) Dynamic System Bars Project
 * Copyright (C) Nusantara Project
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


// This sources implements DSB. Simple as that.

package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Handler;
import android.provider.Settings;
import android.view.Surface;
import android.view.WindowManager;

import com.android.systemui.derp.DynamicUtils;
import com.android.systemui.derp.ResourceUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class BarBackgroundUpdater {
    public static boolean abu;
    public static boolean accent;
    public static boolean linkedColor;
    public static boolean mHeaderEnabled;
    public static boolean mStatusGradientEnabled;
    public static boolean mNavigationGradientEnabled;
    public static boolean mStatusFilterEnabled;
    public static boolean mNavigationEnabled;
    public static boolean mNotifEnabled;
    public static boolean mStatusEnabled;
    public static boolean reverse;
    public static boolean PAUSED;
    public static int mHeaderIconOverrideColor;
    public static int mHeaderOverrideColor;
    public static int mNavigationBarIconOverrideColor;
    public static int mNavigationBarOverrideColor;
    public static int mNotifIconOverrideColor;
    public static int mNotifOverrideColor;
    public static int mNotipIconOverrideColor;
    public static int mNotipOverrideColor;
    public static int mPreviousHeaderIconOverrideColor;
    public static int mPreviousHeaderOverrideColor;
    public static int mPreviousNavigationBarIconOverrideColor;
    public static int mPreviousNavigationBarOverrideColor;
    public static int mPreviousNotifIconOverrideColor;
    public static int mPreviousNotifOverrideColor;
    public static int mPreviousNotipIconOverrideColor;
    public static int mPreviousNotipOverrideColor;
    public static int mPreviousStatusBarIconOverrideColor;
    public static int mPreviousStatusBarOverrideColor;
    public static int mPreviousTileIconOverrideColor;
    public static int mPreviousTileOverrideColor;
    public static int mStatusBarIconOverrideColor;
    public static int mStatusBarOverrideColor;
    public static int mTileIconOverrideColor;
    public static int mTileOverrideColor;
    public static int headerOverrideColor;
    public static int headerIconOverrideColor;
    public static int navigationBarOverrideColor;
    public static int navigationBarIconOverrideColor;
    public static int notifOverrideColor;
    public static int notifIconOverrideColor;
    public static int notipOverrideColor;
    public static int notipIconOverrideColor;
    public static int statusBarOverrideColor;
    public static int statusBarIconOverrideColor;
    public static int tileOverrideColor;
    public static int tileIconOverrideColor;
    public static int mTransparency;
    public static int parseColorLight;
    public static int parseColorDark;
    public static long sMinDelay = 50;
    public static int mDynamicColor;

    public static final ArrayList<UpdateListener> mListeners = new ArrayList<>();
    public static Handler mHandler;
    public static Context mContext;
    public static SettingsObserver mObserver;

    private static final BroadcastReceiver RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (BarBackgroundUpdater.class) {
                if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                    pause();
                } else if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                    resume();
                }
            }
        }
    };

    private static final Thread THREAD = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                float f2 = 0.3f;
                int f3 = -10;
                float f = 0.7f;

                if (PAUSED) {

                    // we have been told to do nothing; wait for notify to continue
                    synchronized (BarBackgroundUpdater.class) {
                        try {
                            BarBackgroundUpdater.class.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    continue;
                }

                boolean isAnyDsbEnabled = mStatusEnabled || mNavigationEnabled || mHeaderEnabled || mNotifEnabled;

                if (isAnyDsbEnabled) {
                    final Context context = mContext;
                    int isSleep = 1000;

                    if (context == null) {

                        // we haven't been initiated yet; retry in a bit
                        try {
                            Thread.sleep(isSleep);
                        } catch (InterruptedException e) {
                            return;
                        }

                        continue;
                    }

                    final WindowManager wm =
                            (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

                    final int rotation = wm.getDefaultDisplay().getRotation();
                    final boolean isLandscape = rotation == Surface.ROTATION_90 ||
                            rotation == Surface.ROTATION_270;

                    final Resources r = context.getResources();
                    final int statusBarHeight = r.getDimensionPixelSize(
                            ResourceUtils.getAndroidDimenResId("status_bar_height"));
                    final int navigationBarHeight = r.getDimensionPixelSize(
                            ResourceUtils.getAndroidDimenResId("navigation_bar_height" + (isLandscape ?
                                    "_landscape" : "")));

                    parseColorLight = Color.parseColor("#FFFFFFFF");
                    parseColorDark = accent ? r.getColor(ResourceUtils.getAndroidColorResId("accent_device_default_light")) : Color.parseColor(abu ? "#ff646464" : "#FF000000");

                    final int colors = DynamicUtils.getTargetColorStatusBar(DynamicUtils.TakeScreenshotSurface(), statusBarHeight);
                    final int colorsn = linkedColor ? colors : DynamicUtils.getTargetColorNavi(DynamicUtils.TakeScreenshotSurface(), navigationBarHeight);

                    if (navigationBarHeight <= 0 && mNavigationEnabled) {
                        // the navigation bar height is not positive - no dynamic navigation bar
                        Settings.System.putInt(context.getContentResolver(),
                                "DYNAMIC_NAVIGATION_BAR_STATE", 0);

                        // configuration has changed - abort and retry in a bit
                        try {
                            Thread.sleep(isSleep);
                        } catch (InterruptedException e) {
                            return;
                        }
                        continue;
                    }

                    boolean statuscolors = (colors != 0);

                    int dsbColor = mDynamicColor = statusBarOverrideColor = tileOverrideColor = headerOverrideColor = mStatusFilterEnabled ? filter(colors, (float) f3) : colors;
                    int iconColor = statusBarIconOverrideColor = tileIconOverrideColor = headerIconOverrideColor = (cekbriknes(dsbColor) <= f || !statuscolors) ? parseColorLight : parseColorDark;

                    // Dynamic status bar
                    boolean statusEnable = mStatusEnabled;

                    updateStatusBarColor(statusEnable ? dsbColor : 0);
                    updateStatusBarIconColor(statusEnable ? iconColor : 0);

                    // Dynamic Qs Header & Tile
                    boolean headerEnable = mHeaderEnabled;

                    int headerColor = headerEnable ? dsbColor : 0;
                    int headerIconColor = headerEnable ? iconColor : 0;

                    updateTileColor(headerColor);
                    updateTileIconColor(headerIconColor);
                    updateHeaderColor(headerColor);
                    updateHeaderIconColor(headerIconColor);

                    // Dynamic navigation bar
                    if (mNavigationEnabled) {
                        int colornav = navigationBarOverrideColor = colorsn;
                        int colorstat = statusBarOverrideColor;
                        updateNavigationBarColor(colornav);
                        float cekbriknesStatus = cekbriknes(colorstat);
                        float cekbriknesnavigationBar = cekbriknes(colornav);
                        boolean navigationcolors = (colorsn != 0);
                        int red = Color.red(colorstat);
                        int green = Color.green(colorstat);
                        int blue = Color.blue(colorstat);
                        int colorResult = Color.argb(0xFF, red, green, blue);
                        int parseColor = !reverse || !mStatusEnabled || cekbriknesStatus > f && cekbriknesnavigationBar > f || cekbriknesStatus < f2 && cekbriknesnavigationBar < f2 || cekbriknesStatus == cekbriknesnavigationBar ? (cekbriknesnavigationBar <= f || !navigationcolors) ? parseColorLight : parseColorDark : colorResult;
                        updateNavigationBarIconColor(parseColor);
                    } else {
                        // dynamic navigation bar is disabled
                        updateNavigationBarColor(0);
                        updateNavigationBarIconColor(0);
                    }
                } else {
                    // we are disabled completely - shush
                    updateStatusBarColor(0);
                    updateStatusBarIconColor(0);
                    updateNavigationBarColor(0);
                    updateNavigationBarIconColor(0);
                    updateHeaderColor(0);
                    updateHeaderIconColor(0);
                    updateTileColor(0);
                    updateTileIconColor(0);
                }

                // do a quick cleanup of the listener list
                synchronized (BarBackgroundUpdater.class) {
                    final ArrayList<UpdateListener> removables = new ArrayList<UpdateListener>();

                    for (final UpdateListener listener : mListeners) {
                        if (listener.shouldGc()) {
                            removables.add(listener);
                        }
                    }

                    for (final UpdateListener removable : removables) {
                        mListeners.remove(removable);
                    }
                }
                final long now = System.currentTimeMillis();
                final long delta = now - now;
                final long delay = Math.max(sMinDelay, delta * 2);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

    });

    private static final Thread THREAD2 = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                int f3 = -10;
                float f = 0.7f;

                if (PAUSED) {
                    // we have been told to do nothing; wait for notify to continue
                    synchronized (BarBackgroundUpdater.class) {
                        try {
                            BarBackgroundUpdater.class.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    continue;
                }

                if (mNotifEnabled) {
                    final Context context = mContext;

                    if (context == null) {
                        // we haven't been initiated yet; retry in a bit

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            return;
                        }
                        continue;
                    }

                    final int colors = mDynamicColor;

                    boolean dsbcolors = (colors != 0);
                    int dsbColor = mStatusFilterEnabled ? filter(colors, (float) f3) : colors;
                    int notifcolor = notifOverrideColor = notipOverrideColor = dsbColor;
                    int iconColor = notifIconOverrideColor = notipIconOverrideColor = (cekbriknes(notifcolor) <= f || !dsbcolors) ? parseColorLight : parseColorDark;

                    boolean notifEnable = mNotifEnabled;
                    int dsbNotifColor = notifEnable ? notifcolor : 0;
                    int dsbIconNotifColor = notifEnable ? iconColor : 0;

                    updateNotificationColor(dsbNotifColor);
                    updateNotipColor(dsbNotifColor);
                    updateNotificationIconColor(dsbIconNotifColor);
                    updateNotipIconColor(dsbIconNotifColor);

                } else {
                    // we are disabled completely - shush
                    updateNotificationColor(0);
                    updateNotificationIconColor(0);
                    updateNotipColor(0);
                    updateNotipIconColor(0);
                }

                // do a quick cleanup of the listener list
                synchronized (BarBackgroundUpdater.class) {
                    final ArrayList<UpdateListener> removables = new ArrayList<UpdateListener>();

                    for (final UpdateListener listener : mListeners) {
                        if (listener.shouldGc()) {
                            removables.add(listener);
                        }
                    }

                    for (final UpdateListener removable : removables) {
                        mListeners.remove(removable);
                    }
                }
                final long now = System.currentTimeMillis();
                final long delta = now - now;
                final long delay = Math.max(sMinDelay, delta * 2);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    });

    static {
        THREAD.setPriority(4);
        THREAD2.setPriority(4);
        THREAD.start();
        THREAD2.start();
    }

    public static float cekbriknes(int i) {
        return (0.299f * Color.red(i) +
                0.587f * Color.green(i) +
                0.114f * Color.blue(i)) / 255;
    }

    public static synchronized void setPauseState(boolean pause) {
        PAUSED = pause;
        if (!pause) {
            BarBackgroundUpdater.class.notifyAll();
        }
    }

    public static void pause() {
        setPauseState(true);
    }

    public static void resume() {
        setPauseState(false);
    }

    public synchronized static void init(Context context) {
        Context ctx = mContext;
        if (ctx != null) {
            ctx.unregisterReceiver(RECEIVER);
            if (mObserver != null) {
                context.getContentResolver().unregisterContentObserver(mObserver);
            }
        }
        mHandler = new Handler();
        mContext = context;
        ContentResolver resolver = mContext.getContentResolver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        mContext.registerReceiver(RECEIVER, intentFilter);
        if (mObserver == null) {
            mObserver = new SettingsObserver(new Handler());
        }
        resolver.registerContentObserver(Settings.System.getUriFor("DYNAMIC_STATUS_BAR_STATE"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("DYNAMIC_HEADER_STATE"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("DYNAMIC_SYSTEM_BARS_GRADIENT_STATE"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("DYNAMIC_NAVIGATION_BARS_GRADIENT_STATE"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("DYNAMIC_NOTIF_STATE"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("DYNAMIC_NAVIGATION_BAR_STATE"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("DYNAMIC_STATUS_BAR_FILTER_STATE"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("EXPERIMENTAL_DSB_FREQUENCY"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("UI_COLOR"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("ABU_ABU"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("ACCENT_COLOR"), false, mObserver);
        resolver.registerContentObserver(Settings.System.getUriFor("LINKED_COLOR"), false, mObserver);
        accent = Settings.System.getIntForUser(resolver, "ACCENT_COLOR", 0, -2) == 1;
        linkedColor = Settings.System.getIntForUser(resolver, "LINKED_COLOR", 0, -2) == 1;
        abu = Settings.System.getIntForUser(resolver, "ABU_ABU", 0, -2) == 1;
        reverse = Settings.System.getIntForUser(resolver, "UI_COLOR", 0, -2) == 1;
        mStatusEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_STATUS_BAR_STATE", 0, -2) == 1;
        mNotifEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_NOTIF_STATE", 0, -2) == 1;
        mHeaderEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_HEADER_STATE", 0, -2) == 1;
        mStatusGradientEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_SYSTEM_BARS_GRADIENT_STATE", 0, -2) == 1;
        mNavigationGradientEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_NAVIGATION_BARS_GRADIENT_STATE", 0, -2) == 1;
        mNavigationEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_NAVIGATION_BAR_STATE", 0, -2) == 1;
        mStatusFilterEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_STATUS_BAR_FILTER_STATE", 0, -2) == 1;
        mTransparency = Settings.System.getIntForUser(resolver, "EXPERIMENTAL_DSB_FREQUENCY", 255, -2);
        resume();
    }

    public synchronized static void addListener(UpdateListener... updateListenerArr) {
        for (UpdateListener updateListener : updateListenerArr) {
            if (updateListener != null) {
                updateListener.onUpdateStatusBarColor(mPreviousStatusBarOverrideColor, mStatusBarOverrideColor);
                updateListener.onUpdateHeaderColor(mPreviousHeaderOverrideColor, mHeaderOverrideColor);
                updateListener.onUpdateHeaderIconColor(mPreviousHeaderIconOverrideColor, mHeaderIconOverrideColor);
                updateListener.onUpdateTileColor(mPreviousTileOverrideColor, mTileOverrideColor);
                updateListener.onUpdateTileIconColor(mPreviousTileIconOverrideColor, mTileIconOverrideColor);
                updateListener.onUpdateNotificationColor(mPreviousNotifOverrideColor, mNotifOverrideColor);
                updateListener.onUpdateNotificationIconColor(mPreviousNotifIconOverrideColor, mNotifIconOverrideColor);
                updateListener.onUpdateNotipColor(mPreviousNotipOverrideColor, mNotipOverrideColor);
                updateListener.onUpdateNotipIconColor(mPreviousNotipIconOverrideColor, mNotipIconOverrideColor);
                updateListener.onUpdateStatusBarIconColor(mPreviousStatusBarIconOverrideColor, mStatusBarIconOverrideColor);
                updateListener.onUpdateNavigationBarColor(mPreviousNavigationBarOverrideColor, mNavigationBarOverrideColor);
                updateListener.onUpdateNavigationBarIconColor(mPreviousNavigationBarIconOverrideColor, mNavigationBarIconOverrideColor);
                boolean update = true;
                for (UpdateListener updateListener2 : mListeners) {
                    if (updateListener2 == updateListener) {
                        update = false;
                    }
                }
                if (update) {
                    mListeners.add(updateListener);
                }
            }
        }
    }

    public static int filter(final int original, final float diff) {
        final int red = (int) (Color.red(original) + diff);
        final int green = (int) (Color.green(original) + diff);
        final int blue = (int) (Color.blue(original) + diff);

        return Color.argb(
                Color.alpha(original),
                red > 0 ? (red < 255 ? red : 255) : 0,
                green > 0 ? (green < 255 ? green : 255) : 0,
                blue > 0 ? (blue < 255 ? blue : 255) : 0);
    }

    private static final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            accent = Settings.System.getIntForUser(resolver, "ACCENT_COLOR", 0, -2) == 1;
            linkedColor = Settings.System.getIntForUser(resolver, "LINKED_COLOR", 0, -2) == 1;
            abu = Settings.System.getIntForUser(resolver, "ABU_ABU", 0, -2) == 1;
            reverse = Settings.System.getIntForUser(resolver, "UI_COLOR", 0, -2) == 1;
            mStatusEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_STATUS_BAR_STATE", 0, -2) == 1;
            mNotifEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_NOTIF_STATE", 0, -2) == 1;
            mHeaderEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_HEADER_STATE", 0, -2) == 1;
            mStatusGradientEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_SYSTEM_BARS_GRADIENT_STATE", 0, -2) == 1;
            mNavigationGradientEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_NAVIGATION_BARS_GRADIENT_STATE", 0, -2) == 1;
            mNavigationEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_NAVIGATION_BAR_STATE", 0, -2) == 1;
            mStatusFilterEnabled = Settings.System.getIntForUser(resolver, "DYNAMIC_STATUS_BAR_FILTER_STATE", 0, -2) == 1;
            mTransparency = Settings.System.getIntForUser(resolver, "EXPERIMENTAL_DSB_FREQUENCY", 255, -2);
        }
    }

    public static class UpdateListener {
        private final WeakReference<Object> mRef;

        public void onUpdateHeaderColor(int previousColor, int newColor) {
        }

        public void onUpdateHeaderIconColor(int previousColor, int newColor) {
        }

        public void onUpdateNavigationBarColor(int previousColor, int newColor) {
        }

        public void onUpdateNavigationBarIconColor(int previousColor, int newColor) {
        }

        public void onUpdateNotificationColor(int previousColor, int newColor) {
        }

        public void onUpdateNotificationIconColor(int previousColor, int newColor) {
        }

        public void onUpdateNotipColor(int previousColor, int newColor) {
        }

        public void onUpdateNotipIconColor(int previousColor, int newColor) {
        }

        public void onUpdateStatusBarColor(int previousColor, int newColor) {
        }

        public void onUpdateStatusBarIconColor(int previousColor, int newColor) {
        }

        public void onUpdateTileColor(int previousColor, int newColor) {
        }

        public void onUpdateTileIconColor(int previousColor, int newColor) {
        }

        public UpdateListener(Object obj) {
            mRef = new WeakReference<Object>(obj);
        }

        public final boolean shouldGc() {
            return mRef.get() == null;
        }
    }

    public synchronized static void updateNotificationColor(int newColor) {
        if (mNotifOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mNotifEnabled) {
                mPreviousNotifOverrideColor = mNotifOverrideColor;
                mNotifOverrideColor = newColor;
                for (UpdateListener onUpdateNotificationColor : mListeners) {
                    onUpdateNotificationColor.onUpdateNotificationColor(mPreviousNotifOverrideColor, mNotifOverrideColor);
                }
            }
        }
    }

    public synchronized static void updateNotipColor(int newColor) {
        if (mNotipOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mNotifEnabled) {
                mPreviousNotipOverrideColor = mNotipOverrideColor;
                mNotipOverrideColor = newColor;
                for (UpdateListener onUpdateNotipColor : mListeners) {
                    onUpdateNotipColor.onUpdateNotipColor(mPreviousNotipOverrideColor, mNotipOverrideColor);
                }
            }
        }
    }

    public synchronized static void updateStatusBarColor(int newColor) {
        if (mStatusBarOverrideColor != newColor) {
            mPreviousStatusBarOverrideColor = mStatusBarOverrideColor;
            mStatusBarOverrideColor = newColor;
            for (UpdateListener onUpdateStatusBarColor : mListeners) {
                onUpdateStatusBarColor.onUpdateStatusBarColor(mPreviousStatusBarOverrideColor, mStatusBarOverrideColor);
            }
        }
    }

    public synchronized static void updateTileColor(int newColor) {
        if (mTileOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mHeaderEnabled) {
                mPreviousTileOverrideColor = mTileOverrideColor;
                mTileOverrideColor = newColor;
                for (UpdateListener onUpdateTileColor : mListeners) {
                    onUpdateTileColor.onUpdateTileColor(mPreviousTileOverrideColor, mTileOverrideColor);
                }
            }
        }
    }

    public synchronized static void updateHeaderColor(int newColor) {
        if (mHeaderOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mHeaderEnabled) {
                mPreviousHeaderOverrideColor = mHeaderOverrideColor;
                mHeaderOverrideColor = newColor;
                for (UpdateListener onUpdateHeaderColor : mListeners) {
                    onUpdateHeaderColor.onUpdateHeaderColor(mPreviousHeaderOverrideColor, mHeaderOverrideColor);
                }
            }
        }
    }

    public synchronized static void updateNavigationBarColor(int newColor) {
        if (mNavigationBarOverrideColor != newColor) {
            mPreviousNavigationBarOverrideColor = mNavigationBarOverrideColor;
            mNavigationBarOverrideColor = newColor;
            for (UpdateListener onUpdateNavigationBarColor : mListeners) {
                onUpdateNavigationBarColor.onUpdateNavigationBarColor(mPreviousNavigationBarOverrideColor, mNavigationBarOverrideColor);
            }
        }
    }

    public synchronized static void updateHeaderIconColor(int newColor) {
        if (mHeaderIconOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mHeaderEnabled) {
                mPreviousHeaderIconOverrideColor = mHeaderIconOverrideColor;
                mHeaderIconOverrideColor = newColor;
                for (UpdateListener onUpdateHeaderIconColor : mListeners) {
                    onUpdateHeaderIconColor.onUpdateHeaderIconColor(mPreviousHeaderIconOverrideColor, mHeaderIconOverrideColor);
                }
            }
        }
    }

    public synchronized static void updateNotificationIconColor(int newColor) {
        if (mNotifIconOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mNotifEnabled) {
                mPreviousNotifIconOverrideColor = mNotifIconOverrideColor;
                mNotifIconOverrideColor = newColor;
                for (UpdateListener onUpdateNotificationIconColor : mListeners) {
                    onUpdateNotificationIconColor.onUpdateNotificationIconColor(mPreviousNotifIconOverrideColor, mNotifIconOverrideColor);
                }
            }
        }
    }

    public synchronized static void updateNotipIconColor(int newColor) {
        if (mNotipIconOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mNotifEnabled) {
                mPreviousNotipIconOverrideColor = mNotipIconOverrideColor;
                mNotipIconOverrideColor = newColor;
                for (UpdateListener onUpdateNotipIconColor : mListeners) {
                    onUpdateNotipIconColor.onUpdateNotipIconColor(mPreviousNotipIconOverrideColor, mNotipIconOverrideColor);
                }
            }
        }
    }

    public synchronized static void updateStatusBarIconColor(int newColor) {
        if (mStatusBarIconOverrideColor != newColor) {
            mPreviousStatusBarIconOverrideColor = mStatusBarIconOverrideColor;
            mStatusBarIconOverrideColor = newColor;
            for (UpdateListener onUpdateStatusBarIconColor : mListeners) {
                onUpdateStatusBarIconColor.onUpdateStatusBarIconColor(mPreviousStatusBarIconOverrideColor, mStatusBarIconOverrideColor);
            }
        }
    }

    public synchronized static void updateTileIconColor(int newColor) {
        if (mTileIconOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mHeaderEnabled) {
                mPreviousTileIconOverrideColor = mTileIconOverrideColor;
                mTileIconOverrideColor = newColor;
                for (UpdateListener onUpdateTileIconColor : mListeners) {
                    onUpdateTileIconColor.onUpdateTileIconColor(mPreviousTileIconOverrideColor, mTileIconOverrideColor);
                }
            }
        }
    }

    public synchronized static void updateNavigationBarIconColor(int newColor) {
        if (mNavigationBarIconOverrideColor != newColor) {
            if (!StatusBar.mExpandedDsb || !mNavigationEnabled) {
                mPreviousNavigationBarIconOverrideColor = mNavigationBarIconOverrideColor;
                mNavigationBarIconOverrideColor = newColor;
                for (UpdateListener onUpdateNavigationBarIconColor : mListeners) {
                    onUpdateNavigationBarIconColor.onUpdateNavigationBarIconColor(mPreviousNavigationBarIconOverrideColor, mNavigationBarIconOverrideColor);
                }
            }
        }
    }
}
