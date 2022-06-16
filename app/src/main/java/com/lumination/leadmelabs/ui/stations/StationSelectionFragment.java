package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.utilities.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StationSelectionFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private StationAdapter stationAdapter;

    public static StationSelectionFragment instance;
    public static StationSelectionFragment getInstance() { return instance; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_station_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.stations_list);
        stationAdapter = new StationAdapter(mViewModel, false);
        stationAdapter.stationList = new ArrayList<>();
        recyclerView.setAdapter(stationAdapter);

        mViewModel.getStations().observe(getViewLifecycleOwner(), this::reloadData);

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

        Button identifyStations = view.findViewById(R.id.identify_button);
        identifyStations.setOnClickListener(v -> {
            List<Station> stations = stationAdapter.stationList;
            Identifier.identifyStations(stations);
        });

        instance = this;
    }

    /**
     * Reload the current appliance list when a room is changed.
     */
    public void notifyDataChange() {
        mViewModel.getStations().observe(getViewLifecycleOwner(), this::reloadData);
    }

    private void reloadData(List<Station> stations) {
        ArrayList<Station> stationRoom = new ArrayList<>();

        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();
        if(roomType == null) {
            roomType = "All";
        }

        for(Station station : stations) {
            if(roomType.equals("All")) {
                stationRoom.add(station);
            } else if(station.room.equals(roomType)) {
                stationRoom.add(station);
            }
        }

        stationAdapter.stationList = stationRoom;
        stationAdapter.notifyDataSetChanged();
        TextView noStationsAvailable = getView().findViewById(R.id.no_stations_available);
        noStationsAvailable.setVisibility(stationRoom.size() == 0 ? View.VISIBLE : View.GONE);
    }

    public void confirmLaunchGame(int[] selectedIds, int steamGameId) {
        String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));
        NetworkService.sendMessage("Station," + stationIds, "Steam", "Launch:" + steamGameId);
        SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
        DialogManager.awaitStationGameLaunch(selectedIds, SteamSelectionFragment.mViewModel.getSelectedSteamApplicationName(steamGameId));
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
}