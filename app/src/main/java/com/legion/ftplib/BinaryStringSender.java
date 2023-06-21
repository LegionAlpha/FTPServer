package com.legion.ftplib;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;

import java.nio.charset.StandardCharsets;

public class BinaryStringSender {
    private static final String TAG = "BinaryStringSender";
    private AsyncSocket socket;

    public void setSocket(AsyncSocket socketToSet) {
        socket = socketToSet;
    }

    public void sendStringInBinaryMode(String stringToSend) {
        byte[] contentToSend = (stringToSend + "\r\n").getBytes(StandardCharsets.UTF_8);

        Util.writeAll(socket, contentToSend, new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
 
