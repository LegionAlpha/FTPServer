package com.legion.ftplib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class WIFIConnectChangeReceiver extends BroadcastReceiver {
    public static final String TAG = "WIFIConnectChangeReceiver";
    private FtpServer ftpServer = null;

    public WIFIConnectChangeReceiver(FtpServer ftpServer) {
        this.ftpServer = ftpServer;
    }

    public void onWifiConnectChange(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();

        String ssidName = info.getSSID();
        boolean connected = false;
        int connect_type = -1;

        if (wifiInfo != null && wifiInfo.isConnected()) {
            connect_type = LanImeBaseDef.DATA_CONNECT_STATE_WIFI;
            connected = true;
        }

        ftpServer.noticeConnectChange(ssidName, connected, connect_type);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        onWifiConnectChange(context);
    }
}
