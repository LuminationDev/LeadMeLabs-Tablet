package com.lumination.leadmelabs.ui.room;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.List;

public class RoomViewModel extends ViewModel {
    private MutableLiveData<List<String>> rooms;
    private MutableLiveData<String> selectedRoom = new MutableLiveData<>();

    public LiveData<List<String>> getRooms() {
        if (rooms == null) {
            rooms = new MutableLiveData<>(null);
            loadRooms();
        }
        return rooms;
    }

    private void loadRooms() {
        // Do an asynchronous operation to fetch saved rooms from NUC.
        List<String> roomList = Arrays.asList("Classroom", "VR Room", "All");
        rooms.setValue(roomList);
    }

    public LiveData<String> getSelectedRoom() {
        if (selectedRoom == null) {
            selectedRoom = new MutableLiveData<>("All");
        }
        return selectedRoom;
    }

    public void setSelectedRoom(String page) { selectedRoom.setValue(page);}
}
