package com.lumination.leadmelabs.models.applications;

import com.lumination.leadmelabs.managers.ImageManager;

public class ReviveApplication extends Application {
    public ReviveApplication(String type, String name, String id) {
        super(type, name, id);
    }

    public static String getImageUrl(String fileName) {
        return ImageManager.loadLocalImage(fileName);
    }
}
