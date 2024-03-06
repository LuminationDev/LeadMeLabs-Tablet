package com.lumination.leadmelabs.ui.library;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<String> pageTitle = new MutableLiveData<>("VR Library");
    private MutableLiveData<String> libraryTitle = new MutableLiveData<>("VR Library");
    private MutableLiveData<String> subTitle = new MutableLiveData<>("Pick an experience to play in VR");
    private MutableLiveData<String> currentSearch = new MutableLiveData<>("");
    private MutableLiveData<String> libraryType = new MutableLiveData<>("vr_experiences");

    public MutableLiveData<String> getPageTitle() {
        if (pageTitle == null) {
            pageTitle = new MutableLiveData<>("VR Library");
        }
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle.setValue(pageTitle);
    }

    public MutableLiveData<String> getLibraryTitle() {
        if (libraryTitle == null) {
            libraryTitle = new MutableLiveData<>("VR Library");
        }
        return libraryTitle;
    }

    public void setLibraryTitle(String libraryTitle) {
        this.libraryTitle.setValue(libraryTitle);
    }

    public MutableLiveData<String> getSubTitle() {
        if (subTitle == null) {
            subTitle = new MutableLiveData<>("Pick an experience to play in VR");
        }
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle.setValue(subTitle);
    }

    public MutableLiveData<String> getCurrentSearch() {
        if (currentSearch == null) {
            currentSearch = new MutableLiveData<>("");
        }
        return currentSearch;
    }

    public void setCurrentSearch(String currentSearch) {
        this.currentSearch.setValue(currentSearch);
    }

    public MutableLiveData<String> getLibraryType() {
        if (libraryType == null) {
            libraryType = new MutableLiveData<>("vr_experiences");
        }
        return libraryType;
    }

    public void setLibraryType(String libraryType) {
        this.libraryType.setValue(libraryType);
    }
}
