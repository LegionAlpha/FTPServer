package com.legion.ftplib;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.callback.ListenCallback;

import java.io.File;
import java.math.BigInteger;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.List;

public class FtpServer {
    private FilePathInterpreter filePathInterpreter = new FilePathInterpreter();
    private UserManager userManager = null;
    private EventListener eventListener = null;
    private ErrorListener errorListener = null;
    private Context context;
    private static final String TAG = "FtpServer";
    private InetAddress host;
    private int port;
    private String ip;
    private boolean allowActiveMode = true;
    private boolean autoDetectIp = true;
    private boolean fileNameTolerant = false;
    private File rootDirectory = null;
    private WIFIConnectChangeReceiver wifiConnectChangeReceiver = new WIFIConnectChangeReceiver(this);


    public String getIp() {
        return ip;
    }

    public void setIp(String externalIp) {
        this.ip = externalIp;
        autoDetectIp = false;
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

    private String getByConnectivityManager() {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = conMgr.getActiveNetwork();
        LinkProperties linkProperties = conMgr.getLinkProperties(network);
        String ipAddressString = null;

        if (linkProperties != null) {
            List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();

            InetAddress inetAddress = linkAddresses.get(0).getAddress();

            ipAddressString = inetAddress.getHostAddress();
        }

        return ipAddressString;
    }

    private String getHotspotIPAddress() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
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

    private String detectIp() {
        String ipAddress = null;

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());

        Log.d(TAG, "109, detectIp, ipAddress: " + ipAddress);

        if (ipAddress.equals("0.0.0.0")) {
            ipAddress = getHotspotIPAddress();
            Log.d(TAG, "114, detectIp, ipAddress: " + ipAddress);

            ipAddress = getByConnectivityManager();
            Log.d(TAG, "117, detectIp, ipAddress: " + ipAddress);

            ipAddress = getIpAddress();
            Log.d(TAG, "120, detectIp, ipAddress: " + ipAddress);
        }

        return ipAddress;
    }

    public void noticeConnectChange(String ssidName, boolean connected, int connect_type) {
        if (autoDetectIp) {
            String newIp = detectIp();

            if (newIp.equals(ip)) {
            } else {
                ip = newIp;
                notifyEvent(EventListener.IP_CHANGE);
            }
        }
    }

    private void notifyEvent(final String eventCode) {
        if (eventListener != null) {
            Handler uiHandler = new Handler(Looper.getMainLooper());

            Runnable runnable = new Runnable() {
                public void run() {
                    eventListener.onEvent(eventCode, null);
                }
            };

            uiHandler.post(runnable);
        }
    }

    public void setAutoDetectIp(boolean autoDetectIp) {
        this.autoDetectIp = autoDetectIp;
        registerWlanChangeListener();
    }

    private void registerWlanChangeListener() {
        IntentFilter filterWifiChange = new IntentFilter();
        filterWifiChange.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(wifiConnectChangeReceiver, filterWifiChange);
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setRootDirectory(File root) {
        rootDirectory = root;
    }

    public Uri getVirtualPath(String path) {
        String fullPath = Constants.FilePath.ExternalRoot + path;
        return filePathInterpreter.getVirtualPath(fullPath);
    }

    public void setFileNameTolerant(boolean toleranttrue) {
        fileNameTolerant = toleranttrue;
    }

    public void setExternalStoragePerformanceOptimize(boolean isChecked) {
        filePathInterpreter.setExternalStoragePerformanceOptimize(isChecked);
    }

    public void unmountVirtualPath(String path) {
        String fullPath = Constants.FilePath.ExternalRoot + path;
        filePathInterpreter.unmountVirtualPath(fullPath);
    }

    public void mountVirtualPath(String path, Uri uri) {
        String fullPath = Constants.FilePath.ExternalRoot + path;
        filePathInterpreter.mountVirtualPath(fullPath, uri);
        int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
    }

    public void answerBrowseDocumentTreeReqeust(int requestCode, Uri uri) {
        String fullPath = Constants.FilePath.AndroidData;
        filePathInterpreter.mountVirtualPath(fullPath, uri);
        int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public FtpServer(String host, int port, Context context, boolean allowActiveMode) {
        this(host, port, context, allowActiveMode, null);
    }

    public FtpServer(String host, int port, Context context, boolean allowActiveMode, ErrorListener errorListener) {
        this(host, port, context, allowActiveMode, errorListener, null);
    }

    public FtpServer(String host, int port, Context context, boolean allowActiveMode, ErrorListener errorListener, String externalIp) {
        this.context = context;
        this.allowActiveMode = allowActiveMode;
        this.errorListener = errorListener;

        this.ip = externalIp;
        autoDetectIp = false;

        rootDirectory = context.getFilesDir();

        try {
            this.host = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        this.port = port;
        setup();
        filePathInterpreter.setContext(context);
        filePathInterpreter.loadVirtualPathMap();
        registerWlanChangeListener();
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    private void setup() {
        AsyncServer.getDefault().listen(host, port, new ListenCallback() {
            @Override
            public void onAccepted(final AsyncSocket socket) {
                ControlConnectHandler handler = new ControlConnectHandler(context, allowActiveMode, host, ip);
                handler.handleAccept(socket);
                handler.setRootDirectory(rootDirectory);
                handler.setEventListener(eventListener);
                handler.setErrorListener(errorListener);
                handler.setUserManager(userManager);
                handler.setFilePathInterpreter(filePathInterpreter);
                handler.setFileNameTolerant(fileNameTolerant);
            }

            @Override
            public void onListening(AsyncServerSocket socket) {
                System.out.println("[Server] Server started listening for connections");
            }

            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    if (ex instanceof BindException) {
                        if (errorListener != null) {
                            errorListener.onError(Constants.ErrorCode.ADDRESS_ALREADY_IN_USE);
                        } else {
                            Log.d(TAG, "onCompleted, no error listener set, throwing exception.");

                            throw new RuntimeException(ex);
                        }
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
                System.out.println("[Server] Successfully shutdown server");
            }
        });
    }
}