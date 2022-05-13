package com.lumination.leadmelabs.ui.sidemenu.submenu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SubMenuViewModel extends ViewModel {
    private MutableLiveData<List<String>> info;
    private MutableLiveData<String> selectedPage = new MutableLiveData<>();

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

    //Light is the default when opening the sub menu
    public LiveData<String> getSelectedPage() {
        if (selectedPage == null) {
            selectedPage = new MutableLiveData<>("lighting");
        }
        return selectedPage;
    }

    public void setSelectedPage(String page) { selectedPage.setValue(page);}
}