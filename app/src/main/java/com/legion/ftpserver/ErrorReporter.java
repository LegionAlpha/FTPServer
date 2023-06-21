package com.legion.ftpserver;

import android.content.Context;
import android.util.Log;

import com.legion.additional.PreferenceManagerUtil;
import com.legion.ftplib.Constants;
import com.legion.ftpserver.demo.R;
import com.legion.launcher.application.App;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

public class ErrorReporter implements ErrorListener {
    private boolean firstRunAfterBoot = false;
    private static App mInstance = null;

    public ErrorReporter(Context context) {
        this.mContext = context;
    }

    public void onError(Integer errorCode) {
        if (errorCode == Constants.ErrorCode.ControlConnectionEndedUnexpectedly) {
            String downloadFinished = mContext.getResources().getString(R.string.controlConnectionEndedUnexpectedlyged);
            Log.d(TAG, "notifyDownloadFinish, text: " + downloadFinished);
        }
    }

    public static App getInstance() {
        if (mInstance == null) {
            mInstance = new App();
        }
        return mInstance;
    }

    private static Context mContext;
    private static final String TAG = "ErrorReporter";

    private int chooseRandomPort() {
        Random random = new Random();

        int randomIndex = random.nextInt(65535 - 1025) + 1025;

        boolean builtinShortcutsVisible = PreferenceManagerUtil.hasPortNumber();

        if (builtinShortcutsVisible) {
            randomIndex = PreferenceManagerUtil.getPortNumber();
        } else {
            PreferenceManagerUtil.setPortNumber(randomIndex);
        }

        return randomIndex;
    }

    private String getIpAddress() {
        String ip = "";
        boolean found = false;
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip = inetAddress.getHostAddress();
                        Log.d(TAG, "164, getIpAddress, ipAddress: " + ip);

                        if (ip.startsWith("192.168.")) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    break;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }

    public static Context getAppContext() {
        return mContext;
    }
}