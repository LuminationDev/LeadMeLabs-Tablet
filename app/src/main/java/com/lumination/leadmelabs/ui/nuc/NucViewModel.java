package com.lumination.leadmelabs.ui.nuc;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.services.NetworkService;

public class NucViewModel extends AndroidViewModel {
    private MutableLiveData<String> nucAddress;

    public NucViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getNuc() {
        if (nucAddress == null) {
            nucAddress = new MutableLiveData<String>();
            loadNuc();
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
        NetworkService.sendMessage("NUC", "Stations", "List");
        NetworkService.sendMessage("NUC", "Scenes", "List");
    }

    private void loadNuc() {
//      TODO  consider if this should be moved out - arguably best practice doesn't include android references in the view model
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("nuc_address", Context.MODE_PRIVATE);
        setNucAddress(sharedPreferences.getString("nuc_address", ""));
    }
}