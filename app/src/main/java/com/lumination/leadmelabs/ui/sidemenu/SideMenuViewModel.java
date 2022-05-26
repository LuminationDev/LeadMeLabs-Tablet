package com.lumination.leadmelabs.ui.sidemenu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SideMenuViewModel extends ViewModel {
    private MutableLiveData<List<String>> info;
    private MutableLiveData<String> selectedIcon = new MutableLiveData<>();

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

    public LiveData<String> getSelectedIcon() {
        if (selectedIcon == null) {
            selectedIcon = new MutableLiveData<>("dashboard");
        }
        return selectedIcon;
    }

    public void setSelectedIcon(String icon) {
        selectedIcon.setValue(icon);
    }
}