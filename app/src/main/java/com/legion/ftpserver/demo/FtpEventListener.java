package com.legion.ftpserver.demo;

import android.util.Log;

import com.legion.ftplib.EventListener;

public class FtpEventListener implements EventListener {
    private static final String TAG = "FtpEventListener";
    private LauncherActivity launcherActivity = null;

    @Override
    public void onEvent(String eventCode) {
    }

    @Override
    public void onEvent(String eventCode, Object eventContent) {
        Log.d(TAG, "onEvent, eventCode: " + eventCode);

        if (eventCode.equals(DELETE)) {
            launcherActivity.refreshAvailableSpace();

            if (eventContent != null) {
                launcherActivity.notifyDelete(eventContent);
            }
        } else if (eventCode.equals(DOWNLOAD_FINISH)) {
            launcherActivity.notifyDownloadFinish();
        } else if (eventCode.equals(UPLOAD_FINISH)) {
            if (eventContent != null) {
                launcherActivity.notifyUploadFinish(eventContent);
            }
        } else if (eventCode.equals(NEED_BROWSE_DOCUMENT_TREE)) {
            launcherActivity.browseDocumentTree(eventContent);
        } else if (eventCode.equals(NEED_EXTERNAL_STORAGE_MANAGER_PERMISSION)) {
            launcherActivity.guideExternalStorageManagerPermission(eventContent);
        } else if (eventCode.equals(DOWNLOAD_START)) {
            launcherActivity.notifyDownloadStart();
        } else if (eventCode.equals(IP_CHANGE)) {
            launcherActivity.notifyIpChange();
        }
    }

    public FtpEventListener(LauncherActivity launcherActivity) {
        this.launcherActivity = launcherActivity;
    }
}