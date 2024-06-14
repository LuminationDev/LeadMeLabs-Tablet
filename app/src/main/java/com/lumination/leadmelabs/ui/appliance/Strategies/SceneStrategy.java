package com.lumination.leadmelabs.ui.appliance.Strategies;

import android.util.Log;
import android.view.View;

import androidx.databinding.ViewDataBinding;

import com.lumination.leadmelabs.abstractClasses.AbstractApplianceStrategy;
import com.lumination.leadmelabs.databinding.CardApplianceSceneBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.controllers.SceneController;
import com.lumination.leadmelabs.ui.appliance.adapters.ApplianceAdapter;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceParentAdapter;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.room.RoomFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

/**
 * A scene can be re-triggered if active, as sub elements can be changed while a scene is
 * active. If nothing has changed then the Cbus disregards the message.
 */
public class SceneStrategy extends AbstractApplianceStrategy {
    @Override
    public <T extends ViewDataBinding > void trigger(T binding, Appliance appliance, View finalResult) {
        CardApplianceSceneBinding cardBinding = (CardApplianceSceneBinding) binding;

        ArrayList<JSONObject> stationsToTurnOff = new ArrayList<>();

        if (appliance.stations != null && appliance.stations.length() > 0) {
            for (int i = 0; i < appliance.stations.length(); i++) {
                try {
                    JSONObject station = appliance.stations.getJSONObject(i);
                    if (!station.has("action")) {
                        continue;
                    }
                    if (station.getString("action").equals("Off")) {
                        stationsToTurnOff.add(station);
                    }
                } catch (JSONException e) {
                    Log.e("SceneStrategy", e.toString());
                }
            }

            String stationIdsString = String.join(", ", stationsToTurnOff.stream().map(station -> {
                try {
                    return station.getString("id");
                } catch (JSONException e) {
                    Log.e("SceneStrategy", e.toString());
                }
                return "";
            }).toArray(String[]::new));

            BooleanCallbackInterface confirmShutdownCallback = confirmationResult -> {
                if (confirmationResult) {
                    performAction(cardBinding, appliance, finalResult);
                }
            };
            if (!stationsToTurnOff.isEmpty()) {
                DialogManager.createConfirmationDialog(
                        "Confirm station shutdown", "Station(s) " + stationIdsString + " will shutdown. Please confirm this scene.",
                        confirmShutdownCallback,
                        "Cancel",
                        "Confirm",
                        false);
                return;
            }
        }
        performAction(cardBinding, appliance, finalResult);
    }

    private void performAction(CardApplianceSceneBinding binding, Appliance appliance, View finalResult) {
        HashSet<String> updates = new HashSet<>();
        Appliance last = ApplianceViewModel.activeSceneList.get(appliance.room);

        if(last != null && last != appliance) {
            ApplianceViewModel.activeSceneList.remove(appliance.room);
            ApplianceViewModel.activeScenes.getValue().remove(last.id);
            SceneController.latestOff.add(last.id);
            updates.add(last.id);
        }
        SceneController.latestOn.add(appliance.id);

        updates.add(appliance.id);
        ApplianceViewModel.activeScenes.getValue().put(appliance.id, "On");

        //Action : [cbus unit : group address : id address : scene value] : [type : room : id scene]
        String automationValue = appliance.id.substring(appliance.id.length() -1 , appliance.id.length());
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"                             //[0] Action
                        + appliance.id + ":"                        //[1] CBUS unit number
                        + automationValue + ":"                     //[7] CBUS object id/doubles as card id
                        + NetworkService.getIPAddress());           //[8] The IP address of the tablet

        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();

        //Disable all other scenes in the room
        SceneController.handeActivatingScene(appliance, false);

        //Check what sort of RecyclerView is active then update the card's appearance if visible
        if((!Objects.equals(roomType, "All")  || ApplianceFragment.overrideRoom != null) && !ApplianceFragment.checkForEmptyRooms(roomType)) {
            for (String cards : updates) {
                ApplianceAdapter.getInstance().updateIfVisible(cards);
            }
        } else {
            if (ApplianceParentAdapter.getInstance() != null) {
                for (String cards : updates) {
                    ApplianceParentAdapter.getInstance().updateIfVisible(cards);
                }
            }
        }

        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
            put("appliance_type", appliance.type);
            put("appliance_room", appliance.room);
            put("appliance_new_value", appliance.value);
            put("appliance_action_type", "scene");
        }};
        FirebaseManager.logAnalyticEvent("scene_updated", analyticsAttributes);
    }
}
