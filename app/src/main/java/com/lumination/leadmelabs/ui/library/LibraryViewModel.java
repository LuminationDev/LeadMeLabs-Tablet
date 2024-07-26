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
    private MutableLiveData<ArrayList<String>> filters = new MutableLiveData<>(new ArrayList<>());

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

    public MutableLiveData<ArrayList<String>> getFilters() {
        if (filters == null) {
            filters = new MutableLiveData<>(new ArrayList<>());
        }
        return filters;
    }

    public void setFilters(ArrayList<String> filters) {
        this.filters.setValue(filters);
    }

    public void toggleFilter(String filter) {
        ArrayList<String> filters = this.filters.getValue();

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

        setFilters(filters);
    }

    /**
     * Reset the filters list only if it is not already empty
     */
    public void resetFilter() {
        Properties properties = new Properties();
        properties.put("classification", "Filter");
        Segment.trackEvent(SegmentConstants.Filters_Reset, properties);

        List<String> currentFilters = this.filters.getValue();
        if (currentFilters != null && !currentFilters.isEmpty()) {
            setFilters(new ArrayList<>());
        }
    }
}
