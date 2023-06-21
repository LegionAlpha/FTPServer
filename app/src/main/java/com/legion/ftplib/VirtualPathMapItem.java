package com.legion.ftplib;

public class VirtualPathMapItem {
    private String uri;
    private String virtualPath;

    public String getVirtualPath() {
        return virtualPath;
    }

    public String getUri() {
        return uri;
    }

    public void setVirtualPath(String virtualPath) {
        this.virtualPath = virtualPath;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
