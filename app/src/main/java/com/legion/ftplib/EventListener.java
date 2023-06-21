package com.legion.ftplib;

public interface EventListener {
    public static final String DELETE = "com.stupidbeauty.ftpserver.lib.delete";
    public static final String DOWNLOAD_FINISH = "com.stupidbeauty.ftpserver.lib.download_finish";
    public static final String UPLOAD_FINISH = "com.stupidbeauty.ftpserver.lib.upload_finish";
    public static final String DOWNLOAD_START = "com.stupidbeauty.ftpserver.lib.download_start";
    public static final String IP_CHANGE = "com.stupidbeauty.ftpserver.lib.ip_change";
    public static final String NEED_BROWSE_DOCUMENT_TREE = "com.stupidbeauty.ftpserver.lib.need_browse_document_tree";
    public static final String NEED_EXTERNAL_STORAGE_MANAGER_PERMISSION = "com.stupidbeauty.ftpserver.lib.need_external_storage_manager_permission";

    @Deprecated
    public void onEvent(String eventCode);

    public void onEvent(String eventCode, Object extraContent);
}

