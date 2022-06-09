package com.lumination.leadmelabs.ui.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
    private MutableLiveData<String> pinCode;
    private MutableLiveData<String> encryptionKey;
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
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("hide_station_controls", Context.MODE_PRIVATE);
            hideStationControls = new MutableLiveData<>(sharedPreferences.getBoolean("hide_station_controls", true));
        }
        return hideStationControls;
    }

    public void setHideStationControls(Boolean value) {
        hideStationControls.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("hide_station_controls", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("hide_station_controls", value);
        editor.apply();
    }

    public void setNucAddress(String newValue) {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("nuc_address", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nuc_address", newValue);
        editor.apply();
        NetworkService.setNUCAddress(newValue);
        nucAddress.setValue(newValue);
        NetworkService.sendMessage("NUC", "Stations", "List");
        NetworkService.sendMessage("NUC", "Zones", "List");
    }

    public LiveData<String> getPinCode() {
        if (pinCode == null) {
            pinCode = new MutableLiveData<String>();
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("pin_code", Context.MODE_PRIVATE);
            pinCode.setValue(sharedPreferences.getString("pin_code", null));
        }
        return pinCode;
    }

    public void setPinCode(String value) {
        getPinCode(); // to initialize if not already done
        pinCode.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("pin_code", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pin_code", value);
        editor.apply();
    }

    public LiveData<String> getEncryptionKey() {
        if (encryptionKey == null) {
            encryptionKey = new MutableLiveData<String>();
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("encryption_key", Context.MODE_PRIVATE);
            encryptionKey.setValue(sharedPreferences.getString("encryption_key", null));
        }
        return encryptionKey;
    }

    public void setEncryptionKey(String value) {
        getEncryptionKey(); // to initialize if not already done
        encryptionKey.setValue(value);
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("encryption_key", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("encryption_key", value);
        editor.apply();
    }
}