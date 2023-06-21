package com.legion.ftplib;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.format.Formatter;
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ControlConnectHandler implements DataServerManagerInterface {
    private FilePathInterpreter filePathInterpreter = null;
    private String passWord = null;
    private boolean authenticated = true;
    private String userName = null;
    private UserManager userManager = null;
    private BinaryStringSender binaryStringSender = new BinaryStringSender();
    private EventListener eventListener = null;
    private ErrorListener errorListener = null;
    private AsyncSocket socket;
    private static final String TAG = "ControlConnectHandler";
    private Context context;
    private AsyncSocket data_socket;
    private FileContentSender fileContentSender = new FileContentSender();
    private DirectoryListSender directoryListSender = new DirectoryListSender();
    private byte[] dataSocketPendingByteArray = null;
    private String currentWorkingDirectory = "/";
    private int data_port = 1544;
    private String ip;
    private String clientIp;
    private int clientDataPort;
    private int retryConnectClientDataPortAmount = 0;
    private boolean allowActiveMode = true;
    private DisconnectIntervalManager disconnectIntervalManager = new DisconnectIntervalManager();
    private DataServerManager dataServerManager = new DataServerManager();
    private Timer disconnectTimer = null;
    private DocumentFile writingFile;
    private boolean isUploading = false;
    private InetAddress host;
    private File rootDirectory = null;

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setFilePathInterpreter(FilePathInterpreter filePathInterpreter) {
        this.filePathInterpreter = filePathInterpreter;
        directoryListSender.setFilePathInterpreter(filePathInterpreter);
        fileContentSender.setFilePathInterpreter(filePathInterpreter);
        this.filePathInterpreter.setContext(context);
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
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

    public void setFileNameTolerant(boolean toleranttrue) {
        directoryListSender.setFileNameTolerant(toleranttrue);
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

    public ControlConnectHandler(Context context, boolean allowActiveMode, InetAddress host, String ip) {
        this.context = context;
        this.allowActiveMode = allowActiveMode;
        this.host = host;
        this.ip = ip;

        fileContentSender.setContext(context);
    }

    private void connectToClientDataPort() {
        String ip = clientIp;
        int port = clientDataPort;

        AsyncServer.getDefault().connectSocket(new InetSocketAddress(ip, port), new ConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, final AsyncSocket socket) {
                handleConnectCompleted(ex, socket);
            }
        });
    }

    private void openDataConnectionToClient(String content) {
        String portString = content.split(" ")[1].trim();
        String[] addressStringList = portString.split(",");

        String ip = addressStringList[0] + "." + addressStringList[1] + "." + addressStringList[2] + "." + addressStringList[3];
        int port = Integer.parseInt(addressStringList[4]) * 256 + Integer.parseInt(addressStringList[5]);

        clientIp = ip;
        clientDataPort = port;

        retryConnectClientDataPortAmount = 0;
        connectToClientDataPort();
    }

    public void notifyFileSendStarted(String filePath) {
        String replyString = "150 start send content: " + filePath;
        Log.d(TAG, "reply string: " + replyString);
        binaryStringSender.sendStringInBinaryMode(replyString);
    }

    public void notifyFileNotExist(String filePath) {
        String replyString = "550 File not exist " + filePath;
        binaryStringSender.sendStringInBinaryMode(replyString);
    }

    private void cancelDisconnectTimer() {
        if (disconnectTimer != null) {
            disconnectTimer.cancel();
        }
    }

    private void scheduleDisconnect() {
        Timer timerObj = new Timer();

        cancelDisconnectTimer();

        disconnectTimer = timerObj;

        TimerTask timerTaskObj = new TimerTask() {
            public void run() {
                socket.close();
            }
        };

        long suggestedInterfal20 = disconnectIntervalManager.getSuggestedDisconnectInterval();
        timerObj.schedule(timerTaskObj, suggestedInterfal20);
        disconnectIntervalManager.markScheduleDisconnect();
    }

    public void delayednotifyFileSendCompleted() {
        Timer timerObj = new Timer();
        TimerTask timerTaskObj = new TimerTask() {
            public void run() {
                notifyFileSendCompleted();
            }
        };
        timerObj.schedule(timerTaskObj, 20);
    }

    public void notifyFileSendCompleted() {
        String replyString = "226 File sent. ";
        binaryStringSender.sendStringInBinaryMode(replyString);
        scheduleDisconnect();
        notifyEvent(EventListener.DOWNLOAD_FINISH);
    }

    private void sendFileContent(String data51, String currentWorkingDirectory) {
        fileContentSender.setControlConnectHandler(this);
        fileContentSender.setDataSocket(data_socket);
        fileContentSender.sendFileContent(data51, currentWorkingDirectory);
        notifyEvent(EventListener.DOWNLOAD_START);
    }

    private void sendListContentBySender(String fileName, String currentWorkingDirectory) {
        directoryListSender.setControlConnectHandler(this);
        directoryListSender.setDataSocket(data_socket);
        directoryListSender.sendDirectoryList(fileName, currentWorkingDirectory);
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

    private void processRetrCommand(String data51) {
        sendFileContent(data51, currentWorkingDirectory);
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

    private void processFeatCommand() {
        binaryStringSender.sendStringInBinaryMode("211-Feature list");
        binaryStringSender.sendStringInBinaryMode(" UTF8");
        binaryStringSender.sendStringInBinaryMode("211 end");
    }

    private void processUserCommand(String userName) {
        this.userName = userName;
        binaryStringSender.sendStringInBinaryMode("331 Send password");
    }

    private void processCwdCommand(String targetWorkingDirectory) {
        DocumentFile photoDirecotry = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, targetWorkingDirectory);

        String replyString = "";
        String fullPath = filePathInterpreter.resolveWholeDirectoryPath(rootDirectory, currentWorkingDirectory, targetWorkingDirectory);

        if (photoDirecotry.isDirectory()) {
            String rootPath = rootDirectory.getPath();
            currentWorkingDirectory = fullPath.substring(rootPath.length());

            if (currentWorkingDirectory.isEmpty()) {
                currentWorkingDirectory = "/";
            }

            Log.d(TAG, "processCwdCommand, rootPath: " + rootPath);
            Log.d(TAG, "processCwdCommand, currentWorkingDirectory: " + currentWorkingDirectory);

            replyString = "250 cwd succeed";
        } else {
            replyString = "550 not a directory: " + targetWorkingDirectory;
        }

        binaryStringSender.sendStringInBinaryMode(replyString);

        if (filePathInterpreter.isSamePath(fullPath, Constants.FilePath.AndroidData)) {
            CheckAndroidDataPermission();
        }
    }

    private void processSizeCommand(String data51) {
        Log.d(TAG, "processSizeCommand: filesdir: " + rootDirectory.getPath());
        Log.d(TAG, "processSizeCommand: data51: " + data51);

        DocumentFile photoDirecotry = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, data51);

        String replyString = "";

        if ((photoDirecotry != null) && (photoDirecotry.exists() && (photoDirecotry.isFile()))) {
            long fileSize = photoDirecotry.length();
            replyString = "213 " + fileSize + " ";
        } else {
            if ((photoDirecotry == null) || (!photoDirecotry.exists())) {
                replyString = "550 File not exist " + data51;
            } else {
                replyString = "550 No directory traversal allowed in SIZE param";
            }
        }

        Log.d(TAG, "reply string: " + replyString);
        binaryStringSender.sendStringInBinaryMode(replyString);
    }

    private void processDeleCommand(String data51) {
        String wholeDirecotoryPath = rootDirectory.getPath() + currentWorkingDirectory + data51;

        wholeDirecotoryPath = wholeDirecotoryPath.replace("//", "/");

        DocumentFile photoDirecotry = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, data51);

        String replyString = "250 ";

        if (photoDirecotry != null) {
            boolean deleteResult = photoDirecotry.delete();

            if (deleteResult) {
                notifyEvent(EventListener.DELETE, (Object) (photoDirecotry));

                replyString = "250 Delete success " + data51;

                PathDocumentFileCacheManager pathDocumentFileCacheManager = filePathInterpreter.getPathDocumentFileCacheManager();

                String effectiveVirtualPathForCurrentSegment = wholeDirecotoryPath;
                effectiveVirtualPathForCurrentSegment = effectiveVirtualPathForCurrentSegment.replace("//", "/");
                pathDocumentFileCacheManager.remove(effectiveVirtualPathForCurrentSegment);
            } else {
                replyString = "550 File delete failed";
                checkFileManagerPermission(Constants.Permission.Write, photoDirecotry);
            }
        } else {
            replyString = "550 File delete failed " + data51;
        }

        binaryStringSender.sendStringInBinaryMode(replyString);
    }

    private void processPasvCommand() {
        data_socket = null;
        setupDataServer();
        String ipAddress = ip;

        if (ipAddress == null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        }

        String ipString = ipAddress.replace(".", ",");

        int port256 = data_port / 256;
        int portModule = data_port - port256 * 256;

        String replyString = "227 Entering Passive Mode (" + ipString + "," + port256 + "," + portModule + ") ";
        binaryStringSender.sendStringInBinaryMode(replyString);
    }

    public void processCommand(String command, String content, boolean hasFolloingCommand) {
        cancelDisconnectTimer();
        disconnectIntervalManager.markNewCommand();

        if (command.equals("SYST")) {
            binaryStringSender.sendStringInBinaryMode("215 UNIX Type: L8");
        } else if (command.equals("PWD")) {
            String replyString = "257 \"" + currentWorkingDirectory + "\"";
            Log.d(TAG, "reply string: " + replyString);
            binaryStringSender.sendStringInBinaryMode(replyString);
        } else if (command.equals("TYPE")) {
            String replyString = "200 binary type set";
            Log.d(TAG, "reply string: " + replyString);
            binaryStringSender.sendStringInBinaryMode(replyString);
        } else if (command.equalsIgnoreCase("PASV")) {
            processPasvCommand();
        } else if (command.equals("EPSV")) {
            String replyString = "202 ";

            if (hasFolloingCommand) {
            } else {
                Log.d(TAG, "reply string: " + replyString);
                binaryStringSender.sendStringInBinaryMode(replyString);
            }
        } else if (command.equals("PORT")) {
            String replyString = "150 ";
            boolean shouldSend = true;
            if (allowActiveMode) {
                data_socket = null;
                openDataConnectionToClient(content);
                replyString = "150 ";
            } else {
                replyString = "202 ";
                if (hasFolloingCommand) {
                    shouldSend = false;
                }
            }

            if (shouldSend) {
                Log.d(TAG, "reply string: " + replyString);
                binaryStringSender.sendStringInBinaryMode(replyString);
            }
        } else if (command.toLowerCase().equals("list")) {
            processListCommand(content);
        } else if (command.toLowerCase().equals("retr")) {
            String data51 = content.substring(5);
            data51 = data51.trim();
            processRetrCommand(data51);
        } else if (command.toLowerCase().equals("rest")) {
            String data51 = content.substring(5);
            data51 = data51.trim();
            String replyString = "350 Restart position accepted (" + data51 + ")";
            Log.d(TAG, "reply string: " + replyString);
            binaryStringSender.sendStringInBinaryMode(replyString);
            Long restartPosition = Long.valueOf(data51);
            fileContentSender.setRestartPosition(restartPosition);
        } else if (command.equalsIgnoreCase("USER")) {
            String targetWorkingDirectory = content.substring(5).trim();
            processUserCommand(targetWorkingDirectory);
        } else if (command.equalsIgnoreCase("feat")) {
            processFeatCommand();
        } else if (command.equalsIgnoreCase("PASS")) {
            String targetWorkingDirectory = content.substring(5).trim();
            processPassCommand(targetWorkingDirectory);
        } else if (command.equalsIgnoreCase("cwd")) {
            String targetWorkingDirectory = content.substring(4).trim();
            processCwdCommand(targetWorkingDirectory);
        } else if (command.equalsIgnoreCase("stor")) {
            String data51 = content.substring(5);
            data51 = data51.trim();
            processStorCommand(data51);
        } else if (command.equalsIgnoreCase("quit")) {
            processQuitCommand();
        } else if (command.equals("SIZE")) {
            String data51 = content.substring(5);
            data51 = data51.trim();
            processSizeCommand(data51);
        } else if (command.equals("DELE")) {
            String data51 = content.substring(5);
            data51 = data51.trim();
            processDeleCommand(data51);
        } else if (command.equals("RMD")) {
            String data51 = content.substring(4);

            data51 = data51.trim();

            String wholeDirecotoryPath = rootDirectory.getPath() + currentWorkingDirectory + data51;
            wholeDirecotoryPath = wholeDirecotoryPath.replace("//", "/");
            DocumentFile photoDirecotry = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, data51);

            boolean deleteResult = photoDirecotry.delete();
            Log.d(TAG, "delete result: " + deleteResult);
            notifyEvent(EventListener.DELETE);
            String replyString = "250 Delete success " + data51;
            Log.d(TAG, "reply string: " + replyString);
            binaryStringSender.sendStringInBinaryMode(replyString);
        } else {
            String replyString = "502 " + content.trim() + " not implemented";
            Log.d(TAG, "reply string: " + replyString);

            binaryStringSender.sendStringInBinaryMode(replyString);
        }
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

    private void notifyError(Integer eventCode) {
        if (errorListener != null) {
            Handler uiHandler = new Handler(Looper.getMainLooper());

            Runnable runnable = new Runnable() {
                public void run() {
                    errorListener.onError(eventCode);
                }
            };

            uiHandler.post(runnable);
        }
    }

    private void notifyEvent(final String eventCode) {
        notifyEvent(eventCode, null);
    }

    public void checkFileManagerPermission(int permissinTypeCode, DocumentFile targetFile) {
        Log.d(TAG, "checkFileManagerPermission ");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isFileManager = Environment.isExternalStorageManager();
            Log.d(TAG, "checkFileManagerPermission, is file manager: " + isFileManager);
            if (isFileManager) {
            } else {
                if (permissinTypeCode == Constants.Permission.Read) {
                    File photoDirecotry = Environment.getExternalStorageDirectory();
                    File[] paths = photoDirecotry.listFiles();
                    if (paths == null) {
                        notifyEvent(EventListener.NEED_EXTERNAL_STORAGE_MANAGER_PERMISSION, null);
                    }
                } else {
                    boolean canDelete = targetFile.canWrite();

                    if (canDelete) {
                    } else {
                        notifyEvent(EventListener.NEED_EXTERNAL_STORAGE_MANAGER_PERMISSION, null);
                    }
                }
            }
        }
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

    private void requestAndroidDataPermission() {
        File androidDataFile = new File(Constants.FilePath.AndroidData);
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata");
        openDirectory(uri);
    }

    public void openDirectory(Uri uriToLoad) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);

        String packageNmae = context.getPackageName();
        Log.d(TAG, "gotoFileManagerSettingsPage, package name: " + packageNmae);

        String url = "package:" + packageNmae;

        Log.d(TAG, "gotoFileManagerSettingsPage, url: " + url);

        int yourrequestcode = Constants.RequestCode.AndroidDataPermissionRequestCode;

        DocumentTreeBrowseRequest browseRequest = new DocumentTreeBrowseRequest();
        browseRequest.setRequestCode(yourrequestcode);
        browseRequest.setIntent(intent);

        notifyEvent(EventListener.NEED_BROWSE_DOCUMENT_TREE, (Object) (browseRequest));
    }

    private void CheckAndroidDataPermission() {
        File photoDirecotry = new File(Constants.FilePath.AndroidData);

        File[] paths = photoDirecotry.listFiles();

        if (paths == null) {
            if (filePathInterpreter.virtualPathExists(Constants.FilePath.AndroidData)) {
            } else {
                requestAndroidDataPermission();
            }
        }
    }

    private void processListCommand(String content) {
        String replyString = "150 Opening BINARY mode data connection for file list, ChenXin";
        binaryStringSender.sendStringInBinaryMode(replyString);
        sendListContentBySender(content, currentWorkingDirectory);
    }

    private void handleConnectCompleted(Exception ex, final AsyncSocket socket) {
        if (ex != null) {
            if (retryConnectClientDataPortAmount >= 10) {
            } else {
                connectToClientDataPort();
                retryConnectClientDataPortAmount++;
            }
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
                    if (ex != null) {

                        ex.printStackTrace();
                    }

                    System.out.println("[Client] Successfully closed connection");

                    data_socket = null;

                    if (writingFile != null) {
                        notifyStorCompleted();
                    }
                }
            });

            socket.setEndCallback(new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    if (ex != null) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void handleDataAccept(final AsyncSocket socket) {
        this.data_socket = socket;
        fileContentSender.setDataSocket(socket);
        directoryListSender.setDataSocket(socket);

        socket.setDataCallback(
                new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        receiveDataSocket(bb);
                    }
                });

        socket.setClosedCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    if (ex instanceof IOException) {
                        ex.printStackTrace();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }

                System.out.println("[Server] data Successfully closed connection");
                data_socket = null;
                fileContentSender.setDataSocket(data_socket);
                directoryListSender.setDataSocket(data_socket);
                if (isUploading) {
                    notifyStorCompleted();
                    isUploading = false;
                }
            }
        });
    }

    public void handleAccept(final AsyncSocket socket) {
        this.socket = socket;
        binaryStringSender.setSocket(socket);
        ControlConnectionDataCallback dataCallback = new ControlConnectionDataCallback(this);
        socket.setDataCallback(dataCallback);
        socket.setClosedCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    ex.printStackTrace();
                } else {
                    System.out.println("[Server] Successfully closed connection");
                }
            }
        });

        socket.setEndCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    notifyError(Constants.ErrorCode.ControlConnectionEndedUnexpectedly);
                    ex.printStackTrace();
                } else {
                    dataServerManager.stopServerSockets();
                }
            }
        });

        binaryStringSender.sendStringInBinaryMode("220 StupidBeauty FtpServer");
    }

    @Override
    public void setupDataServer() {
        setupDataServerByManager();
    }

    private void setupDataServerByManager() {
        data_port = dataServerManager.setupDataServer(this);
    }

    private void setupDataServerListen() {
        Random random = new Random();

        int randomIndex = random.nextInt(65535 - 1025) + 1025;

        data_port = randomIndex;

        AsyncServer.getDefault().listen(host, data_port, new ListenCallback() {
            @Override
            public void onAccepted(final AsyncSocket socket) {
                handleDataAccept(socket);
            }

            @Override
            public void onListening(AsyncServerSocket socket) {
                System.out.println("[Server] Server started listening for data connections");
            }

            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) {
                    ex.printStackTrace();

                    setupDataServer();
                } else {
                    System.out.println("[Server] Successfully shutdown server");
                }
            }
        });
    }
}