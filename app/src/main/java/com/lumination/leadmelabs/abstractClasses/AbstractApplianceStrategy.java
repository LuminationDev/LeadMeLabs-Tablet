package com.lumination.leadmelabs.abstractClasses;

import android.view.View;

import androidx.databinding.ViewDataBinding;

import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;

public abstract class AbstractApplianceStrategy {
    public abstract <T extends ViewDataBinding> void trigger(T binding, Appliance appliance, View finalResult);
}
