package com.lumination.leadmelabs.models;

import static com.lumination.leadmelabs.ui.appliance.ApplianceFragment.checkForEmptyRooms;

import com.lumination.leadmelabs.ui.appliance.adapters.ApplianceAdapter;
import com.lumination.leadmelabs.ui.appliance.ApplianceParentAdapter;
import com.lumination.leadmelabs.ui.room.RoomFragment;

import java.util.HashMap;
import java.util.Objects;

/**
 * If the lambda from observer does not access anything then it compiles as a singleton and
 * does not allow for movement between subpages. Instantiating a class bypasses this base
 * behaviour.
 */
public class Filler {
    /*
     * Update all available ApplianceAdapters, depending if all rooms are visible or just a single
     * one is active.
     *
     * @param active A list containing the IDs of any appliance card that needs to be refreshed due
     *             to information changing.
     */
    public Filler(HashMap<String, String> active, String overrideRoom) {
        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();

        boolean roomCheck = checkForEmptyRooms(roomType);
        if((!Objects.equals(roomType, "All") || overrideRoom != null) && !roomCheck) {
            for (String cards : active.keySet()) {
                ApplianceAdapter.getInstance().updateIfVisible(cards);
            }
        } else {
            for (String cards : active.keySet()) {
                ApplianceParentAdapter.getInstance().updateIfVisible(cards);
            }
        }
    }
}
