package com.legion.ftplib;

import java.util.ArrayList;
import java.util.List;

public class VirtualPathMapData {
    private List<VirtualPathMapItem> voicePackageMapJsonItemList = new ArrayList<>();

    public List<VirtualPathMapItem> getVoicePackageMapJsonItemList() {
        return voicePackageMapJsonItemList;
    }

    public void setVoicePackageMapJsonItemList(List<VirtualPathMapItem> listToSet) {
        voicePackageMapJsonItemList = listToSet;
    }
}