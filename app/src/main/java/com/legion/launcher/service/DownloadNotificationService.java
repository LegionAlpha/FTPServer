package com.legion.launcher.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.legion.ftpserver.BuiltinFtpServer;
import com.legion.ftpserver.demo.LauncherActivity;
import com.legion.ftpserver.demo.R;
import com.legion.launcher.application.App;

public class DownloadNotificationService extends Service {
    private Notification continiusNotification = null;
    private BuiltinFtpServer builtinFtpServer = null;

    private int NOTIFICATION = 163731;

    private NotificationManager mNM;

    private long lastPublishTimestamp = 0;
    private String callbackIp = "127.0.0.1";

    private static final String TAG = "DownloadNotificationService";
    private static final String LanServiceName = "com.stupidbeauty.shutdownat2100.android";
    private static final int LanServicePort = 9521;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand,180");
        String contentText = getString(R.string.app_name);
        showNotification(contentText);
        startForeground(NOTIFICATION, continiusNotification);
        App app = App.getInstance();
        builtinFtpServer = app.getBuiltinFtpServer();
        return START_STICKY;
    }

    private void showNotification(String contentText) {
        CharSequence text = getText(R.string.app_name);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, LauncherActivity.class), 0);

        String downloadingText = "Running " + contentText;

        NotificationChannel chan = new NotificationChannel("#include", "My Foreground Service", NotificationManager.IMPORTANCE_LOW);

        mNM.createNotificationChannel(chan);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.app_name))
                .setContentText(downloadingText)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setChannelId("#include")
                .build();

        continiusNotification = notification;
        mNM.notify(NOTIFICATION, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startHttpServer();
    }

    private void startHttpServer() {
        AsyncHttpServer server = new AsyncHttpServer();

        HttpServerRequestCallback callback = new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("Hello!!!");
            }
        };
        server.get("/", callback);
    }
}
