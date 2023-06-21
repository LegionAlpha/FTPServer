package com.legion.ftplib;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.HashMap;

public class FilePathInterpreter implements VirtualPathLoadInterface {
    private static final String TAG = "FilePathInterpreter";
    private HashMap<String, Uri> virtualPathMap = new HashMap<>();
    private HashMap<String, DocumentFile> pathDocumentFileMap = new HashMap<>();
    private PathDocumentFileCacheManager pathDocumentFileCacheManager = new PathDocumentFileCacheManager();
    private Context context = null;
    private boolean externalStoragePerformanceOptimize = false;
    private ExternalStorageUriGuessor externalStorageUriGuessor = new ExternalStorageUriGuessor();

    public boolean isSamePath(String fullPath, String ConstantsFilePathAndroidData) {
        File fullPathFile = new File(fullPath);
        File constantsFilePathAndroidDataFile = new File(ConstantsFilePathAndroidData);
        boolean result = fullPathFile.getPath().equals(constantsFilePathAndroidDataFile.getPath());
        return result;
    }

    public void loadVirtualPathMap() {
        LoadVirtualPathMapTask loadVirtualPathMapTask = new LoadVirtualPathMapTask();
        loadVirtualPathMapTask.execute(this, context);
    }

    private void saveVirtualPathMap() {
        VirtualPathMapSaveTask translateRequestSendTask = new VirtualPathMapSaveTask();
        translateRequestSendTask.execute(virtualPathMap, context);
    }

    @Override
    public void setVoicePackageNameMap(HashMap<String, Uri> voicePackageNameMap) {
        virtualPathMap = voicePackageNameMap;
    }

    public Uri getVirtualPath(String path) {
        return virtualPathMap.get(path);
    }

    public void setExternalStoragePerformanceOptimize(boolean isChecked) {
        externalStoragePerformanceOptimize = isChecked;
    }

    public void unmountVirtualPath(String fullPath) {
        virtualPathMap.remove(fullPath);
        saveVirtualPathMap();
    }

    public void mountVirtualPath(String fullPath, Uri uri) {
        virtualPathMap.put(fullPath, uri);
        saveVirtualPathMap();
    }

    public void setContext(Context context) {
        this.context = context;
        externalStorageUriGuessor.setContext(context);
    }

    private Uri getParentUriByVirtualPathMap(String wholeDirecotoryPath) {
        String currentTryingPath = getParentVirtualPathByVirtualPathMap(wholeDirecotoryPath);
        Uri result = null;
        result = virtualPathMap.get(currentTryingPath);

        if (externalStoragePerformanceOptimize) {
            result = externalStorageUriGuessor.guessUri(result);
        }

        return result;
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


    public boolean virtualPathExists(String ConstantsFilePathAndroidData) {
        boolean result = false;

        String currentTryingPath = getParentVirtualPathByVirtualPathMap(ConstantsFilePathAndroidData);

        if (currentTryingPath != null) {
            result = true;
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

    public PathDocumentFileCacheManager getPathDocumentFileCacheManager() {
        return pathDocumentFileCacheManager;
    }

    public DocumentFile getFile(File rootDirectory, String currentWorkingDirectory, String data51) {
        DocumentFile result = null;

        String wholeDirecotoryPath = resolveWholeDirectoryPath(rootDirectory, currentWorkingDirectory, data51);
        File photoDirecotry = new File(wholeDirecotoryPath);

        if (virtualPathExists(wholeDirecotoryPath)) {
            Uri uri = getParentUriByVirtualPathMap(wholeDirecotoryPath);
            DocumentFile documentFile = getDocumentFileFromUri(context, uri);
            String parentVirtualPath = getParentVirtualPathByVirtualPathMap(wholeDirecotoryPath);
            String trailingPath = wholeDirecotoryPath.substring(parentVirtualPath.length(), wholeDirecotoryPath.length());
            String[] trialingPathSegments = trailingPath.split("/");
            DocumentFile targetdocumentFile = documentFile;
            String effectiveVirtualPathForCurrentSegment = parentVirtualPath;
            for (String currentSegmetn : trialingPathSegments) {
                if (currentSegmetn.isEmpty()) {

                } else {
                    if (targetdocumentFile != null) {
                        effectiveVirtualPathForCurrentSegment = effectiveVirtualPathForCurrentSegment + "/" + currentSegmetn;
                        effectiveVirtualPathForCurrentSegment = effectiveVirtualPathForCurrentSegment.replace("//", "/");
                        DocumentFile cachedtargetdocumentFile = pathDocumentFileCacheManager.get(effectiveVirtualPathForCurrentSegment);
                        if (cachedtargetdocumentFile != null) {
                            targetdocumentFile = cachedtargetdocumentFile;
                        } else {

                            targetdocumentFile = targetdocumentFile.findFile(currentSegmetn);
                            if (targetdocumentFile != null) {
                                pathDocumentFileCacheManager.put(effectiveVirtualPathForCurrentSegment, targetdocumentFile);
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
            result = targetdocumentFile;
        } else {
            if (photoDirecotry.exists()) {
            } else {
            }

            result = DocumentFile.fromFile(photoDirecotry);
        }

        if (result != null) {

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