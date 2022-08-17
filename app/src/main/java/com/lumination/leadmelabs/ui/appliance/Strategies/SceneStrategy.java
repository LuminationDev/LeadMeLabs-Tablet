package com.lumination.leadmelabs.ui.appliance.Strategies;

import android.view.View;

import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.abstractClasses.AbstractApplianceStrategy;
import com.lumination.leadmelabs.databinding.CardApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceAdapter;
import com.lumination.leadmelabs.ui.appliance.ApplianceController;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceParentAdapter;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.WakeOnLan;

import java.util.HashSet;
import java.util.Objects;

/**
 * A scene can be re-triggered if active, as sub elements can be changed while a scene is
 * active. If nothing has changed then the Cbus disregards the message.
 */
public class SceneStrategy extends AbstractApplianceStrategy {
    @Override
    public void trigger(CardApplianceBinding binding, Appliance appliance, View finalResult) {
        HashSet<String> updates = new HashSet<>();
        String type = "scene";

        binding.setStatus(new MutableLiveData<>("active"));
        Appliance last = ApplianceViewModel.activeSceneList.put(appliance.room, appliance);

        if(last != null && last != appliance) {
            ApplianceController.latestOff.add(last.id);
            updates.add(last.id);
        }
        ApplianceController.latestOn.add(appliance.id);

        updates.add(appliance.id);

        //Action : [cbus unit : group address : id address : scene value] : [type : room : id scene]
        NetworkService.sendMessage("NUC",
                "Automation",
                "Set" + ":"                             //[0] Action
                        + appliance.automationBase + ":"            //[1] CBUS unit number
                        + appliance.automationGroup + ":"           //[2] CBUS group address
                        + appliance.automationId  + ":"             //[3] CBUS unit address
                        + appliance.automationValue + ":"           //[4] New value for address
                        + type + ":"                                //[5] Object type (computer, appliance, scene)
                        + appliance.room + ":"                      //[6] Appliance room
                        + appliance.id + ":"                        //[7] CBUS object id/doubles as card id
                        + NetworkService.getIPAddress());           //[8] The IP address of the tablet


        //Cancel/start the timer to get the latest updated cards
        ApplianceViewModel.delayLoadCall();

        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();

        //Check what sort of RecyclerView is active then update the card's appearance if visible
        if((!Objects.equals(roomType, "All")  || ApplianceFragment.overrideRoom != null) && !ApplianceFragment.checkForEmptyRooms(roomType)) {
            for (String cards : updates) {
                ApplianceAdapter.getInstance().updateIfVisible(cards);
            }
        } else {
            for (String cards : updates) {
                ApplianceParentAdapter.getInstance().updateIfVisible(cards);
            }
        }

        if(appliance.room.equals(Constants.VR_ROOM) && appliance.name.equals("On")) {
            WakeOnLan.WakeAll();
        }
    }
}
