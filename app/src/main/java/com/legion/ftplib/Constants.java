package com.legion.ftplib;

import android.os.Environment;

public class Constants {
    public static class ErrorCode {
        public static final Integer ADDRESS_ALREADY_IN_USE = 182735;
        public static final Integer ControlConnectionEndedUnexpectedly = 95731;
    }

    public static class FilePath {
        public static final String AndroidData = Environment.getExternalStorageDirectory().getPath() + "/Android/data/";
        public static final String ExternalRoot = Environment.getExternalStorageDirectory().getPath();
    }

    public static class RequestCode {
        public static final Integer AndroidDataPermissionRequestCode = 100345;
    }

    public static class Permission {
        public static final Integer Write = 194238;
        public static final Integer Read = 202152;
    }
}

