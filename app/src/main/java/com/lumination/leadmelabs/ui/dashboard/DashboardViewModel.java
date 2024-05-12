package com.lumination.leadmelabs.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class DashboardViewModel extends AndroidViewModel {
    private MutableLiveData<String> activatingMode = new MutableLiveData<>();
    private MutableLiveData<Boolean> changingMode = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getActivatingMode() {
        if (activatingMode == null) {
            activatingMode = new MutableLiveData<>();
        }

        return activatingMode;
    }

    public void setActivatingMode(String mode) {
        this.activatingMode.setValue(mode);
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

    public void resetMode() {
        this.changingMode.setValue(false);
        this.activatingMode.setValue(null);
    }
}
