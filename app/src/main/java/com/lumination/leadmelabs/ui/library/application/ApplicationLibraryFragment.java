package com.lumination.leadmelabs.ui.library.application;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentLibraryApplicationBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.ILibraryInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.segment.analytics.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class ApplicationLibraryFragment extends Fragment implements ILibraryInterface {

    public static StationsViewModel mViewModel;
    public static ApplicationAdapter installedApplicationAdapter;
    private static ArrayList<Application> installedApplicationList;
    private FragmentLibraryApplicationBinding binding;
    public static FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_application, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        UpdateCurrentStationId();
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridView steamGridView = view.findViewById(R.id.experience_list);
        installedApplicationAdapter = new ApplicationAdapter(getContext(), requireActivity().getSupportFragmentManager(), (SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
        updateApplicationList(LibrarySelectionFragment.getStationId(), steamGridView, true);
        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            if (LibrarySelectionFragment.getStationId() > 0) {
                if (installedApplicationAdapter.applicationList.size() != mViewModel.getStationApplications(LibrarySelectionFragment.getStationId()).size()) {
                    updateApplicationList(LibrarySelectionFragment.getStationId(), steamGridView, false);
                }
            } else {
                if (installedApplicationAdapter.applicationList.size() != mViewModel.getAllApplications().size()) {
                    updateApplicationList(LibrarySelectionFragment.getStationId(), steamGridView, false);
                }
            }
        });
    }

    /**
     * On load check if there is a selected Station in the StationViewModel.selectedStationId. This
     * determines if the user is launching an application on a single Station or multiple.
     */
    public void UpdateCurrentStationId() {
        int stationId = 0;
        if(StationsFragment.mViewModel.getSelectedStationId().getValue() != null) {
            stationId = StationsFragment.mViewModel.getSelectedStationId().getValue();
        }

        LibrarySelectionFragment.setStationId(stationId);
        if (LibrarySelectionFragment.getStationId() > 0) {
            installedApplicationList = (ArrayList<Application>) mViewModel.getStationApplications(LibrarySelectionFragment.getStationId());
        } else {
            installedApplicationList = (ArrayList<Application>) mViewModel.getAllApplications();
        }
        if (installedApplicationAdapter != null) {
            installedApplicationAdapter.applicationList = (ArrayList<Application>) installedApplicationList.clone();
            binding.setApplicationList(installedApplicationAdapter.applicationList);
            installedApplicationAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Updates the application list displayed in the provided GridView based on the given station ID.
     * If the new application list is the same as the currently installed application list, no update is performed.
     * If a station ID is provided, updates the installed application list with applications specific to that station;
     * otherwise, updates it with all applications. Then, sets the adapter's application list,
     * updates the UI, performs a search (if applicable), and sets the adapter to the provided GridView.
     *
     * @param stationId The ID of the station to retrieve applications for, Id = 0 retrieves all applications.
     * @param view The GridView to update with the application list.
     * @param onCreate A boolean representing if the fragment has just been created.
     */
    private void updateApplicationList(int stationId, GridView view, boolean onCreate) {
        ArrayList<Application> newApplicationList = (ArrayList<Application>) mViewModel.getAllApplications();
        if (!onCreate && newApplicationList.equals(installedApplicationList)) {
            return;
        }

        if (stationId > 0) {
            installedApplicationList = (ArrayList<Application>) mViewModel.getStationApplications(stationId);
        } else {
            installedApplicationList = (ArrayList<Application>) mViewModel.getAllApplications();
        }
        installedApplicationAdapter.applicationList = (ArrayList<Application>) installedApplicationList.clone();
        binding.setApplicationList(installedApplicationAdapter.applicationList);
        binding.setApplicationsLoaded(mViewModel.getAllApplications().size() > 0);

        //The list has been updated, perform the search again
        performSearch(LibrarySelectionFragment.mViewModel.getCurrentSearch().getValue());
        view.setAdapter(installedApplicationAdapter);
    }

    public void performSearch(String searchTerm) {
        ArrayList<Application> filteredApplicationList = (ArrayList<Application>) installedApplicationList.clone();
        filteredApplicationList.removeIf(currentApplication -> !currentApplication.name.toLowerCase(Locale.ROOT).contains(searchTerm.trim()));
        installedApplicationAdapter.applicationList = filteredApplicationList;
        binding.setApplicationList(installedApplicationAdapter.applicationList);
        installedApplicationAdapter.notifyDataSetChanged();
    }

    /**
     * Checks if there are any running experiences on the current station or other stations.
     * If running experiences are found, prompts the user with a confirmation dialog.
     * If no running experiences are found, proceeds to refresh the list of Steam games.
     */
    public void refreshList() {
        boolean showPrompt = false;
        String message = "";

        if (LibrarySelectionFragment.getStationId() > 0) {
            Station station = mViewModel.getStationById(LibrarySelectionFragment.getStationId());
            if(station.applicationController.getExperienceId() != null && !station.applicationController.getExperienceId().isEmpty()) {
                showPrompt = true;
                message = "Refreshing this experience list will stop the experience: " + station.applicationController.getExperienceName() + ", running on Station " + LibrarySelectionFragment.getStationId();
            }
        } else {
            ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
            if (stations != null) {
                ArrayList<Integer> stationIds = stations.stream()
                        .filter(station -> !station.applicationController.getExperienceId().isEmpty())
                        .map(Station::getId)
                        .collect(Collectors.toCollection(ArrayList::new));

                if (!stationIds.isEmpty()) {
                    String joinedString = stationIds.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(","));

                    showPrompt = true;
                    message = "Refreshing this experience list will stop the currently open experiences on Stations: " + joinedString;
                }
            }
        }

        if (!showPrompt) {
            refreshExperienceList();
            return;
        }

        BooleanCallbackInterface booleanCallbackInterface = result -> {
            if (result) {
                refreshExperienceList();
            }
        };
        DialogManager.createConfirmationDialog(
                "Attention",
                message,
                booleanCallbackInterface,
                "Cancel",
                "Refresh",
                false);

        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", LibrarySelectionFragment.segmentClassification);
        segmentProperties.put("tab", "vr_experiences");
        Segment.trackEvent(SegmentConstants.Refresh_Library_Warning, segmentProperties);
    }

    /**
     * Refresh the list of steam applications on a particular computer or on all.
     */
    private void refreshExperienceList() {
        String stationIds;
        if(LibrarySelectionFragment.getStationId() > 0) {
            stationIds = String.valueOf(LibrarySelectionFragment.getStationId());
        } else {
            stationIds = String.join(", ", Arrays.stream(mViewModel.getAllStationIds()).mapToObj(String::valueOf).toArray(String[]::new));
        }

        //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
        if (MainActivity.isNucJsonEnabled) {
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "Refresh");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            NetworkService.sendMessage("Station," + stationIds, "Experience", message.toString());
        }
        else {
            NetworkService.sendMessage("Station," + stationIds, "Experience", "Refresh");
        }
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", LibrarySelectionFragment.segmentClassification);
        segmentProperties.put("tab", "vr_experiences");
        Segment.trackEvent(SegmentConstants.Refresh_Library, segmentProperties);
    }
}
