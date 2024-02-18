package com.lumination.leadmelabs.models.applications;

import com.lumination.leadmelabs.managers.ImageManager;

import org.json.JSONObject;

public class SteamApplication extends Application {
    public SteamApplication(String type, String name, String id, boolean isVr, JSONObject subtype) {
        super(type, name, id, isVr, subtype);
    }

    /**
     * Check for a local file before trying to find it online.
     */
    public static String getImageUrl(String fileName, String id) {
        String internalPath = ImageManager.loadLocalImage(fileName);

        if(!internalPath.equals("")) {
            return internalPath;
        } else {
            return "https://cdn.cloudflare.steamstatic.com/steam/apps/" + id + "/header.jpg";
        }
    }
}
