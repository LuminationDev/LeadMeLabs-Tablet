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

        return inflater.inflate(R.layout.fragment_page_station_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadFragments();

        CheckBox selectCheckbox = view.findViewById(R.id.select_all_checkbox);
        selectCheckbox.setOnCheckedChangeListener((checkboxView, checked) -> {
            ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
            stations = (ArrayList<Station>) stations.clone();
            for (Station station:stations) {
                if (!station.status.equals("Off") && station.hasSteamApplicationInstalled(mViewModel.getSelectedSteamApplicationId())) {
                    station.selected = checked;
                    mViewModel.updateStationById(station.id, station);
                }
            }
        });

        Button backButton = view.findViewById(R.id.cancel_button);
        backButton.setOnClickListener(v -> {
            mViewModel.selectSelectedSteamApplication(0);
            SideMenuFragment.loadFragment(SteamSelectionFragment.class, "session");
        });

        Button playButton = view.findViewById(R.id.select_stations);
        playButton.setOnClickListener(v -> {
            int steamGameId = mViewModel.getSelectedSteamApplicationId();
            int[] selectedIds = mViewModel.getSelectedStationIds();
            if (selectedIds.length > 0) {
                ArrayList<String> theatreStations = new ArrayList<>();
                for (Station station:mViewModel.getSelectedStations()) {
                    if (station.theatreText != null) {
                        theatreStations.add(station.name);
                    }
                }

                if (theatreStations.size() > 0) {
                    DialogManager.buildSelectionLaunch(getContext(), theatreStations, steamGameId, selectedIds);
                } else {
                    confirmLaunchGame(selectedIds, steamGameId);
                }
            }
        });

        View identifyStations = view.findViewById(R.id.identify_button);
        identifyStations.setOnClickListener(v -> {
            List<Station> stations = StationSelectionFragment.getInstance().getRoomStations();
            Identifier.identifyStations(stations);
        });

        instance = this;
    }

    public void confirmLaunchGame(int[] selectedIds, int steamGameId) {
        String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));
        NetworkService.sendMessage("Station," + stationIds, "Steam", "Launch:" + steamGameId);
        SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
        DialogManager.awaitStationGameLaunch(selectedIds, SteamSelectionFragment.mViewModel.getSelectedSteamApplicationName(steamGameId), false);
    }

    public void confirmLaunchGame(int[] selectedIds, int steamGameId, AlertDialog dialog) {
        dialog.dismiss();
        confirmLaunchGame(selectedIds, steamGameId);
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