package com.legion.ftplib;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.upokecenter.cbor.CBORObject;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VirtualPathMapSaveTask extends AsyncTask<Object, Void, Boolean> {
    private Context context = null;
    private static final String TAG = "VirtualPathMapSaveTask";

    @Override
    protected Boolean doInBackground(Object... params) {
        Boolean result = false;

        HashMap<String, Uri> voicePackageNameMap = (HashMap<String, Uri>) (params[0]);
        context = (Context) (params[1]);

        boolean addPhotoFile = false;

        Log.d(TAG, "1129, saveVoicePackageNameMap, answer value: ");

        byte[] serializedContent = constructVirtualPathMapMessageCbor(voicePackageNameMap);

        Log.d(TAG, "1134, saveVoicePackageNameMap, answer value: content length: " + serializedContent.length);

        File photoFile = findVoicePackageMapFile();

        Log.d(TAG, "143, saveVoicePackageNameMap, file path: " + photoFile.getAbsolutePath());

        try {
            FileUtils.writeByteArrayToFile(photoFile, serializedContent);

            Log.d(TAG, "149, saveVoicePackageNameMap, file saved, length: " + photoFile.length());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "1144, saveVoicePackageNameMap, answer value: ");

        return result;
    }

    private byte[] constructVirtualPathMapMessageCbor(HashMap<String, Uri> subject) {
        VirtualPathMapData translateRequestBuilder = new VirtualPathMapData();

        List<VirtualPathMapItem> virtualPathMapList = new ArrayList<>();

        List<String> virtualPathList = new ArrayList<>(subject.keySet());

        for (String currentVirtualPath : virtualPathList) {
            VirtualPathMapItem currentVirtualPathMapItem = new VirtualPathMapItem();

            Uri curentUri = subject.get(currentVirtualPath);

            currentVirtualPathMapItem.setVirtualPath(currentVirtualPath);
            currentVirtualPathMapItem.setUri(curentUri.toString());

            virtualPathMapList.add(currentVirtualPathMapItem);
        }

        translateRequestBuilder.setVoicePackageMapJsonItemList(virtualPathMapList);

        CBORObject cborObject = CBORObject.FromObject(translateRequestBuilder);

        byte[] array = cborObject.EncodeToBytes();

        String arrayString = new String(array);

        return array;
    }

    private File findVoicePackageMapFile() {
        File result = null;

        File filesDir = context.getFilesDir();

        Log.d(TAG, "1459, findRandomPhotoFile, files dir: " + filesDir);

        if (filesDir == null) {
        } else {
            result = new File(filesDir.getAbsolutePath() + "/voicePackageNameMap.proto");

            Log.d(TAG, "1469, findRandomPhotoFile, files exists: " + result.exists() + ", size: " + result.length());

            if (result.exists()) {
            } else {
                try {
                    boolean createResult = result.createNewFile();

                    Log.d(TAG, "findRandomPhotoFile, create file result: " + createResult);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
    }
}