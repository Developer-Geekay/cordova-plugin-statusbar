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
package com.geekay.plugin;

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

    // Standard Cordova Action Names
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
            // 1. Initial Edge-to-Edge Setup (From your proven logic)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            // 2. Read 'StatusBarOverlaysWebView' from config.xml (Default is true)
            boolean overlays = preferences.getBoolean("StatusBarOverlaysWebView", true);
            setStatusBarTransparent(overlays);

            // 3. Read 'StatusBarBackgroundColor' from config.xml
            String colorPref = preferences.getString("StatusBarBackgroundColor", "");
            if (!colorPref.isEmpty()) {
                setStatusBarBackgroundColor(colorPref);
            } else if (overlays) {
                // Start with transparent if overlaying and no color provided
                window.setStatusBarColor(Color.TRANSPARENT);
            }

            // 4. Read 'StatusBarStyle' from config.xml (Default is lightcontent)
            setStatusBarStyle(preferences.getString("StatusBarStyle", STYLE_LIGHT_CONTENT).toLowerCase());
        });
    }

    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) {
        LOG.v(TAG, "Executing action: " + action);

        switch (action) {
            case ACTION_READY:
                boolean isVisible = true;
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, isVisible));
                return true;

            case ACTION_SHOW:
                activity.runOnUiThread(() -> {
                    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
                    if (controller != null) {
                        controller.show(WindowInsetsCompat.Type.statusBars());
                        callbackContext.success("Status bar shown");
                    }
                });
                return true;

            case ACTION_HIDE:
                activity.runOnUiThread(() -> {
                    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
                    if (controller != null) {
                        // Hide logic from your proven code
                        controller.hide(WindowInsetsCompat.Type.statusBars());
                        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                        callbackContext.success("Status bar hidden");
                    }
                });
                return true;

            case ACTION_BACKGROUND_COLOR_BY_HEX_STRING:
                activity.runOnUiThread(() -> {
                    try {
                        String hexColor = args.getString(0);
                        setStatusBarBackgroundColor(hexColor);
                        callbackContext.success("Color updated successfully");
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                        callbackContext.error("Invalid color format");
                    }
                });
                return true;

            case ACTION_OVERLAYS_WEB_VIEW:
                activity.runOnUiThread(() -> {
                    try {
                        boolean overlays = args.getBoolean(0);
                        setStatusBarTransparent(overlays);
                        callbackContext.success("Overlay updated");
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid boolean argument");
                    }
                });
                return true;

            case ACTION_STYLE_DEFAULT:
                activity.runOnUiThread(() -> {
                    setStatusBarStyle(STYLE_DEFAULT);
                    callbackContext.success();
                });
                return true;

            case ACTION_STYLE_LIGHT_CONTENT:
                activity.runOnUiThread(() -> {
                    setStatusBarStyle(STYLE_LIGHT_CONTENT);
                    callbackContext.success();
                });
                return true;

            default:
                return false;
        }
    }

    /**
     * Exact color setting and auto-brightness logic from your working file.
     */
    private void setStatusBarBackgroundColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) return;

        try {
            if (!hexColor.startsWith("#")) {
                hexColor = "#" + hexColor;
            }

            int colorInt = Color.parseColor(hexColor);
            
            // Let Android natively apply the color over the edge-to-edge content
            window.setStatusBarColor(colorInt);

            // Update icon colors based on brightness
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                double brightness = (0.299 * Color.red(colorInt) + 0.587 * Color.green(colorInt) + 0.114 * Color.blue(colorInt));
                boolean isLight = brightness > 150;
                controller.setAppearanceLightStatusBars(isLight);
            }
        } catch (IllegalArgumentException e) {
            LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'. Error: " + e.getMessage());
        }
    }

    /**
     * Controls whether the WebView slides underneath the status bar (Edge-to-Edge)
     */
    private void setStatusBarTransparent(final boolean isTransparent) {
        // false = app content sits below status bar
        // true = app content slides under status bar (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, !isTransparent);

        if (isTransparent) {
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            // Default to black if not transparent (Cordova legacy behavior)
            window.setStatusBarColor(Color.BLACK);
        }
    }

    /**
     * Manual override for Status Bar icon colors
     */
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
            }
        }
    }
}