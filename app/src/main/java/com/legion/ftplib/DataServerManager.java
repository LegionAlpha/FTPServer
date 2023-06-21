package com.legion.ftplib;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.ListenCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DataServerManager {
    private Map<Integer, Integer> dataPortUsageMap = new HashMap<>();
    private List<Integer> dataPortPool = new ArrayList<>();
    private FilePathInterpreter filePathInterpreter = null;
    private String passWord = null;
    private boolean authenticated = true;
    private String userName = null;
    private UserManager userManager = null;
    private BinaryStringSender binaryStringSender = new BinaryStringSender();
    private EventListener eventListener = null;
    private AsyncSocket socket;
    private static final String TAG = "DataServerManager";
    private Context context;
    private AsyncSocket data_socket;
    private FileContentSender fileContentSender = new FileContentSender();
    private DirectoryListSender directoryListSender = new DirectoryListSender();
    private byte[] dataSocketPendingByteArray = null;
    private String currentWorkingDirectory = "/";
    private AsyncServerSocket listeningServerSocket = null;
    private String ip;
    private boolean allowActiveMode = true;
    private DataServerManager dataServerManager = null;

    private DocumentFile writingFile;

    private boolean isUploading = false;
    private InetAddress host;
    private File rootDirectory = null;

    public void stopServerSockets() {
        listeningServerSocket.stop();
    }

    public void setFilePathInterpreter(FilePathInterpreter filePathInterpreter) {
        this.filePathInterpreter = filePathInterpreter;

        directoryListSender.setFilePathInterpreter(filePathInterpreter);
        fileContentSender.setFilePathInterpreter(filePathInterpreter);

        this.filePathInterpreter.setContext(context);
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
            Uri uri = writingFile.getUri();
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "wa");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

            fileOutputStream.write(content);
            fileOutputStream.close();
            pfd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openDataConnectionToClient(String content) {
        String portString = content.split(" ")[1].trim();

        String[] addressStringList = portString.split(",");

        String ip = addressStringList[0] + "." + addressStringList[1] + "." + addressStringList[2] + "." + addressStringList[3];
        int port = Integer.parseInt(addressStringList[4]) * 256 + Integer.parseInt(addressStringList[5]);


        AsyncServer.getDefault().connectSocket(new InetSocketAddress(ip, port), new ConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, final AsyncSocket socket) {
                handleConnectCompleted(ex, socket);
            }
        });
    }

    private void notifyStorCompleted() {
        String replyString = "226 Stor completed.";

        Log.d(TAG, "reply string: " + replyString);

        binaryStringSender.sendStringInBinaryMode(replyString);

        notifyEvent(EventListener.UPLOAD_FINISH, (Object) (writingFile));
    }

    public void notifyLsCompleted() {
        String replyString = "226 Data transmission OK. ChenXin";
        binaryStringSender.sendStringInBinaryMode(replyString);
        Log.d(TAG, "reply string: " + replyString);
    }

    private void processQuitCommand() {
        String replyString = "221 Quit OK. ChenXin";

        binaryStringSender.sendStringInBinaryMode(replyString);

        Log.d(TAG, "reply string: " + replyString);
    }

    private void processStorCommand(String data51) {
        String replyString = "150 ";

        binaryStringSender.sendStringInBinaryMode(replyString);

        startStor(data51, currentWorkingDirectory);
    }

    private void startStor(String data51, String currentWorkingDirectory) {
        DocumentFile photoDirecotry = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, data51);

        writingFile = photoDirecotry;
        isUploading = true;

        if (photoDirecotry != null && photoDirecotry.exists()) {
            photoDirecotry.delete();
        }

        try {
            File virtualFile = new File(data51);
            File parentVirtualFile = virtualFile.getParentFile();
            String currentTryingPath = parentVirtualFile.getPath();
            DocumentFile parentDocuemntFile = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, currentTryingPath);
            String fileNameOnly = virtualFile.getName();
            writingFile = parentDocuemntFile.createFile("", fileNameOnly);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processUserCommand(String userName) {
        this.userName = userName;
        binaryStringSender.sendStringInBinaryMode("331 Send password");
    }

    private void processSizeCommand(String data51) {
        Log.d(TAG, "processSizeCommand: filesdir: " + rootDirectory.getPath());
        Log.d(TAG, "processSizeCommand: workding directory: " + currentWorkingDirectory);
        Log.d(TAG, "processSizeCommand: data51: " + data51);

        DocumentFile photoDirecotry = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, data51);

        String replyString = "";

        if (photoDirecotry.exists()) {
            long fileSize = photoDirecotry.length();

            replyString = "213 " + fileSize + " ";
        } else {
            replyString = "550 No directory traversal allowed in SIZE param";
        }

        Log.d(TAG, "reply string: " + replyString);

        binaryStringSender.sendStringInBinaryMode(replyString);
    }

    private void notifyEvent(final String eventCode, final Object extraContent) {
        if (eventListener != null) {
            Handler uiHandler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                public void run() {
                    eventListener.onEvent(eventCode);
                    eventListener.onEvent(eventCode, extraContent);
                }
            };
            uiHandler.post(runnable);
        }
    }

    private void notifyEvent(final String eventCode) {
        notifyEvent(eventCode, null);
    }

    private void gotoFileManagerSettingsPage() {
        Log.d(TAG, "gotoFileManagerSettingsPage");

        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String packageNmae = context.getPackageName();
        Log.d(TAG, "gotoFileManagerSettingsPage, package name: " + packageNmae);

        String url = "package:" + packageNmae;

        Log.d(TAG, "gotoFileManagerSettingsPage, url: " + url);

        intent.setData(Uri.parse(url));

        context.startActivity(intent);
    }

    private void handleConnectCompleted(Exception ex, final AsyncSocket socket) {
        if (ex != null) {
            ex.printStackTrace();
        } else {
            this.data_socket = socket;
            fileContentSender.setDataSocket(socket);
            directoryListSender.setDataSocket(socket);


            socket.setDataCallback(new DataCallback() {
                @Override
                public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                    receiveDataSocket(bb);
                }
            });

            socket.setClosedCallback(new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    if (ex != null) throw new RuntimeException(ex);
                    System.out.println("[Client] Successfully closed connection");
                    data_socket = null;
                    notifyStorCompleted();
                }
            });

            socket.setEndCallback(new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    if (ex != null) throw new RuntimeException(ex);

                }
            });
        }
    }

    public int setupDataServer(DataServerManagerInterface dataServerManagerInterface) {
        int result = setupDataServerListen(dataServerManagerInterface);

        return result;
    }

    private int setupDataServerListen(DataServerManagerInterface dataServerManagerInterface) {
        int result = 0;

        boolean foundExistingPort = false;
        for (int currentPortInPool : dataPortPool) {
            {
                foundExistingPort = true;
                result = currentPortInPool;
                break;
            }
        }

        if (foundExistingPort) {
        } else {
            Random random = new Random();

            int randomIndex = random.nextInt(65535 - 1025) + 1025;

            final int data_port = randomIndex;
            result = randomIndex;

            AsyncServer.getDefault().listen(host, data_port, new ListenCallback() {
                @Override
                public void onAccepted(final AsyncSocket socket) {
                    dataServerManagerInterface.handleDataAccept(socket);

                    int dataPortUsageCounter = dataPortUsageMap.get(data_port);
                    dataPortUsageCounter++;
                    dataPortUsageMap.put(data_port, dataPortUsageCounter);

                    socket.setEndCallback(new CompletedCallback() {
                        @Override
                        public void onCompleted(Exception ex) {
                            if (ex != null) {
                                if (ex instanceof IOException) {
                                    ex.printStackTrace();
                                } else {
                                    throw new RuntimeException(ex);
                                }
                            }

                            int dataPortUsageCounter = dataPortUsageMap.get(data_port);
                            dataPortUsageCounter--;
                            dataPortUsageMap.put(data_port, dataPortUsageCounter);
                        }
                    });
                }

                @Override
                public void onListening(AsyncServerSocket socket) {
                    listeningServerSocket = socket;
                    dataPortPool.add(data_port);
                    int dataPortUsageCounter = 0;
                    dataPortUsageMap.put(data_port, dataPortUsageCounter);
                }

                @Override
                public void onCompleted(Exception ex) {
                    if (ex != null) {
                        ex.printStackTrace();
                        dataServerManagerInterface.setupDataServer();
                    } else {
                        System.out.println("[Server] Successfully shutdown server");
                    }

                    dataPortPool.remove(data_port);
                }
            });
        }

        return result;
    }
}