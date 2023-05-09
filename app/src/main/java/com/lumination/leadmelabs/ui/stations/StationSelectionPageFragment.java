package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.application.ApplicationSelectionFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.utilities.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StationSelectionPageFragment extends Fragment {
    public static FragmentManager childManager;
    public static StationsViewModel mViewModel;

    public static StationSelectionPageFragment instance;
    public static StationSelectionPageFragment getInstance() { return instance; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        childManager = getChildFragmentManager();

        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
        stations = (ArrayList<Station>) stations.clone();
        for (Station station:stations) {
            station.selected = false;
            mViewModel.updateStationById(station.id, station);
        }

        return inflater.inflate(R.layout.fragment_page_station_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadFragments();

        CheckBox selectCheckbox = view.findViewById(R.id.select_all_checkbox);
        selectCheckbox.setOnCheckedChangeListener((checkboxView, checked) -> {
            ArrayList<Station> stations = StationSelectionFragment.getInstance().getRoomStations();
            stations = (ArrayList<Station>) stations.clone();
            for (Station station:stations) {
                if (!station.status.equals("Off") && station.hasApplicationInstalled(mViewModel.getSelectedApplicationId())) {
                    station.selected = checked;
                    mViewModel.updateStationById(station.id, station);
                }
            }
        });

        Button backButton = view.findViewById(R.id.cancel_button);
        backButton.setOnClickListener(v -> {
            mViewModel.selectSelectedApplication("");
            SideMenuFragment.loadFragment(ApplicationSelectionFragment.class, "session");
        });

        Button playButton = view.findViewById(R.id.select_stations);
        playButton.setOnClickListener(v -> {
            String selectedGameId = mViewModel.getSelectedApplicationId();
            int[] selectedIds = mViewModel.getSelectedStationIds();
            if (selectedIds.length > 0) {
                confirmLaunchGame(selectedIds, selectedGameId);
            }
        });

        View identifyStations = view.findViewById(R.id.identify_button);
        identifyStations.setOnClickListener(v -> {
            List<Station> stations = StationSelectionFragment.getInstance().getRoomStations();
            Identifier.identifyStations(stations);
        });

        instance = this;
    }

    public void confirmLaunchGame(int[] selectedIds, String selectedGameId) {
        String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));
        NetworkService.sendMessage("Station," + stationIds, "Experience", "Launch:" + selectedGameId);
        SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
        DialogManager.awaitStationGameLaunch(selectedIds, ApplicationSelectionFragment.mViewModel.getSelectedApplicationName(selectedGameId), false);
    }

    public void confirmLaunchGame(int[] selectedIds, String selectedGameId, AlertDialog dialog) {
        dialog.dismiss();
        confirmLaunchGame(selectedIds, selectedGameId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
        stations = (ArrayList<Station>) stations.clone();
        for (Station station:stations) {
            station.selected = false;
            mViewModel.updateStationById(station.id, station);
        }
    }

    private void loadFragments() {
        childManager.beginTransaction()
                .replace(R.id.station_selection_list_container, StationSelectionFragment.class, null)
                .commitNow();
    }
}