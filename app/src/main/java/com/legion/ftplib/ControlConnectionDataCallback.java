package com.legion.ftplib;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;

import java.io.File;
import java.net.InetAddress;

public class ControlConnectionDataCallback implements DataCallback {
    private FilePathInterpreter filePathInterpreter = null;
    private String passWord = null;
    private boolean authenticated = true;
    private String userName = null;
    private UserManager userManager = null;
    private BinaryStringSender binaryStringSender = new BinaryStringSender();
    private EventListener eventListener = null;
    private ErrorListener errorListener = null;
    private AsyncSocket socket;
    private static final String TAG = "ControlConnectionDataCallback";
    private ControlConnectHandler context;

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

    private DataServerManager dataServerManager = new DataServerManager();

    private DocumentFile writingFile;

    private boolean isUploading = false;
    private InetAddress host;
    private File rootDirectory = null;

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
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

    public ControlConnectionDataCallback(ControlConnectHandler context) {
        this.context = context;
        this.allowActiveMode = allowActiveMode;
        this.host = host;
        this.ip = ip;
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

            Log.d(TAG, "delete result: " + deleteResult);

            if (deleteResult) {
                notifyEvent(EventListener.DELETE);
                replyString = "250 Delete success " + data51;

                PathDocumentFileCacheManager pathDocumentFileCacheManager = filePathInterpreter.getPathDocumentFileCacheManager();
                {
                    String effectiveVirtualPathForCurrentSegment = wholeDirecotoryPath;
                    effectiveVirtualPathForCurrentSegment = effectiveVirtualPathForCurrentSegment.replace("//", "/");
                    pathDocumentFileCacheManager.remove(effectiveVirtualPathForCurrentSegment);
                }
            } else {
                replyString = "550 File delete failed";
                checkFileManagerPermission(Constants.Permission.Write, photoDirecotry);
            }
        } else {
            replyString = "550 File delete failed " + data51;
        }
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

    @Override

    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        String content = new String(bb.getAllByteArray());
        String[] lines = content.split("\r\n");
        int lineAmount = lines.length;
        for (int lineCounter = 0; lineCounter < lineAmount; lineCounter++) {
            String currentLine = lines[lineCounter];
            String command = currentLine.split(" ")[0];
            command = command.trim();
            boolean hasFolloingCommand = true;
            if ((lineCounter + 1) == (lineAmount)) {
                hasFolloingCommand = false;
            }
            context.processCommand(command, currentLine, hasFolloingCommand);
        }
    }
}
