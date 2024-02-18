package com.lumination.leadmelabs.models.applications;

import org.json.JSONObject;

public class ViveApplication extends Application {
    public ViveApplication(String type, String name, String id, boolean isVr, JSONObject subtype) {
        super(type, name, id, isVr, subtype);
    }

    public static String getImageUrl(String id) {
        return "";
    }
}
