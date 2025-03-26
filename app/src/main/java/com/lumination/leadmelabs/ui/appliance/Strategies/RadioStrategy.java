package com.lumination.leadmelabs.ui.appliance.Strategies;

import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.segment.analytics.Properties;

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

        Properties properties = new Properties();
        properties.put("classification", "Appliance");
        properties.put("applianceType", appliance.type);
        properties.put("applianceRoom", appliance.room);
        properties.put("applianceCurrentValue", appliance.value);
        properties.put("applianceNewValue", value);
        properties.put("applianceActionType", "radio");

        Segment.trackEvent(SegmentConstants.Appliance_Triggered, properties);
    }
}
