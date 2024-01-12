package com.lumination.leadmelabs.models.applications;

import com.lumination.leadmelabs.managers.ImageManager;

public class CustomApplication extends Application {
    public CustomApplication(String type, String name, String id, boolean isVr) {
        super(type, name, id, isVr);
    }

    public static String getImageUrl(String fileName) {
        return ImageManager.loadLocalImage(fileName);
    }
}
