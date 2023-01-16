package com.lumination.leadmelabs.ui.appliance.Strategies;

import android.view.View;

import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.appliance.ExtendedApplianceCard;
import com.lumination.leadmelabs.utilities.Constants;

/**
 * Show the expanded blind element for the currently selected blind.
 * Create a transparent background layout that covers the entire page then add the cardView
 * onto that. Create an on touch listener on the background page to remove both the cardView
 * and background view when not clicking the cardView.
 */
public class BlindStrategy extends ExtendedApplianceCard {
    //Values for if the card is a scene
    private final String openValue = "2";
    private final String closeValue = "0";
    private final String stopValue = "1";

    public BlindStrategy(boolean isSceneCard) {
        super(isSceneCard);
    }

    public void trigger(CardApplianceBinding binding, Appliance appliance, View finalResult) {
        super.trigger(binding, appliance, finalResult);

        super.setupButton(0, super.isSceneCard ? openValue : Constants.APPLIANCE_ON_VALUE, appliance.description.get(0));
        super.setupButton(1, super.isSceneCard ? closeValue : Constants.APPLIANCE_OFF_VALUE, appliance.description.get(1));
        super.setupButton(2, super.isSceneCard ? stopValue : Constants.BLIND_STOPPED_VALUE, appliance.description.get(2));
    }
}
