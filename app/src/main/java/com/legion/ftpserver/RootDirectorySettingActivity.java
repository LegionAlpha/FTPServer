package com.legion.ftpserver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;

import com.legion.additional.PreferenceManagerUtil;
import com.legion.ftpserver.demo.Constants;
import com.legion.ftpserver.demo.R;
import com.legion.launcher.application.App;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class RootDirectorySettingActivity extends Activity {
    private static final String TAG = "RootDirectorySettingActivity";
    private static final int TIMEOUT = 30000;

    @Bind(R.id.paidCreditPrompttextView6)
    TextView paidCreditPrompttextView6;
    @Bind(R.id.paidCredittextView7)
    CheckBox paidCredittextView7;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                App app = App.getInstance();
                BuiltinFtpServer builtinFtpServer = null;
                builtinFtpServer = app.getBuiltinFtpServer();
                builtinFtpServer.mountVirtualPath("/", uri);
                queryRootDirectory();
            }
        }
    }

    @OnCheckedChanged(R.id.paidCredittextView7)
    public void toggleUseHiveLayout(boolean isChecked) {
        PreferenceManagerUtil.setExternalStoragePerformanceOptimize(isChecked);
        App app = App.getInstance();
        BuiltinFtpServer builtinFtpServer = null;
        builtinFtpServer = app.getBuiltinFtpServer();
        builtinFtpServer.setExternalStoragePerformanceOptimize(isChecked);
    }

    @OnClick(R.id.resetRootDirectoryss)
    public void resetRootDirectoryss() {
        App app = App.getInstance();
        BuiltinFtpServer builtinFtpServer = null;
        builtinFtpServer = app.getBuiltinFtpServer();
        builtinFtpServer.unmountVirtualPath("/");
        queryRootDirectory();
    }

    @OnClick(R.id.loginbutton)
    public void chooseRootDirectory() {
        StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        Intent intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
        File rootDirectory = Environment.getExternalStorageDirectory();
        Uri uriToLoad = Uri.fromFile(rootDirectory);
        int yourrequestcode = Constants.RequestCode.RootDirectoryPermissionRequestCode;
        startActivityForResult(intent, yourrequestcode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.root_directory_setting_activity);
        ButterKnife.bind(this);
        queryRootDirectory();
        showExternalStoragePerformanceOptimize();
    }


    private void showExternalStoragePerformanceOptimize() {
        boolean externalStoragePerformanceOPtimize = PreferenceManagerUtil.getExternalStoragePerformanceOptimize();
        paidCredittextView7.setChecked(externalStoragePerformanceOPtimize);
    }

    private void queryRootDirectory() {
        App app = App.getInstance();
        BuiltinFtpServer builtinFtpServer = null;
        builtinFtpServer = app.getBuiltinFtpServer();
        Uri uriForRootDirectory = builtinFtpServer.getVirtualPath("/");
        String uriStringRoot = null;
        if (uriForRootDirectory != null) {
        } else {
            File rootDirectory = Environment.getExternalStorageDirectory();
            Uri uriToLoad = Uri.fromFile(rootDirectory);
            uriForRootDirectory = uriToLoad;
        }
        uriStringRoot = uriForRootDirectory.toString();
        paidCreditPrompttextView6.setText(uriStringRoot);
    }
}
