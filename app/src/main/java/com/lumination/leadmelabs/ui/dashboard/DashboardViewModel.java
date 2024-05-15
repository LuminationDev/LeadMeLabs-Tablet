package com.lumination.leadmelabs.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends AndroidViewModel {
    /**
     * Track what dashboard mode is being activated.
     */
    private MutableLiveData<List<String>> activatingMode = new MutableLiveData<>();

    /**
     * Track if a dashboard mode is being activated.
     */
    private MutableLiveData<Boolean> changingMode = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<String>> getActivatingMode() {
        if (activatingMode == null) {
            activatingMode = new MutableLiveData<>();
        }

        return activatingMode;
    }

    public void setActivatingMode(String mode) {
        // Get the current list from activatingMode
        List<String> currentList = getActivatingMode().getValue();

        // Check if the currentList is null (if it hasn't been initialized yet)
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        currentList.add(mode);
        // Set the updated list back to activatingMode
        this.activatingMode.setValue(currentList);
    }

    public void removeActivatingMode(String mode) {
        // Get the current list from activatingMode
        List<String> currentList = activatingMode.getValue();

        // Check if the currentList is not null
        if (currentList == null) return;

        // Remove the item you want from the list
        currentList.remove(mode);

        // Set the updated list back to activatingMode
        activatingMode.setValue(currentList);
    }

    public LiveData<Boolean> getChangingMode() {
        if (changingMode == null) {
            changingMode = new MutableLiveData<>();
        }

        return changingMode;
    }

    public void setChangingMode(boolean changing) {
        this.changingMode.setValue(changing);
    }

    public void resetMode(String mode) {
        //TODO this may have to be linked to a room or lab??
        this.changingMode.setValue(false);



        removeActivatingMode(mode);
    }
}
