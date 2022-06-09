package com.lumination.leadmelabs.ui.room;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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

    public void setRooms(HashSet<String> values) { rooms.setValue(values); }

    public LiveData<String> getSelectedRoom() {
        if (selectedRoom == null) {
            selectedRoom = new MutableLiveData<>("All");
        }
        return selectedRoom;
    }

    public void setSelectedRoom(String page) { selectedRoom.setValue(page);}
}
