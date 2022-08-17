package com.lumination.leadmelabs.ui.appliance.Strategies;

import android.view.View;

import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.abstractClasses.AbstractApplianceStrategy;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceController;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.utilities.Constants;

/*
 * Toggle strategy that determines custom values for on and off.
 */
public class ToggleStrategy  extends AbstractApplianceStrategy {
    @Override
    public void trigger(CardApplianceBinding binding, Appliance appliance, View finalResult) {
        String type = "appliance";
        String value;

        if(ApplianceViewModel.activeApplianceList.containsKey(appliance.id)) {
            binding.setStatus(new MutableLiveData<>(Constants.INACTIVE));
            ApplianceController.applianceTransition(finalResult, R.drawable.transition_appliance_fade_active);
            value = "0";
            ApplianceViewModel.activeApplianceList.remove(appliance.id);
        } else {
            binding.setStatus(new MutableLiveData<>(Constants.ACTIVE));
            ApplianceController.applianceTransition(finalResult, R.drawable.transition_appliance_fade);
            value = appliance.type.equals(Constants.LED) ? Constants.LED_ON_VALUE : Constants.APPLIANCE_ON_VALUE; //TODO this needs to be scalable
            ApplianceViewModel.activeApplianceList.put(appliance.id, value);
        }

        //additionalData break down
        //Action : [cbus unit : group address : id address : value] : [type : room : id appliance]
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"                         //[0] Action
                        + appliance.automationBase + ":"        //[1] CBUS unit number
                        + appliance.automationGroup + ":"       //[2] CBUS group address
                        + appliance.automationId  + ":"         //[3] CBUS unit address
                        + value + ":"                           //[4] New value for address
                        + type + ":"                            //[5] Object type (computer, appliance, scene)
                        + appliance.room + ":"                  //[6] Appliance room
                        + appliance.id + ":"                    //[7] CBUS object id/doubles as card id
                        + NetworkService.getIPAddress());       //[8] The IP address of the tablet
    }
}
