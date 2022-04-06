package com.lumination.leadmelabs.ui.logo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class LogoViewModel extends ViewModel {
    private MutableLiveData<List<String>> info;

    public LiveData<List<String>> getInfo() {
        if (info == null) {
            info = new MutableLiveData<>(null);
            loadInfo();
        }
        return info;
    }

    private void loadInfo() {
        // Do an asynchronous operation to fetch saved stations from NUC.
    }
}