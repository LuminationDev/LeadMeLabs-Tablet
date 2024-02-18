package com.lumination.leadmelabs.models.applications;

import com.lumination.leadmelabs.managers.ImageManager;

import org.json.JSONObject;

public class CustomApplication extends Application {
    public CustomApplication(String type, String name, String id, boolean isVr, JSONObject subtype) {
        super(type, name, id, isVr, subtype);
    }

    public static String getImageUrl(String fileName) {
        return ImageManager.loadLocalImage(fileName);
    }
}
