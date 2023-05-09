package com.lumination.leadmelabs.managers;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.services.NetworkService;

import java.io.File;
import java.util.ArrayList;

public class ImageManager {
    /**
     * Keep a record of all images that have been requested from the NUC as to not double up on the
     * amount of transfers.
     */
    public static ArrayList<String> requestedImages = new ArrayList<>();

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
     * Send a socket message to the NUC requesting that and image associated with the supplied
     * experience name is sent over.
     * @param experienceName A string of the experience whose image is missing.
     */
    public static void requestImage(String experienceName) {
        requestedImages.add(experienceName);
        NetworkService.sendMessage("NUC", "ThumbnailRequest", experienceName);
    }

    /**
     * Load an image from the local storage, if it does not exist run the request image function
     * while returning an empty string.
     * @param experienceName A string of the image to load.
     * @return A string representing the absolute path of the image.
     */
    public static String loadLocalImage(String experienceName) {
        String filePath = MainActivity.getInstance().getApplicationContext().getFilesDir()+ "/" + experienceName + "_header.jpg";
        File image = new File(filePath);

        if(image.exists()) {
            return image.getPath();
        } else {
            if(!requestedImages.contains(experienceName)) {
                requestImage(experienceName);
            }

            return "";
        }
    }
}
