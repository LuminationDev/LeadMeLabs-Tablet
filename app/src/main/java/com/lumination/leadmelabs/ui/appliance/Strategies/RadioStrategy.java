package com.lumination.leadmelabs.ui.appliance.Strategies;

import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;

import java.util.HashMap;

public class RadioStrategy {
    public void trigger(Appliance appliance, String value) {
        ApplianceViewModel.activeApplianceList.put(appliance.id, value);

        //additionalData break down
        //Action : [cbus unit : group address : id address : value] : [type : room : id appliance]
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"
                        + appliance.id + ":"
                        + value + ":"
                        + NetworkService.getIPAddress());

        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
            put("appliance_type", appliance.type);
            put("appliance_room", appliance.room);
            put("appliance_new_value", appliance.value);
            put("appliance_action_type", "toggle");
        }};
        FirebaseManager.logAnalyticEvent("appliance_value_changed", analyticsAttributes);
    }
}
