package com.mortendahl.velib.library;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

public abstract class BaseApplication extends Application {

    public static String getCurrentVersionName(Context context) {

        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();

        try {

            PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
            return pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new AssertionError(e);
        }

        //return null;

    }

    public static int getCurrentVersionCode(Context context) {

        try {

            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;

        }
        catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new AssertionError("Could not get package name, " + e.toString());
        }

    }

    public static String getDeviceId(Context appContext) {
        return Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}
