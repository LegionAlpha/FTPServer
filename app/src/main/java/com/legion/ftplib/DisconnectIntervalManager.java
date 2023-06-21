package com.legion.ftplib;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DisconnectIntervalManager {
    private FilePathInterpreter filePathInterpreter = null;
    private static final String TAG = "DisconnectIntervalManager";
    private long restSTart = 0;
    private long scheduledDisconnectTimestamp = 0;
    private byte[] dataSocketPendingByteArray = null;
    private ControlConnectHandler controlConnectHandler = null;
    private AsyncSocket data_socket = null;
    private File rootDirectory = null;
    private DocumentFile fileToSend = null;
    private Context context = null;
    private String wholeDirecotoryPath = "";
    private long newCommandAmount = 0;
    private long newCommandTimeDelayTotal = 0;

    public void markScheduleDisconnect() {
        long startTimestamp = System.currentTimeMillis();

        scheduledDisconnectTimestamp = startTimestamp;
    }

    public long getSuggestedDisconnectInterval() {
        long averageNewCommandDelay = 100;

        if (newCommandAmount > 0) {
            averageNewCommandDelay = newCommandTimeDelayTotal / newCommandAmount;
        }

        long result = averageNewCommandDelay * 10;

        return result;
    }

    public void markNewCommand() {
        long startTimestamp = System.currentTimeMillis();

        if (scheduledDisconnectTimestamp != 0) {
            long timeDiff = startTimestamp - scheduledDisconnectTimestamp;

            newCommandTimeDelayTotal += timeDiff;
            newCommandAmount++;

            scheduledDisconnectTimestamp = 0;
        }
    }

    public void setFilePathInterpreter(FilePathInterpreter filePathInterpreter) {
        this.filePathInterpreter = filePathInterpreter;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public void setControlConnectHandler(ControlConnectHandler controlConnectHandler) {
        this.controlConnectHandler = controlConnectHandler;
    }

    public void setDataSocket(AsyncSocket socket) {
        data_socket = socket;

        if ((fileToSend != null) && (data_socket != null)) {
            startSendFileContentForLarge();
        }
    }

    private void startSendFileContentForLarge() {
        if (fileToSend.exists()) {
            notifyFileSendStarted();

            try {
                Uri fileUri = fileToSend.getUri();

                final InputStream is = context.getContentResolver().openInputStream(fileUri);

                if (restSTart > 0) {
                    is.skip(restSTart);

                    restSTart = 0;
                }

                Util.pump(is, data_socket, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex != null) {
                            if (ex instanceof IOException) {
                                ex.printStackTrace();
                            } else {
                                throw new RuntimeException(ex);
                            }
                        }

                        Log.d(TAG, "startSendFileContentForLarge, file sent.");


                        delayednotifyFileSendCompleted();

                        fileToSend = null;
                        data_socket.close();
                        data_socket = null;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            notifyFileNotExist();
        }
    }

    public void sendFileContent(String data51, String currentWorkingDirectory) {
        wholeDirecotoryPath = rootDirectory.getPath() + currentWorkingDirectory + data51;
        wholeDirecotoryPath = wholeDirecotoryPath.replace("//", "/");
        DocumentFile photoDirecotry = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, data51);
        fileToSend = photoDirecotry;
        if (data_socket != null) {
            startSendFileContentForLarge();
        }
    }

    private void notifyFileSendCompleted() {
        controlConnectHandler.notifyFileSendCompleted();
    }

    private void delayednotifyFileSendCompleted() {
        controlConnectHandler.delayednotifyFileSendCompleted();
    }

    private void notifyFileSendStarted() {
        controlConnectHandler.notifyFileSendStarted(wholeDirecotoryPath);
    }

    private void notifyFileNotExist() {
        controlConnectHandler.notifyFileNotExist(wholeDirecotoryPath);
    }

    private void queueForDataSocket(byte[] output) {
        dataSocketPendingByteArray = output;
    }

    private void queueForDataSocket(String output) {
        dataSocketPendingByteArray = output.getBytes();
    }
}
