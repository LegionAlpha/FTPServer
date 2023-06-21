package com.legion.ftplib;

import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class DirectoryListSender {
    private boolean fileNameTolerant = false;
    private FilePathInterpreter filePathInterpreter = null;
    private byte[] dataSocketPendingByteArray = null;
    private ControlConnectHandler controlConnectHandler = null;
    private AsyncSocket data_socket = null;
    private File rootDirectory = null;
    private String wholeDirecotoryPath = "";
    private DocumentFile fileToSend = null;
    private String subDirectoryName = null;
    private static final String TAG = "DirectoryListSender";
    private BinaryStringSender binaryStringSender = new BinaryStringSender();

    public void setFilePathInterpreter(FilePathInterpreter filePathInterpreter) {
        this.filePathInterpreter = filePathInterpreter;
    }

    public void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public void setControlConnectHandler(ControlConnectHandler controlConnectHandler) {
        this.controlConnectHandler = controlConnectHandler;
    }

    public void setDataSocket(AsyncSocket socket) {
        data_socket = socket;
        binaryStringSender.setSocket(data_socket);
        if ((fileToSend != null) && (data_socket != null)) {
            startSendFileContentForLarge();
        }
    }

    private String construct1LineListFile(DocumentFile photoDirecotry) {


        DocumentFile path = photoDirecotry;


        String fileName = path.getName();

        Date dateCompareYear = new Date(path.lastModified());
        Date dateNow = new Date();
        boolean sameYear = false;

        if (dateCompareYear.getYear() == dateNow.getYear()) {
            sameYear = true;
        }

        LocalDateTime date =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(path.lastModified()), ZoneId.systemDefault());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        String time = "8:00";

        time = date.format(formatter);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM");

        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy").withLocale(Locale.US);

        String year = date.format(yearFormatter);

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM").withLocale(Locale.US);

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd").withLocale(Locale.US);

        String dateString = "30";

        dateString = date.format(dayFormatter);

        long fileSize = path.length();

        String group = "cx";

        String user = "ChenXin";


        Uri directoryUri = path.getUri();
        String directyoryUriPath = directoryUri.getPath();


        File fileObject = new File(directyoryUriPath);
        Path filePathObject = fileObject.toPath();

        if (directoryUri.getScheme().equals("file")) {
            try {
                UserPrincipal userPrincipal = Files.getOwner(filePathObject);
                user = userPrincipal.getName();
            } catch (IOException e) {
                Log.d(TAG, "construct1LineListFile, failed to get owner name:");

                e.printStackTrace();
            }
        }

        String linkNumber = "1";


        String permission = getPermissionForFile(path);

        String month = "Jan";

        month = date.format(monthFormatter);

        String timeOrYear = time;

        if (sameYear) {
        } else {
            timeOrYear = year;
        }

        String currentLine = permission + " " + linkNumber + " " + user + " " + group + " " + fileSize + " " + month + " " + dateString + " " + timeOrYear + " " + fileName;

        return currentLine;
    }

    public void setFileNameTolerant(boolean toleranttrue) {
        fileNameTolerant = toleranttrue;
    }

    private String getDirectoryContentList(DocumentFile photoDirecotry, String nameOfFile) {
        nameOfFile = nameOfFile.trim();

        String result = "";

        if (photoDirecotry.isFile()) {
            String currentLine = construct1LineListFile(photoDirecotry);

            binaryStringSender.sendStringInBinaryMode(currentLine);
        } else {
            DocumentFile[] paths = photoDirecotry.listFiles();


            if (paths.length == 0) {
                controlConnectHandler.checkFileManagerPermission(Constants.Permission.Read, null);
            } else {
                PathDocumentFileCacheManager pathDocumentFileCacheManager = filePathInterpreter.getPathDocumentFileCacheManager();
                for (DocumentFile path : paths) {
                    String currentLine = construct1LineListFile(path);

                    String fileName = path.getName();


                    String effectiveVirtualPathForCurrentSegment = wholeDirecotoryPath + "/" + fileName;
                    effectiveVirtualPathForCurrentSegment = effectiveVirtualPathForCurrentSegment.replace("//", "/");


                    pathDocumentFileCacheManager.put(effectiveVirtualPathForCurrentSegment, path);

                    if (fileNameTolerant) {
                        String tolerantEffectiveVirtualPath = effectiveVirtualPathForCurrentSegment.trim();

                        if (tolerantEffectiveVirtualPath.equals(effectiveVirtualPathForCurrentSegment)) {
                        } else {
                            DocumentFile documentFileForTolerantPath = pathDocumentFileCacheManager.get(tolerantEffectiveVirtualPath);

                            if (documentFileForTolerantPath == null) {
                                pathDocumentFileCacheManager.put(tolerantEffectiveVirtualPath, path);
                            }
                        }
                    }

                    if (fileName.equals(nameOfFile) || (nameOfFile.isEmpty())) {
                        binaryStringSender.sendStringInBinaryMode(currentLine);
                    }
                }
            }
        }

        Util.writeAll(data_socket, ("\r\n").getBytes(), new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                if (ex != null) throw new RuntimeException(ex);
                notifyLsCompleted();
                fileToSend = null;
                data_socket.close();
            }
        });

        return result;
    }

    private String getPermissionForFile(DocumentFile path) {
        String permission = "-rw-r--r--";

        if (path.isDirectory()) {
            permission = "drw-r--r--";
        }

        return permission;
    }

    private void startSendFileContentForLarge() {
        if (fileToSend.exists()) {
            getDirectoryContentList(fileToSend, subDirectoryName);
        } else {
            notifyFileNotExist();
        }
    }

    public void sendDirectoryList(String data51, String currentWorkingDirectory) {
        String parameter = "";

        int directoryIndex = 5;

        if (directoryIndex <= (data51.length() - 1)) {
            parameter = data51.substring(directoryIndex).trim();
        }

        if (parameter.equals("-la")) {
            parameter = "";
        }

        subDirectoryName = parameter;

        wholeDirecotoryPath = filePathInterpreter.resolveWholeDirectoryPath(rootDirectory, currentWorkingDirectory, parameter);
        DocumentFile photoDirecotry = filePathInterpreter.getFile(rootDirectory, currentWorkingDirectory, parameter);

        fileToSend = photoDirecotry;

        if (data_socket != null) {
            startSendFileContentForLarge();
        } else {
        }
    }

    private void notifyLsCompleted() {
        controlConnectHandler.notifyLsCompleted();
    }

    private void notifyFileSendCompleted() {
        controlConnectHandler.notifyFileSendCompleted();
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
