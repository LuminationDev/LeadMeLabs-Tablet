package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSelectionBinding;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;

import java.util.ArrayList;
import java.util.Arrays;

public class StationSelectionFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private View view;
    private StationAdapter stationAdapter;
    private FragmentStationSelectionBinding binding;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_station_selection, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.stations_list);
        stationAdapter = new StationAdapter(mViewModel, false);
        stationAdapter.stationList = new ArrayList<>();
        recyclerView.setAdapter(stationAdapter);

        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            stationAdapter.stationList = (ArrayList<Station>) stations;
            stationAdapter.notifyDataSetChanged();
        });

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
            StationsFragment.mViewModel.selectSelectedSteamApplication(0);
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SteamSelectionFragment.class, null)
                    .commitNow();
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
                    View confirmDialogView = View.inflate(getContext(), R.layout.dialog_confirm, null);
                    Button confirmButton = confirmDialogView.findViewById(R.id.confirm_button);
                    Button cancelButton = confirmDialogView.findViewById(R.id.cancel_button);
                    TextView headingText = confirmDialogView.findViewById(R.id.heading_text);
                    TextView contentText = confirmDialogView.findViewById(R.id.content_text);
                    headingText.setText("Exit theatre mode?");
                    contentText.setText(String.join(", ", theatreStations) + (theatreStations.size() > 1 ? " are" : " is") +" currently in theatre mode. Are you sure you want to exit theatre mode?");
                    AlertDialog confirmDialog = new AlertDialog.Builder(getContext()).setView(confirmDialogView).create();
                    confirmButton.setOnClickListener(w -> confirmLaunchGame(selectedIds, steamGameId, confirmDialog));
                    cancelButton.setOnClickListener(x -> confirmDialog.dismiss());
                    confirmDialog.show();
                } else {
                    confirmLaunchGame(selectedIds, steamGameId);
                }
            }
        });
    }

    private void confirmLaunchGame(int[] selectedIds, int steamGameId) {
        String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));
        NetworkService.sendMessage("Station," + stationIds, "Steam", "Launch:" + steamGameId);
        MainActivity.fragmentManager.beginTransaction()
                .replace(R.id.main, DashboardPageFragment.class, null)
                .commitNow();
        MainActivity.awaitStationGameLaunch(selectedIds, SteamSelectionFragment.mViewModel.getSelectedSteamApplicationName(steamGameId));
    }

    private void confirmLaunchGame(int[] selectedIds, int steamGameId, AlertDialog dialog) {
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