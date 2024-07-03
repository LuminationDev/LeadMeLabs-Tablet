package com.lumination.leadmelabs.ui.keyboard;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import org.json.JSONException;
import org.json.JSONObject;

public class KeyboardViewModel extends ViewModel {
    private final MutableLiveData<Boolean> capsLock = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> shift = new MutableLiveData<>(false);

    public LiveData<Boolean> getCapsLock() {
        return capsLock;
    }

    public void toggleCapsLock() {
        Boolean currentValue = capsLock.getValue();
        if (currentValue != null) {
            capsLock.setValue(!currentValue);
        } else {
            capsLock.setValue(true);
        }
    }

    public LiveData<Boolean> getShift() {
        return shift;
    }

    public void toggleShift() {
        Boolean currentValue = shift.getValue();
        if (currentValue != null) {
            shift.setValue(!currentValue);
        } else {
            shift.setValue(true);
        }
    }

    public void toggleShiftOff() {
        Boolean currentValue = shift.getValue();
        if (currentValue != null && currentValue) {
            shift.setValue(false);
        }
    }

    public void simulateKeyboardControl(String key) {
        sendKeyboardAction("Control", key);
    }

    public void simulateKeyboardKey(String key) {
        toggleShiftOff();

        sendKeyboardAction("Character", key);
    }

    private void sendKeyboardAction(String control, String key) {
        JSONObject message = new JSONObject();
        try {
            JSONObject details = new JSONObject();
            details.put("Key", key);

            message.put("Details", details);
            message.put("Action", control);
            message.put("Component", "Keyboard");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        if (StationsFragment.mViewModel == null) return;
        if (StationsFragment.mViewModel.getSelectedStation() == null) return;
        if (StationsFragment.mViewModel.getSelectedStation().getValue() == null) return;

        int id = StationsFragment.mViewModel.getSelectedStation().getValue().getId();
        if (id != -1) {
            NetworkService.sendMessage("Station," + id, "Keyboard", message.toString());
        }
    }
}
