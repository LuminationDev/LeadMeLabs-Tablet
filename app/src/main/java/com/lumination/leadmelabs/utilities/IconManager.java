package com.lumination.leadmelabs.utilities;

import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;

/**
 * Purpose: Manage the various iconAnimators associated to Station data binding that are created
 * and destroyed by fragments. Holds a HashMap that is overridden by new fragments to only manage
 * the currently active (live) image views.
 */
public class IconManager {
    private final Map<String, IconAnimator> iconAnimators = new HashMap<>();

    public void addIconAnimator(String iconName, IconAnimator animator) {
        iconAnimators.put(iconName, animator);
    }

    public ImageView getIconAnimator(String iconName) {
        IconAnimator temp = iconAnimators.get(iconName);
        if(temp != null) {
            return temp.getImageView();
        }
        return null;
    }

    public void startFlashing(String iconName) {
        IconAnimator temp = iconAnimators.get(iconName);
        if(temp != null) {
            temp.startFlashing();
        }
    }

    public void stopFlashing(String iconName) {
        IconAnimator temp = iconAnimators.get(iconName);
        if(temp != null) {
            temp.stopFlashing();
            //Remove the entry
            iconAnimators.remove(iconName);
        }
    }
}
