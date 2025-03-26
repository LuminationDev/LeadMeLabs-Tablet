package com.lumination.leadmelabs.ui.appliance.Strategies;

import android.view.View;

import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.abstractClasses.AbstractApplianceStrategy;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.controllers.ApplianceController;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.segment.analytics.Properties;

import java.util.HashMap;

/*
 * Toggle strategy that determines custom values for on and off.
 */
public class ToggleStrategy extends AbstractApplianceStrategy {
    @Override
    public <T extends ViewDataBinding> void trigger(T binding, Appliance appliance, View finalResult) {
        CardApplianceBinding cardBinding = (CardApplianceBinding) binding;
        String value;

        if(ApplianceViewModel.activeApplianceList.containsKey(appliance.id)) {
            cardBinding.setStatus(new MutableLiveData<>(Constants.INACTIVE));
            ApplianceController.applianceTransition(finalResult, R.drawable.transition_appliance_blue_to_grey);
            value = "0";
            if (appliance.type.equals(Constants.SPLICERS)) {
                value = Constants.SPLICER_MIRROR;
            }
            ApplianceViewModel.activeApplianceList.remove(appliance.id);
        } else {
            cardBinding.setStatus(new MutableLiveData<>(Constants.ACTIVE));
            ApplianceController.applianceTransition(finalResult, R.drawable.transition_appliance_grey_to_blue);
            value = Constants.APPLIANCE_ON_VALUE;
            if (appliance.type.equals(Constants.LED)) {
                value = Constants.LED_ON_VALUE;
            }
            if (appliance.type.equals(Constants.SPLICERS)) {
                value = Constants.SPLICER_STRETCH;
            }
            ApplianceViewModel.activeApplianceList.put(appliance.id, value);
        }

        //additionalData break down
        //Action : [cbus unit : group address : id address : value] : [type : room : id appliance]
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"                         //[0] Action
                        + appliance.id + ":"                    //[1] CBUS unit number
                        + value + ":"                           //[2] CBUS object id/doubles as card id
                        + NetworkService.getIPAddress());       //[3] The IP address of the tablet

        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
            put("appliance_type", appliance.type);
            put("appliance_room", appliance.room);
            put("appliance_new_value", appliance.value);
            put("appliance_action_type", "toggle");
        }};
        FirebaseManager.logAnalyticEvent("appliance_value_changed", analyticsAttributes);

        Properties properties = new Properties();
        properties.put("classification", "Appliance");
        properties.put("applianceType", appliance.type);
        properties.put("applianceRoom", appliance.room);
        properties.put("applianceCurrentValue", appliance.value);
        properties.put("applianceNewValue", value);
        properties.put("applianceActionType", "toggle");

        Segment.trackEvent(SegmentConstants.Appliance_Triggered, properties);
    }
}
