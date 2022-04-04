package com.lumination.leadmelabs.ui.nuc;

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
public class NucViewModel extends AndroidViewModel {
    private MutableLiveData<String> nucAddress;

    public NucViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getNuc() {
        if (NetworkService.getNUCAddress().equals("")) {
            nucAddress = new MutableLiveData<>();
        } else {
            nucAddress = new MutableLiveData<>(NetworkService.getNUCAddress());
        }

        return nucAddress;
    }

    public void setNucAddress (String newValue) {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("nuc_address", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nuc_address", newValue);
        editor.apply();
        NetworkService.setNUCAddress(newValue);
        nucAddress.setValue(newValue);
        NetworkService.sendMessage("NUC", "Automation", "TriggerScene:Get");
        NetworkService.sendMessage("NUC", "Stations", "List");
        NetworkService.sendMessage("NUC", "Scenes", "List");
    }
}