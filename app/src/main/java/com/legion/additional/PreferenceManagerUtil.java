package com.legion.additional;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.legion.launcher.application.App;

public class PreferenceManagerUtil {
    private static final String TAG = "PreferenceManagerUtil";

    public static boolean hasPortNumber() {
        Context ct = App.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ct);
        return sp.contains(Constants.Common.PortNumber);
    }

    public static int getPortNumber() {
        Context ct = App.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ct);
        return sp.getInt(Constants.Common.PortNumber, 17354);
    }

    public static void setPortNumber(int BuildConfigVERSION_CODE) {
        Context ct = App.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ct);
        sp.edit().putInt(Constants.Common.PortNumber, BuildConfigVERSION_CODE).apply();
    }

    public static void setExternalStoragePerformanceOptimize(boolean isChecked) {
        Context ct = App.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ct);
        sp.edit().putBoolean(Constants.Common.ExternalStoragePerformanceOptimize, isChecked).commit();
    }

    public static boolean getExternalStoragePerformanceOptimize() {
        Context ct = App.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ct);
        return sp.getBoolean(Constants.Common.ExternalStoragePerformanceOptimize, false);
    }

    public static void setAllowAnonymous(Boolean hasInit) {
        Context ct = App.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ct);
        sp.edit().putBoolean(Constants.Common.AllowAnonymous, hasInit).commit();
    }

    public static boolean getAllowAnonymous() {
        Context ct = App.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ct);
        return sp.getBoolean(Constants.Common.AllowAnonymous, true);
    }
}
