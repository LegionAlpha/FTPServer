package com.legion.launcher.manager;

import android.content.Context;

import com.legion.launcher.application.App;
import com.stupidbeauty.rotatingactiveuser.RotatingActiveUserClient;

public class ActiveUserReportManager {
    private RotatingActiveUserClient rotatingActiveUserClient = null;

    public void startReportActiveUser() {
        if (rotatingActiveUserClient == null) {
            Context context = App.getAppContext();
            rotatingActiveUserClient = new RotatingActiveUserClient(context);
        }

        rotatingActiveUserClient.reportActiveUser();
    }
}
