package com.mortendahl.velib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class PrefHelper {

    private PrefHelper() {}

    private static volatile Context cachedAppContext = null;

    public static void configure(Context appContext) {
        cachedAppContext = appContext;
    }

    private static SharedPreferences getPrefs(Context appContext) {
        return PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    public static String loadString(String key, String defaultValue) {
        return getPrefs(cachedAppContext).getString(key, defaultValue);
    }

    public static void saveString(String key, String value) {
        getPrefs(cachedAppContext)
                .edit()
                .putString(key, value)
                .commit();
    }

    public static Long loadLong(String key, Long defaultValue) {
        if ( ! getPrefs(cachedAppContext).contains(key)) {
            return defaultValue;
        }
        return getPrefs(cachedAppContext).getLong(key, defaultValue);
    }

    public static void saveLong(String key, long value) {
        getPrefs(cachedAppContext)
                .edit()
                .putLong(key, value)
                .commit();
    }

    public static Boolean loadBoolean(String key, Boolean defaultValue) {
        if ( ! getPrefs(cachedAppContext).contains(key)) {
            return defaultValue;
        }
        return getPrefs(cachedAppContext).getBoolean(key, false);  // default here never returned here!
    }

    public static void saveBoolean(String key, boolean value) {
        getPrefs(cachedAppContext)
                .edit()
                .putBoolean(key, value)
                .commit();
    }

    public static Integer loadInteger(String key, Integer defaultValue) {
        if ( ! getPrefs(cachedAppContext).contains(key)) {
            return defaultValue;
        }
        return getPrefs(cachedAppContext).getInt(key, -1);  // default here never returned here!
    }

    public static void saveInteger(String key, int value) {
        getPrefs(cachedAppContext)
                .edit()
                .putInt(key, value)
                .commit();
    }

    public static Double loadDouble(String key, Double defaultValue) {
        SharedPreferences prefs = getPrefs(cachedAppContext);
        if (!prefs.contains(key)) { return defaultValue; }
        Double value = null;
        try { value = Double.parseDouble(prefs.getString(key, null)); }
        catch (NumberFormatException e) {}
        return value;
    }

    public static void saveDouble(String key, double value) {
        getPrefs(cachedAppContext)
                .edit()
                .putString(key, Double.toString(value))
                .commit();
    }

    public static void clear(String key) {
        getPrefs(cachedAppContext)
                .edit()
                .remove(key)
                .commit();
    }

}
