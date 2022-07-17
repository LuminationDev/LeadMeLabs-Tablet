package com.lumination.leadmelabs.abstractClasses;

import android.view.View;

import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;

public abstract class AbstractApplianceStrategy {
    public abstract void trigger(CardApplianceBinding binding, Appliance appliance, View finalResult);
}
