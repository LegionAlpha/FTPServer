package com.legion.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.legion.ftpserver.RootDirectorySettingActivity;
import com.legion.ftpserver.demo.R;
import com.legion.launcher.activity.ApplicationInformationActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends Activity {
    private static String OptimizeRepairGooglePlayUrl = "https://play.google.com/store/apps/details?id=com.stupidbeauty.hxlauncher";

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);

        ButterKnife.bind(this);
    }

    private void openURL(String url) {
        if (url.startsWith("HTTP://")) {
            url = "http" + url.substring(4);
        } else if (url.startsWith("HTTPS://")) {
            url = "https" + url.substring(5);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            launchIntent(intent);
        } catch (ActivityNotFoundException ignored) {
            Log.w(TAG, "Nothing available to handle " + intent);
        }
    }

    private void launchIntent(Intent intent) {
        try {
            rawLaunchIntent(intent);
        } catch (ActivityNotFoundException ignored) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name);
            builder.setMessage(R.string.msg_intent_failed);
            builder.setPositiveButton(R.string.button_ok, null);
            builder.show();
        }
    }

    private void rawLaunchIntent(Intent intent) {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            Log.d(TAG, "Launching intent: " + intent + " with extras: " + intent.getExtras());
            startActivity(intent);
        }
    }

    @OnClick(R.id.lanime_button1)
    public void gotoAutoRunSettingsActivity() {
        Intent launchIntent = new Intent(this, ApplicationInformationActivity.class);
        startActivity(launchIntent);
    }

    @OnClick(R.id.myAccountbutton1)
    public void gotoAccountActivity() {
        Intent launchIntent = new Intent(this, RootDirectorySettingActivity.class);
        startActivity(launchIntent);
    }
}