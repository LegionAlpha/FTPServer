package com.legion.launcher.application;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import com.legion.additional.PreferenceManagerUtil;
import com.legion.ftpserver.BuiltinFtpServer;
import com.legion.ftpserver.ErrorReporter;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Random;

public class App extends Application {
    private boolean firstRunAfterBoot = false;
    private BuiltinFtpServer builtinFtpServer = new BuiltinFtpServer(this);

    private static App mInstance = null;

    public BuiltinFtpServer getBuiltinFtpServer() {
        return builtinFtpServer;
    }

    public static App getInstance() {
        if (mInstance == null) {
            mInstance = new App();
        }
        return mInstance;
    }

    private static Context mContext;
    private static final String TAG = "App";

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

    private String detectIp() {
        String ipAddress = null;

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());

        Log.d(TAG, "109, detectIp, ipAddress: " + ipAddress);

        if (ipAddress.equals("0.0.0.0")) {
            ipAddress = getHotspotIPAddress();
            Log.d(TAG, "114, detectIp, ipAddress: " + ipAddress);

            ipAddress = getIpAddress();
            Log.d(TAG, "120, detectIp, ipAddress: " + ipAddress);
        }

        return ipAddress;
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

    private String getHotspotIPAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getDhcpInfo().gateway;
        Log.d(TAG, "114, getHotspotIPAddress, ipAddress: " + ipAddress);
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
            Log.d(TAG, "152, getHotspotIPAddress, ipAddress: " + ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        Log.d(TAG, "157, getHotspotIPAddress, ipByteArray: " + ipByteArray.toString());

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            Log.d(TAG, "163, getHotspotIPAddress, ipAddressString: " + ipAddressString);
        } catch (UnknownHostException ex) {
            ipAddressString = "";
            Log.d(TAG, "168, getHotspotIPAddress, ipAddressString: " + ipAddressString);
        }

        return ipAddressString;
    }

    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mContext = getApplicationContext();
        boolean allowAnonymous = PreferenceManagerUtil.getAllowAnonymous();
        boolean externalStoragePerformanceOPtimize = PreferenceManagerUtil.getExternalStoragePerformanceOptimize();
        ErrorReporter errorReporter = new ErrorReporter(mContext);
        int actualPort = chooseRandomPort();
        builtinFtpServer.setPort(actualPort);
        String actualIp = detectIp();
        builtinFtpServer.setIp(actualIp);
        builtinFtpServer.setAllowActiveMode(true);
        builtinFtpServer.setAllowAnonymous(allowAnonymous);
        builtinFtpServer.start();
        builtinFtpServer.setExternalStoragePerformanceOptimize(externalStoragePerformanceOPtimize);
        builtinFtpServer.setFileNameTolerant(true);
        builtinFtpServer.setErrorListener(errorReporter);
    }

    public static Context getAppContext() {
        return mContext;
    }
}
