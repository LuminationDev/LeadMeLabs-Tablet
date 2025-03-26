package com.lumination.leadmelabs.models.applications;

import com.lumination.leadmelabs.managers.ImageManager;

import org.json.JSONObject;

public class ReviveApplication extends Application {
    public ReviveApplication(String type, String name, String id, boolean isVr, JSONObject subtype) {
        super(type, name, id, isVr, subtype);
    }

    public static String getImageUrl(String fileName) {
        return ImageManager.loadLocalImage(fileName, "experience");
    }
}
