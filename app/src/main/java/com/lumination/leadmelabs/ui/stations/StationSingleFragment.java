package com.lumination.leadmelabs.ui.stations;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;

import java.util.ArrayList;

public class StationSingleFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private View view;
    private SteamApplicationAdapter steamApplicationAdapter;
    private FragmentStationSingleBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_station_single, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    private final Slider.OnSliderTouchListener touchListener =
            new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(Slider slider) {

                }

                @Override
                public void onStopTrackingTouch(Slider slider) {
                    Station selectedStation = binding.getSelectedStation();
                    selectedStation.volume = (int) slider.getValue();
                    mViewModel.updateStationById(selectedStation.id, selectedStation);
                    NetworkService.sendMessage("Station," + selectedStation.id, "Station", "SetValue:volume:" + String.valueOf(selectedStation.volume));
                    System.out.println(slider.getValue());
                }
            };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);

        GridView steamGridView = (GridView) view.findViewById(R.id.steam_list);
        steamApplicationAdapter = new SteamApplicationAdapter(getContext(), mViewModel, true);
        steamApplicationAdapter.steamApplicationList = new ArrayList<>();
        steamGridView.setAdapter(steamApplicationAdapter);

        Slider stationVolumeSlider = view.findViewById(R.id.station_volume_slider);
        stationVolumeSlider.addOnSliderTouchListener(touchListener);

        Button shutdownButton = view.findViewById(R.id.station_shutdown);
        shutdownButton.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "Shutdown");

            DialogInterface.OnClickListener cancelButtonFunction = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "CancelShutdown");
                }
            };

            AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                    .setTitle("Shutting Down")
                    .setMessage("Cancel shutdown?")
                    .setNegativeButton("Cancel (10)", cancelButtonFunction)
                    .setPositiveButton("Continue", null).show();
            CountDownTimer timer = new CountDownTimer(9000, 1000) {
                @Override
                public void onTick(long l) {
                    Button cancelButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                    cancelButton.setText("Cancel (" + (l + 1000) / 1000 + ")");
                }

                @Override
                public void onFinish() {
                    alertDialog.dismiss();
                }
            }.start();
        });

        Button stopGame = view.findViewById(R.id.station_stop_game);
        stopGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "StopGame");
        });

        Button closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, DashboardPageFragment.class, null)
                    .commitNow();
        });

        Button startVr = view.findViewById(R.id.station_start_vr);
        startVr.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "StartVR");
        });
        Button restartVr = view.findViewById(R.id.station_restart_vr);
        restartVr.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "RestartVR");
        });
        Button endVr = view.findViewById(R.id.station_end_vr);
        endVr.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "EndVR");
        });

        mViewModel.getSelectedStation().observe(getViewLifecycleOwner(), station -> {
            binding.setSelectedStation(station);
            steamApplicationAdapter.steamApplicationList = station.steamApplications;
            steamApplicationAdapter.stationId = station.id;
            steamApplicationAdapter.notifyDataSetChanged();
        });
    }
}