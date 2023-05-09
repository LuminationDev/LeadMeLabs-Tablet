package com.lumination.leadmelabs.ui.appliance.Strategies;

import android.view.View;

import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.appliance.ExtendedApplianceCard;
import com.lumination.leadmelabs.utilities.Constants;

/**
 * Create and show an expanded card element for the currently selected appliance.
 * Create a transparent background layout that covers the entire page then add the Card
 * onto that. Create an on touch listener on the background page to remove both the Card
 * and background view when not clicking the Card.
 */
public class HDMIStrategy extends ExtendedApplianceCard {
    public HDMIStrategy(boolean isSceneCard) {
        super(isSceneCard);
    }

    public void trigger(CardApplianceBinding binding, Appliance appliance, View finalResult) {
        super.trigger(binding, appliance, finalResult);
        if (appliance.options != null) {
            for (int i = 0; i < appliance.options.size(); i++) {
                super.setupButton(i, appliance.options.get(i).id, appliance.options.get(i).name);
            }
        } else {
            super.setupButton(0, Constants.SOURCE_HDMI_1, appliance.description.get(0));
            super.setupButton(1, Constants.SOURCE_HDMI_2, appliance.description.get(1));
            super.setupButton(2, Constants.SOURCE_HDMI_3, appliance.description.get(2));
        }
    }
}

