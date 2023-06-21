package com.legion.ftplib;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.HashMap;

public class PathDocumentFileCacheManager {
    private static final String TAG = "PathDocumentFileCacheManager";
    private HashMap<String, Uri> virtualPathMap = new HashMap<>();
    private HashMap<String, DocumentFile> pathDocumentFileMap = new HashMap<>();

    public boolean isSamePath(String fullPath, String ConstantsFilePathAndroidData) {
        File fullPathFile = new File(fullPath);
        File constantsFilePathAndroidDataFile = new File(ConstantsFilePathAndroidData);
        boolean result = fullPathFile.getPath().equals(constantsFilePathAndroidDataFile.getPath());
        return result;
    }

    public Uri getVirtualPath(String path) {
        return virtualPathMap.get(path);
    }

    private String getParentVirtualPathByVirtualPathMap(String wholeDirecotoryPath) {
        boolean result = false;
        String currentTryingPath = wholeDirecotoryPath;
        String theFinalPath = null;

        while ((!currentTryingPath.equals("/")) && (!result)) {
            result = virtualPathMap.containsKey(currentTryingPath);
            if (result) {
                break;
            }

            File virtualFile = new File(currentTryingPath);
            File parentVirtualFile = virtualFile.getParentFile();
            currentTryingPath = parentVirtualFile.getPath();
            if (currentTryingPath.endsWith("/")) {
            } else {
                currentTryingPath = currentTryingPath + "/";
            }
        }

        if (result) {
            theFinalPath = currentTryingPath;
        }
        return theFinalPath;
    }

    public void remove(String effectiveVirtualPathForCurrentSegment) {
        pathDocumentFileMap.remove(effectiveVirtualPathForCurrentSegment);
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

    public void put(String effectiveVirtualPathForCurrentSegment, DocumentFile targetdocumentFile) {
        DocumentFile result = null;
        {
            {
                {
                    effectiveVirtualPathForCurrentSegment = effectiveVirtualPathForCurrentSegment.replace("//", "/");
                    {
                        if (targetdocumentFile != null) {
                            pathDocumentFileMap.put(effectiveVirtualPathForCurrentSegment, targetdocumentFile);
                        }
                    }
                }
            }
            result = targetdocumentFile;
        }
    }

    public DocumentFile get(String effectiveVirtualPathForCurrentSegment) {
        DocumentFile result = null;

        {
            DocumentFile targetdocumentFile = null;
            {
                {
                    effectiveVirtualPathForCurrentSegment = effectiveVirtualPathForCurrentSegment.replace("//", "/");
                    DocumentFile cachedtargetdocumentFile = pathDocumentFileMap.get(effectiveVirtualPathForCurrentSegment);
                    if (cachedtargetdocumentFile != null) {
                        targetdocumentFile = cachedtargetdocumentFile;
                    }
                }
            }
            result = targetdocumentFile;
        }

        return result;
    }

    private DocumentFile getDocumentFileFromUri(Context context, Uri uri) {
        DocumentFile result;

        if (uri.getScheme().equals("file")) {
            File photoDirecotry = new File(uri.getPath());
            result = DocumentFile.fromFile(photoDirecotry);
        } else {
            result = DocumentFile.fromTreeUri(context, uri);
        }

        return result;
    }
}