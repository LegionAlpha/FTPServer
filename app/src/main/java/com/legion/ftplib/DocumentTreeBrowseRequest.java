package com.legion.ftplib;

import android.content.Intent;
import android.util.Log;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.InetAddress;

public class DocumentTreeBrowseRequest {
    private String passWord = null;
    private boolean authenticated = true;
    private String userName = null;
    private UserManager userManager = null;
    private BinaryStringSender binaryStringSender = new BinaryStringSender();
    private EventListener eventListener = null;
    private AsyncSocket socket;
    private static final String TAG = "DocumentTreeBrowseRequest";
    private int requestCode;
    private AsyncSocket data_socket;
    private FileContentSender fileContentSender = new FileContentSender();
    private DirectoryListSender directoryListSender = new DirectoryListSender();
    private Intent intent = null;
    private String currentWorkingDirectory = "/";
    private int data_port = 1544;
    private String ip;
    private boolean allowActiveMode = true;
    private File writingFile;
    private boolean isUploading = false;
    private InetAddress host;
    private File rootDirectory = null;

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setRootDirectory(File root) {
        rootDirectory = root;
        Log.d(TAG, "setRootDirectory, rootDirectory: " + rootDirectory);
        fileContentSender.setRootDirectory(rootDirectory);
        directoryListSender.setRootDirectory(rootDirectory);
    }

    private void receiveDataSocket(ByteBufferList bb) {
        byte[] content = bb.getAllByteArray();
        boolean appendTrue = true;
        try {
            FileUtils.writeByteArrayToFile(writingFile, content, appendTrue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DocumentTreeBrowseRequest() {
        this.allowActiveMode = allowActiveMode;
        this.host = host;
        this.ip = ip;
    }

    public void setIntent(Intent content) {
        intent = content;
    }

    public void notifyFileNotExist() {
        String replyString = "550 File not exist";
        Log.d(TAG, "reply string: " + replyString);
        binaryStringSender.sendStringInBinaryMode(replyString);
    }

    public void notifyLsCompleted() {
        String replyString = "226 Data transmission OK.";

        binaryStringSender.sendStringInBinaryMode(replyString);

        Log.d(TAG, "reply string: " + replyString);
    }

    private void processPassCommand(String targetWorkingDirectory) {
        this.passWord = targetWorkingDirectory;

        if (userManager != null) {
            authenticated = userManager.authenticate(userName, passWord);
        }

        if (authenticated) {
            binaryStringSender.sendStringInBinaryMode("230 Loged in.");
        } else {
            binaryStringSender.sendStringInBinaryMode("430 Invalid username or password.");
        }
    }

    private void processUserCommand(String userName) {
        this.userName = userName;

        binaryStringSender.sendStringInBinaryMode("331 Send password");
    }

    public Intent getIntent() {
        return intent;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int socket) {
        this.requestCode = socket;
    }
}