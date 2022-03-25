package com.lumination.leadmelabs.ui.menu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SideMenuViewModel extends ViewModel {
    private MutableLiveData<List<String>> info;

    public LiveData<List<String>> getInfo() {
        if (info == null) {
            info = new MutableLiveData<>();
            loadInfo();
        }
        return info;
    }

    private void loadInfo() {
        // Do an asynchronous operation to fetch saved stations from NUC.
    }
}