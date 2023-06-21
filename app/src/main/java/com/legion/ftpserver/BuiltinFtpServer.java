package com.legion.ftpserver;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.legion.ftplib.EventListener;
import com.legion.ftplib.FtpServer;
import com.legion.ftplib.UserManager;

import java.net.BindException;

public class BuiltinFtpServer {
    private boolean allowAnonymous = true;
    private static final String TAG = "BuiltinFtpServer";
    private ErrorListener errorListener = null;
    private EventListener eventListener = null;
    private FtpServerErrorListener ftpServerErrorListener = null;
    private int port = 1421;
    private String ip = null;
    private FtpServer ftpServer = null;
    private boolean allowActiveMode = true;

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
        ftpServer.setEventListener(eventListener);
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void onError(Integer errorCode) {
        if (errorListener != null) {
            errorListener.onError(errorCode);
        } else {
            Exception ex = new BindException();
            throw new RuntimeException(ex);
        }
    }

    public void setAllowActiveMode(boolean allowActiveMode) {
        this.allowActiveMode = allowActiveMode;
    }

    public void setAllowAnonymous(boolean allowAnonymous) {
        this.allowAnonymous = allowAnonymous;
        assessSetUserManager();
    }

    public int getActualPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        String result = ip;

        if (ftpServer != null) {
            result = ftpServer.getIp();
        }

        return result;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    private BuiltinFtpServer() {
    }

    public BuiltinFtpServer(Context context) {
        this.context = context;
    }

    private Context context;

    public void start() {
        ftpServerErrorListener = new FtpServerErrorListener(this);

        ftpServer = new FtpServer("0.0.0.0", port, context, allowActiveMode, ftpServerErrorListener, ip);
        Log.d(TAG, "start, rootDirectory: " + Environment.getExternalStorageDirectory());

        ftpServer.setRootDirectory(Environment.getExternalStorageDirectory());
        ftpServer.setAutoDetectIp(true);
        assessSetUserManager();
    }

    public Uri getVirtualPath(String path) {
        return ftpServer.getVirtualPath(path);
    }

    public void unmountVirtualPath(String path) {
        ftpServer.unmountVirtualPath(path);
    }

    public void mountVirtualPath(String path, Uri uri) {
        ftpServer.mountVirtualPath(path, uri);
    }

    public void setFileNameTolerant(boolean toleranttrue) {
        ftpServer.setFileNameTolerant(toleranttrue);
    }

    public void setExternalStoragePerformanceOptimize(boolean isChecked) {
        ftpServer.setExternalStoragePerformanceOptimize(isChecked);
    }

    public void answerBrowseDocumentTreeReqeust(int requestCode, Uri uri) {
        ftpServer.answerBrowseDocumentTreeReqeust(requestCode, uri);
    }

    private void assessSetUserManager() {
        UserManager userManager = null;

        if (!allowAnonymous) {
            userManager = new UserManager();
            userManager.addUser("stupidbeauty", "ftpserver");
        }

        if (ftpServer != null) {
            ftpServer.setUserManager(userManager);
        }
    }
}
