package com.legion.ftplib;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.HashMap;

public class ExternalStorageUriGuessor {
    private static final String TAG = "ExternalStorageUriGuessor";
    private HashMap<String, Uri> virtualPathMap = new HashMap<>();
    private Context context = null;
    private boolean externalStoragePerformanceOptimize = false;

    public void setContext(Context context) {
        this.context = context;
    }

    public boolean isSamePath(String fullPath, String ConstantsFilePathAndroidData) {
        File fullPathFile = new File(fullPath);
        File constantsFilePathAndroidDataFile = new File(ConstantsFilePathAndroidData);
        boolean result = fullPathFile.getPath().equals(constantsFilePathAndroidDataFile.getPath());
        return result;
    }

    public Uri getVirtualPath(String path) {
        return virtualPathMap.get(path);
    }

    public void setExternalStoragePerformanceOptimize(boolean isChecked) {
        externalStoragePerformanceOptimize = isChecked;
    }

    public Uri guessUri(Uri sourceUrit) {
        Uri result = sourceUrit;
        String sourceUriString = sourceUrit.toString();
        Uri cachedResult = virtualPathMap.get(sourceUriString);

        if (cachedResult != null) {
            result = cachedResult;
        } else {
            if (sourceUriString.startsWith("content://com.android.externalstorage.documents/")) {
                String filePath = "";
                DocumentFile documentFile = DocumentFile.fromTreeUri(context, sourceUrit);
                Uri uriWithDocumentId = documentFile.getUri();

                try {
                    String docId = DocumentsContract.getDocumentId(uriWithDocumentId);
                    String[] split = docId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        String wholePath = Environment.getExternalStorageDirectory() + "/" + split[1];
                        File whoelPathFile = new File(wholePath);
                        File[] paths = whoelPathFile.listFiles();

                        if (paths != null) {
                            if (paths.length == 0) {

                            } else {
                                result = Uri.fromFile(whoelPathFile);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            virtualPathMap.put(sourceUriString, result);

        }

        return result;
    }

    public String resolveWholeDirectoryPath(File rootDirectory, String currentWorkingDirectory, String data51) {
        String currentWorkingDirectoryUpdate = currentWorkingDirectory;

        if (data51.startsWith("/")) {
            currentWorkingDirectoryUpdate = "/";
        }

        String wholeDirecotoryPath = rootDirectory.getPath() + currentWorkingDirectoryUpdate + "/" + data51;

        wholeDirecotoryPath = wholeDirecotoryPath.replace("//", "/");

        return wholeDirecotoryPath;
    }
}