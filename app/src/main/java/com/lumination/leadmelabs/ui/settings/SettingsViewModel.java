package com.lumination.leadmelabs.ui.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.services.NetworkService;

/**
 * Only responsible for setting the address as it is saved in shared
 * preferences afterwards which can be loaded at the application start.
 */
public class SettingsViewModel extends AndroidViewModel {
    private MutableLiveData<String> nucAddress;
    private MutableLiveData<Boolean> hideStationControls;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getNuc() {
        if (NetworkService.getNUCAddress() == null || NetworkService.getNUCAddress().equals("")) {
            nucAddress = new MutableLiveData<>();
        } else {
            nucAddress = new MutableLiveData<>(NetworkService.getNUCAddress());
        }

        return nucAddress;
    }

    public LiveData<Boolean> getHideStationControls() {
        if (hideStationControls == null) {
            hideStationControls = new MutableLiveData<Boolean>();
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("hide_station_controls", Context.MODE_PRIVATE);
            hideStationControls.setValue(sharedPreferences.getBoolean("hide_station_controls", true));
        }
        return hideStationControls;
    }

    public void setHideStationControls(Boolean value) {
        hideStationControls.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("hide_station_controls", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("hide_station_controls", value.booleanValue());
        editor.apply();
    }

    public void setNucAddress (String newValue) {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("nuc_address", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nuc_address", newValue);
        editor.apply();
        NetworkService.setNUCAddress(newValue);
        nucAddress.setValue(newValue);
        NetworkService.sendMessage("NUC", "Stations", "List");
        NetworkService.sendMessage("NUC", "Zones", "List");
    }
}