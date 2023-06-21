package com.legion.launcher.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.legion.ftpserver.demo.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ApplicationInformationActivity extends Activity {
    @Bind(R.id.launcher_activity)
    RelativeLayout launcher_activity;

    @Bind(R.id.wallpaper)
    ImageView wallpaper;

    @Bind(R.id.progressBar)
    ProgressBar progressBar;

    @Bind(R.id.statustextView)
    TextView statustextView;

    private static final String TAG = "ApplicationInformationB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.application_information_activity);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.lock0)
    public void toggleApplicationLock() {
        Log.d(TAG, "gotoFileManagerSettingsPage");
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String packageName = getPackageName();
        Log.d(TAG, "gotoFileManagerSettingsPage, package name: " + packageName);
        String url = "package:" + packageName;
        Log.d(TAG, "gotoFileManagerSettingsPage, url: " + url);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}
