package com.legion.ftplib;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.upokecenter.cbor.CBORException;
import com.upokecenter.cbor.CBORObject;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class LoadVirtualPathMapTask extends AsyncTask<Object, Void, Object> {
    private HashMap<String, Uri> voicePackageNameMap;
    private Context context = null;

    private static final String TAG = "LoadVirtualPathMapTask";

    private VirtualPathLoadInterface launcherActivity = null;

    private File findVoicePackageMapFile() {
        File result = null;

        File filesDir = context.getFilesDir();

        if (filesDir == null) {
        } else {
            result = new File(filesDir.getAbsolutePath() + "/voicePackageNameMap.proto");

            if (result.exists()) {
            } else {
                try {
                    result.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private void loadVoicePackageNameMap() {
        File photoFile = findVoicePackageMapFile();

        voicePackageNameMap = new HashMap<>();

        if (photoFile != null) {
            if (photoFile.exists()) {
                try {
                    byte[] photoBytes = FileUtils.readFileToByteArray(photoFile);

                    CBORObject videoStreamMessage = CBORObject.DecodeFromBytes(photoBytes);
                    String jsonString = videoStreamMessage.ToJSONString();

                    Collection<CBORObject> subFilesList = videoStreamMessage.get("voicePackageMapJsonItemList").getValues();

                    for (CBORObject currentSubFile : subFilesList) {
                        jsonString = currentSubFile.ToJSONString();
                        CBORObject virtualPathObject = currentSubFile.get("virtualPath");

                        if (virtualPathObject != null) {
                            String currentRelationshipgetVoiceRecognizeResult = virtualPathObject.AsString();
                            String uriString = currentSubFile.get("uri").AsString();
                            Uri currentPackageItemInfo = Uri.parse(uriString);
                            voicePackageNameMap.put(currentRelationshipgetVoiceRecognizeResult, currentPackageItemInfo);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (CBORException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected Object doInBackground(Object... params) {
        Boolean result = false;
        launcherActivity = (VirtualPathLoadInterface) (params[0]);
        context = (Context) (params[1]);
        loadVoicePackageNameMap();
        boolean addPhotoFile = false;
        return voicePackageNameMap;
    }

    @Override
    protected void onPostExecute(Object result) {
        launcherActivity.setVoicePackageNameMap(voicePackageNameMap);
    }
}