package com.lumination.leadmelabs.ui.room;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;

import java.util.HashSet;

public class RoomViewModel extends ViewModel {
    private MutableLiveData<HashSet<String>> rooms;
    private MutableLiveData<String> selectedRoom = new MutableLiveData<>();

    public LiveData<HashSet<String>> getRooms() {
        if (rooms == null) {
            HashSet<String> start = new HashSet<>();
            rooms = new MutableLiveData<>(start);
        }
        return rooms;
    }

    public void setRooms(HashSet<String> values) {
        if (rooms == null || rooms.getValue().size() == 0) {
            lateLoadScenes();
        }
        rooms.setValue(values);
    }

    public LiveData<String> getSelectedRoom() {
        if (selectedRoom == null) {
            selectedRoom = new MutableLiveData<>("All");
        }
        return selectedRoom;
    }

    public void setSelectedRoom(String page) { selectedRoom.setValue(page);}


    /**
     * If a user navigates to the room controls right after start up (within 2 seconds)
     * or if appliances are returned before the rooms are ready, no rooms have been loaded into the
     * adapters array so there is a blank screen. If this is the case, when the
     * rooms do load, redraw the multi recycler and ask for the active appliances again.
     */
    private void lateLoadScenes() {
        if(ApplianceFragment.rooms.isEmpty()) {
            if(ApplianceFragment.getInstance() != null) {
                if(ApplianceFragment.getInstance().getView() != null) {
                    ApplianceFragment.loadMultiRecycler(ApplianceFragment.getInstance().getView());
                }
            }
        }
        ApplianceViewModel.init = false;
        ApplianceViewModel.loadActiveAppliances();
    }
}
