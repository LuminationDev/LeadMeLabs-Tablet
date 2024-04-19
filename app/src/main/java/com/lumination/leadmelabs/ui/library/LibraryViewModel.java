package com.lumination.leadmelabs.ui.library;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.segment.analytics.Properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LibraryViewModel extends ViewModel {
    private MutableLiveData<String> libraryTitle = new MutableLiveData<>("VR Library");
    private MutableLiveData<String> currentSearch = new MutableLiveData<>("");
    private MutableLiveData<String> libraryType = new MutableLiveData<>("vr_experiences");
    private MutableLiveData<ArrayList<String>> subjectFilters = new MutableLiveData<>(new ArrayList<>());

    public MutableLiveData<String> getLibraryTitle() {
        if (libraryTitle == null) {
            libraryTitle = new MutableLiveData<>("VR Library");
        }
        return libraryTitle;
    }

    public void setLibraryTitle(String libraryTitle) {
        this.libraryTitle.setValue(libraryTitle);
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

        Properties properties = new Properties();
        properties.put("classification", "Filter");
        properties.put("filterType", filter);

        if (Objects.requireNonNull(filters).contains(filter)) {
            filters.remove(filter);
            Segment.trackEvent(SegmentConstants.Filter_Removed, properties);
        } else {
            filters.add(filter);
            Segment.trackEvent(SegmentConstants.Filter_Added, properties);
        }

        setSubjectFilters(filters);
    }

    /**
     * Reset the filters list only if it is not already empty
     */
    public void resetFilter() {
        Properties properties = new Properties();
        properties.put("classification", "Filter");
        Segment.trackEvent(SegmentConstants.Filters_Reset, properties);

        List<String> currentFilters = this.subjectFilters.getValue();
        if (currentFilters != null && !currentFilters.isEmpty()) {
            setSubjectFilters(new ArrayList<>());
        }
    }
}
