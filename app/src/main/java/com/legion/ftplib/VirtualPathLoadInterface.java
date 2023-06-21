package com.legion.ftplib;

import android.net.Uri;
import android.os.PowerManager;

import java.util.HashMap;

public interface VirtualPathLoadInterface {
    PowerManager.WakeLock wakeLock = null;

    int ret = 0;

    public void setVoicePackageNameMap(HashMap<String, Uri> voicePackageNameMap);
}
