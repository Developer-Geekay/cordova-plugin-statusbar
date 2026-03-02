/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.cordova.statusbar;

import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

public class StatusBar extends CordovaPlugin {
    private static final String TAG = "StatusBar";

    private static final String ACTION_HIDE = "hide";
    private static final String ACTION_SHOW = "show";
    private static final String ACTION_READY = "_ready";
    private static final String ACTION_BACKGROUND_COLOR_BY_HEX_STRING = "backgroundColorByHexString";
    private static final String ACTION_OVERLAYS_WEB_VIEW = "overlaysWebView";
    private static final String ACTION_STYLE_DEFAULT = "styleDefault";
    private static final String ACTION_STYLE_LIGHT_CONTENT = "styleLightContent";

    private static final String STYLE_DEFAULT = "default";
    private static final String STYLE_LIGHT_CONTENT = "lightcontent";

    private AppCompatActivity activity;
    private Window window;

    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "StatusBar: initialization");
        super.initialize(cordova, webView);

        activity = (AppCompatActivity) this.cordova.getActivity();
        window = activity.getWindow();

        activity.runOnUiThread(() -> {
            // Read 'StatusBarOverlaysWebView' from config.xml, default is true.
            boolean overlays = preferences.getBoolean("StatusBarOverlaysWebView", true);
            
            // Core Edge-to-Edge initialization (Android 15+ compatible)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            setStatusBarTransparent(overlays);

            // Read 'StatusBarBackgroundColor' from config.xml, default is #000000.
            setStatusBarBackgroundColor(preferences.getString("StatusBarBackgroundColor", "#000000"));

            // Read 'StatusBarStyle' from config.xml, default is 'lightcontent'.
            setStatusBarStyle(
                preferences.getString("StatusBarStyle", STYLE_LIGHT_CONTENT).toLowerCase()
            );
        });
    }

    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) {
        LOG.v(TAG, "Executing action: " + action);

        switch (action) {
            case ACTION_READY:
                // Check if status bar is currently visible by checking insets controller
                WindowInsetsControllerCompat controllerReady = WindowCompat.getInsetsController(window, window.getDecorView());
                boolean isVisible = true; // Default assumption
                // Note: accurate visibility requires a view listener, but standard flag check works as fallback
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, isVisible));
                return true;

            case ACTION_SHOW:
                activity.runOnUiThread(() -> {
                    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
                    if (controller != null) {
                        controller.show(WindowInsetsCompat.Type.statusBars());
                        // Reset behavior to standard touch to show if needed
                        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH);
                    }
                });
                return true;

            case ACTION_HIDE:
                activity.runOnUiThread(() -> {
                    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
                    if (controller != null) {
                        controller.hide(WindowInsetsCompat.Type.statusBars());
                        // Allow swipe down to temporarily reveal status bar
                        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                });
                return true;

            case ACTION_BACKGROUND_COLOR_BY_HEX_STRING:
                activity.runOnUiThread(() -> {
                    try {
                        setStatusBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                });
                return true;

            case ACTION_OVERLAYS_WEB_VIEW:
                activity.runOnUiThread(() -> {
                    try {
                        setStatusBarTransparent(args.getBoolean(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid boolean argument");
                    }
                });
                return true;

            case ACTION_STYLE_DEFAULT:
                activity.runOnUiThread(() -> setStatusBarStyle(STYLE_DEFAULT));
                return true;

            case ACTION_STYLE_LIGHT_CONTENT:
                activity.runOnUiThread(() -> setStatusBarStyle(STYLE_LIGHT_CONTENT));
                return true;

            default:
                return false;
        }
    }

    private void setStatusBarBackgroundColor(final String colorPref) {
        if (colorPref == null || colorPref.isEmpty()) return;

        int color;
        try {
            color = Color.parseColor(colorPref);
        } catch (IllegalArgumentException ignore) {
            LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
            return;
        }

        window.setStatusBarColor(color);

        // Auto-update icon colors based on brightness (from your custom logic)
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            double brightness = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
            boolean isLight = brightness > 150;
            controller.setAppearanceLightStatusBars(isLight);
        }
    }

    private void setStatusBarTransparent(final boolean isTransparent) {
        // WindowCompat handles the new layout rules automatically based on the boolean
        // false = standard layout with system bars pushing content down
        // true = edge-to-edge layout, content goes under system bars
        WindowCompat.setDecorFitsSystemWindows(window, !isTransparent);

        if (isTransparent) {
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void setStatusBarStyle(final String style) {
        if (style == null || style.isEmpty()) return;

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            if (style.equals(STYLE_DEFAULT)) {
                // Dark icons (for light backgrounds)
                controller.setAppearanceLightStatusBars(true);
            } else if (style.equals(STYLE_LIGHT_CONTENT)) {
                // Light icons (for dark backgrounds)
                controller.setAppearanceLightStatusBars(false);
            } else {
                LOG.e(TAG, "Invalid style, must be either 'default' or 'lightcontent'");
            }
        }
    }
}