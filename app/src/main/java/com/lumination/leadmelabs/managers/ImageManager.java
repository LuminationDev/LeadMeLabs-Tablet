package com.lumination.leadmelabs.managers;

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
    public static void CheckLocalCache(String list) {
        String[] apps = list.split("/");

        for (String app: apps) {
            String[] appData = app.split("\\|");

            //Currently only tracking the Custom images
            if (appData.length > 1 && appData[0].equals("Custom")) {
                loadLocalImage(appData[2]);
            }
        }
    }

    /**
     * Check the local application directory to see if any experiences require their thumbnails
     * sent over from the NUC.
     * @param jsonArray A JSON array of experiences that has been received from the NUC.
     */
    public static void CheckLocalCache(JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            // Get the JSONObject at the current index
            JSONObject entry = jsonArray.getJSONObject(i);

            String appType = entry.getString("WrapperType");
            String appName = entry.getString("Name");

            //Currently only tracking the Custom images
            if (appType.equals("Custom")) {
                loadLocalImage(appName);
            }
        }
    }

    /**
     * Send a socket message to the NUC requesting that and image associated with the supplied
     * experience name is sent over.
     * @param experienceName A string of the experience whose image is missing.
     */
    public static void requestImage(String experienceName) {
        requestedImages.add(experienceName.replace(":", ""));
        NetworkService.sendMessage("NUC", "ThumbnailRequest", experienceName);
    }

    /**
     * Load an image from the local storage, if it does not exist run the request image function
     * while returning an empty string.
     * @param experienceName A string of the image to load.
     * @return A string representing the absolute path of the image.
     */
    public static String loadLocalImage(String experienceName) {
        String filePath = MainActivity.getInstance().getApplicationContext().getFilesDir()+ "/" + experienceName.replace(":", "") + "_header.jpg";
        File image = new File(filePath);

        if(image.exists()) {
            return image.getPath();
        } else {
            if(!requestedImages.contains(experienceName.replace(":", ""))) {
                requestImage(experienceName);
            }

            return "";
        }
    }
}
