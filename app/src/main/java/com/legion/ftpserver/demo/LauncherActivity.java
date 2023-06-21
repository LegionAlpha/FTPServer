package com.legion.ftpserver.demo;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import com.legion.additional.PreferenceManagerUtil;
import com.legion.ftplib.DocumentTreeBrowseRequest;
import com.legion.ftplib.EventListener;
import com.legion.ftpserver.BuiltinFtpServer;
import com.legion.launcher.Constants;
import com.legion.launcher.SettingsActivity;
import com.legion.launcher.activity.ApplicationInformationActivity;
import com.legion.launcher.application.App;
import com.legion.launcher.manager.ActiveUserReportManager;
import com.legion.launcher.service.DownloadNotificationService;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class LauncherActivity extends Activity {
    private static final String TAG = "LauncherActivity";
    private Timer timerObj = null;

    private ActiveUserReportManager activeUserReportManager = null;
    private BuiltinFtpServer builtinFtpServer = null;

    @Bind(R.id.statustextView)
    TextView statustextView;
    @Bind(R.id.availableSpaceView)
    TextView availableSpaceView;
    @Bind(R.id.allowAnonymousSet)
    CheckBox allowAnonymousetei;
    @Bind(R.id.userNamePassWordayout)
    RelativeLayout userNamePassWordayout;

    @OnClick(R.id.shareIcon)
    public void shareViaText() {
        Log.d(TAG, "gotoLoginActivity, 119.");
        Intent launchIntent = new Intent(this, SettingsActivity.class);
        startActivity(launchIntent);
        Log.d(TAG, "gotoLoginActivity, 122.");
    }

    @OnClick(R.id.copyUrlButton)
    public void copyUrlButton() {
        String stringNodeCopied = statustextView.getText().toString();
        ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = android.content.ClipData.newPlainText("Copied", stringNodeCopied);
        clipboard.setPrimaryClip(clip);
        String downloadFinished = getResources().getString(R.string.urlCopiedged);
        Log.d(TAG, "notifyDownloadFinish, text: " + downloadFinished);
    }

    private void showFtpUrl() {
        int actualPort = builtinFtpServer.getActualPort();
        String actualIp = builtinFtpServer.getIp();
        String ftpUrl = "ftp://" + actualIp + ":" + actualPort + "/";
        statustextView.setText(ftpUrl);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher_activity);
        ButterKnife.bind(this);
        App app = App.getInstance();
        builtinFtpServer = app.getBuiltinFtpServer();
        showFtpUrl();
        initializeEventListener();
        startTimeCheckService();
        loadSettings();
    }

    private void loadSettings() {
        boolean builtinShortcutsVisible = PreferenceManagerUtil.getAllowAnonymous();
        allowAnonymousetei.setChecked(builtinShortcutsVisible);
        toggleUserNamePassWordVisibility(builtinShortcutsVisible);
    }

    private void toggleUserNamePassWordVisibility(boolean isChecked) {
        if (isChecked) {
            userNamePassWordayout.setVisibility(View.INVISIBLE);
        } else {
            userNamePassWordayout.setVisibility(View.VISIBLE);
        }
    }

    @OnCheckedChanged(R.id.allowAnonymousSet)
    public void toggleAllowAnonymouse(boolean isChecked) {
        PreferenceManagerUtil.setAllowAnonymous(isChecked);
        builtinFtpServer.setAllowAnonymous(isChecked);
        toggleUserNamePassWordVisibility(isChecked);
    }

    private void startTimeCheckService() {
        Intent serviceIntent = new Intent(this, DownloadNotificationService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        long startTimestamp = System.currentTimeMillis();
        super.onResume();
        refreshAvailableSpace();
        createActiveUserReportManager();
    }

    private void createActiveUserReportManager() {
        if (activeUserReportManager == null) {
            activeUserReportManager = new ActiveUserReportManager();
            activeUserReportManager.startReportActiveUser();
        }
    }

    private void initializeEventListener() {
        EventListener eventListener = new FtpEventListener(this);
        builtinFtpServer.setEventListener(eventListener);
    }

    public void notifyDownloadStart() {
        cancelNotifyDownloadFinish();
    }

    private void cancelNotifyDownloadFinish() {
        if (timerObj != null) {
            timerObj.cancel();
        }
    }

    public void notifyIpChange() {
        showFtpUrl();
        String downloadFinished = getResources().getString(R.string.ipChanged);
        Log.d(TAG, "notifyDownloadFinish, text: " + downloadFinished);
    }

    public void notifyDownloadFinish() {
        Log.d(TAG, "notifyDownloadFinish");
        cancelNotifyDownloadFinish();
        timerObj = new Timer();
        TimerTask timerTaskObj = new TimerTask() {
            public void run() {
                String downloadFinished = getResources().getString(R.string.downloadFinished);
                Log.d(TAG, "notifyDownloadFinish, text: " + downloadFinished);
                Log.d(TAG, "notifyDownloadFinish, said: " + downloadFinished);
            }
        };
        timerObj.schedule(timerTaskObj, 18000);
    }

    private void scanFile(String path) {
        MediaScannerConnection.scanFile(this,
                new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }

    public void guideExternalStorageManagerPermission(Object eventContent) {
        Log.d(TAG, "gotoLoginActivity, 119.");
        Intent launchIntent = new Intent(this, ApplicationInformationActivity.class);
        startActivity(launchIntent);
        Log.d(TAG, "gotoLoginActivity, 122.");
    }

    public void browseDocumentTree(Object eventContent) {
        DocumentTreeBrowseRequest requestObject = (DocumentTreeBrowseRequest) (eventContent);
        Intent intent = requestObject.getIntent();
        int yourrequestcode = requestObject.getRequestCode();
        startActivityForResult(intent, yourrequestcode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                builtinFtpServer.answerBrowseDocumentTreeReqeust(requestCode, uri);
            }
        }
    }

    public void notifyDelete(Object eventContent) {
        scanDocumentFile(eventContent);
    }

    private void scanDocumentFile(Object eventContent) {
        DocumentFile uploadedFile = (DocumentFile) (eventContent);
        Uri uri = uploadedFile.getUri();
        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            String path = uri.getPath();
            File rawFile = new File(path);
            requestScanFile(rawFile);
        }
    }

    public void notifyUploadFinish(Object eventContent) {
        scanDocumentFile(eventContent);
    }

    private void requestScanFile(File uploadedFile) {
        scanFile(uploadedFile.getAbsolutePath());
    }

    public void refreshAvailableSpace() {
        File file = new File(Constants.DirPath.FARMING_BOOK_APP_SD_CARD_PATH);
        long usableSpaceBytes = file.getUsableSpace();
        double usableSpaceMiB = ((double) (usableSpaceBytes)) / 1024.0 / 1024.0;
        double roundedSpaceMiB = Math.round(usableSpaceMiB * 10) / 10.0;
        availableSpaceView.setText("" + roundedSpaceMiB + "MB");
    }
}
