package com.lumination.leadmelabs.managers;

import android.util.Log;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.services.NetworkService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class ImageManager {
    /**
     * Keep a record of all images that have been requested from the NUC as to not double up on the
     * amount of transfers.
     */
    public static ArrayList<String> requestedImages = new ArrayList<>();

    //BACKWARDS COMPATIBILITY
    /**
     * Check the local application directory to see if any experiences require their thumbnails
     * sent over from the NUC.
     * @param list A string of experiences that has been received from the NUC.
     */
    public static void CheckLocalExperienceCache(String list) {
        String[] apps = list.split("/");

        for (String app: apps) {
            String[] appData = app.split("\\|");

            //Currently only tracking the Custom images
            if (appData.length > 1 && appData[0].equals("Custom")) {
                loadLocalImage(appData[2], "experience");
            }
        }
    }

    /**
     * Check the local directory to see if any experiences require their thumbnails
     * sent over from the NUC.
     * @param jsonArray A JSON array of experiences that has been received from the NUC.
     */
    public static void CheckLocalExperienceCache(JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            // Get the JSONObject at the current index
            JSONObject entry = jsonArray.getJSONObject(i);

            String appType = entry.getString("WrapperType");
            String appName = entry.getString("Name");

            //Currently only tracking the Custom images
            if (appType.equals("Custom")) {
                loadLocalImage(appName, "experience");
            }
        }
    }

    /**
     * Check the local directory to see if any videos require their thumbnails
     * sent over from the NUC.
     * @param jsonArray A JSON array of videos that has been received from the NUC.
     */
    public static void CheckLocalVideoCache(JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            // Get the JSONObject at the current index
            JSONObject entry = jsonArray.getJSONObject(i);

            String id = entry.optString("id", "");

            //Currently only tracking the Custom images
            loadLocalImage(id, "video");
        }
    }

    /**
     * Send a socket message to the NUC requesting that and image associated with the supplied
     * experience name is sent over.
     * @param uniqueId A string of the experience whose image is missing.
     * @param type A string of the model the image relates to.
     */
    private static void requestImage(String uniqueId, String type) {
        switch (type) {
            case "experience":
                requestedImages.add(uniqueId.replace(":", ""));
                break;

            case "video":
                requestedImages.add(uniqueId);
                break;

            default:
                break;
        }

        NetworkService.sendMessage("NUC", "ThumbnailRequest", uniqueId);
    }

    /**
     * Load an image from the local storage, if it does not exist run the request image function
     * while returning an empty string.
     * @param uniqueId A string of the image to load.
     * @param type A string of the model the image relates to.
     * @return A string representing the absolute path of the image.
     */
    public static String loadLocalImage(String uniqueId, String type) {
        String filePath;
        switch (type) {
            case "experience":
                filePath = MainActivity.getInstance().getApplicationContext().getFilesDir() + "/" + uniqueId.replace(":", "") + "_header.jpg";
                break;

            case "video":
                filePath = MainActivity.getInstance().getApplicationContext().getFilesDir() + "/" + uniqueId + "_thumbnail.jpg";
                break;

            default:
                return "";
        }

        File image = new File(filePath);

        if(image.exists()) {
            return image.getPath();
        } else {
            if(!requestedImages.contains(uniqueId.replace(":", ""))) {
                requestImage(uniqueId, type);
            }

            return "";
        }
    }
}
