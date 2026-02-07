package com.shejan.nextdo;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class ThemeManager {

    public static void applyTheme(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = sharedPreferences.getString("app_theme", "light");
        int nightMode;
        switch (theme) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "auto":
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }

        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }
}
