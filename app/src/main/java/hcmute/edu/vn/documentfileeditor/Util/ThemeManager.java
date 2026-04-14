package hcmute.edu.vn.documentfileeditor.Util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";

    private ThemeManager() {
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(getSavedThemeMode(context));
    }

    public static void setDarkModeEnabled(Context context, boolean isDarkModeEnabled) {
        int nightMode = isDarkModeEnabled
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        getPreferences(context).edit().putInt(KEY_THEME_MODE, nightMode).apply();
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    public static boolean toggleDarkMode(Context context) {
        boolean newDarkModeState = !isDarkModeEnabled(context);
        setDarkModeEnabled(context, newDarkModeState);
        return newDarkModeState;
    }

    public static boolean isDarkModeEnabled(Context context) {
        return getSavedThemeMode(context) == AppCompatDelegate.MODE_NIGHT_YES;
    }

    private static int getSavedThemeMode(Context context) {
        return getPreferences(context).getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO);
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
