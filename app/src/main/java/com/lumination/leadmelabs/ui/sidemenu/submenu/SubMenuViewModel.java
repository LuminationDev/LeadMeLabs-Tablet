package com.lumination.leadmelabs.ui.sidemenu.submenu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Station;

import java.util.HashSet;
import java.util.List;

public class SubMenuViewModel extends ViewModel {
    private MutableLiveData<HashSet<String>> subObjects = new MutableLiveData<>();
    private MutableLiveData<String> selectedPage = new MutableLiveData<>();

    public LiveData<HashSet<String>> getSubObjects() {
        if (subObjects == null) {
            subObjects = new MutableLiveData<>(new HashSet<>()); //Create empty hashset
        }
        return subObjects;
    }

    public void setSubObjects(HashSet<String> applianceTypes) {
        subObjects.setValue(applianceTypes);
    }

    //Light is the default when opening the sub menu
    public LiveData<String> getSelectedPage() {
        if (selectedPage == null) {
            selectedPage = new MutableLiveData<>("scenes");
        }
        return selectedPage;
    }

    public void setSelectedPage(String page) { selectedPage.setValue(page);}
}