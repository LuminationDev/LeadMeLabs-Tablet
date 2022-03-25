package com.lumination.leadmelabs.models;

import android.graphics.Bitmap;

public class Application {
    private final static String TAG = "Application";

    private final String name;
    private final String AppID;
    private Bitmap picture;
    private boolean isSelected;

    public Application(String name, String ID) {
        this.name = name;
        this.AppID = ID;
    }

    public String getName() {
        return this.name;
    }

    public String getAppID() {
        return this.AppID;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    public Bitmap getPicture() {
        return this.picture;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public boolean isSelected() {
        return this.isSelected;
    }
}
