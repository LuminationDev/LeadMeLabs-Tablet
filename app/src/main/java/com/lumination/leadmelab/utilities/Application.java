package com.lumination.leadmelab.utilities;

import android.graphics.Bitmap;

public class Application {
    private final static String TAG = "Application";

    private final String name;
    private final String ID;
    private Bitmap picture;
    private boolean isSelected;

    public Application(String name, String ID) {
        this.name = name;
        this.ID = ID;
    }

    public String getName() {
        return this.name;
    }

    public String getID() {
        return this.ID;
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
