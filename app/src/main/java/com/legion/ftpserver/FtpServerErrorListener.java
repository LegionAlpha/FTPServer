package com.legion.ftpserver;

import com.legion.ftplib.ErrorListener;

public class FtpServerErrorListener implements ErrorListener {
    private BuiltinFtpServer builtinFtpServer = null;

    @Override
    public void onError(Integer errorCode) {
        builtinFtpServer.onError(errorCode);
    }

    public FtpServerErrorListener(BuiltinFtpServer builtinFtpServer) {
        this.builtinFtpServer = builtinFtpServer;
    }
}

