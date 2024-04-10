package com.lumination.leadmelabs.ui.library;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<String> pageTitle = new MutableLiveData<>("VR Library");
    private MutableLiveData<String> libraryTitle = new MutableLiveData<>("VR Library");
    private MutableLiveData<String> subTitle = new MutableLiveData<>("Pick an experience to play in VR");
    private MutableLiveData<String> currentSearch = new MutableLiveData<>("");
    private MutableLiveData<String> libraryType = new MutableLiveData<>("vr_experiences");
    private MutableLiveData<ArrayList<String>> subjectFilters = new MutableLiveData<>(new ArrayList<>());

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

    public MutableLiveData<ArrayList<String>> getSubjectFilters() {
        if (subjectFilters == null) {
            subjectFilters = new MutableLiveData<>(new ArrayList<>());
        }
        return subjectFilters;
    }

    public void setSubjectFilters(ArrayList<String> subjectFilters) {
        this.subjectFilters.setValue(subjectFilters);
    }

    public void toggleFilter(String filter) {
        ArrayList<String> filters = this.subjectFilters.getValue();

        if (Objects.requireNonNull(filters).contains(filter)) {
            filters.remove(filter);
        } else {
            filters.add(filter);
        }

        setSubjectFilters(filters);
    }

    /**
     * Reset the filters list only if it is not already empty
     */
    public void resetFilter() {
        List<String> currentFilters = this.subjectFilters.getValue();
        if (currentFilters != null && !currentFilters.isEmpty()) {
            setSubjectFilters(new ArrayList<>());
        }
    }
}
