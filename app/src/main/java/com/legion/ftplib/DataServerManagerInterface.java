package com.legion.ftplib;

import com.koushikdutta.async.AsyncSocket;

public interface DataServerManagerInterface {
    public void handleDataAccept(final AsyncSocket socket);

    public void setupDataServer();
}