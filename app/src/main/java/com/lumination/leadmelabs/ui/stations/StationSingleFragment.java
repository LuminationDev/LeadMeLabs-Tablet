package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.utilities.Identifier;

import java.util.Collections;
import java.util.List;

public class StationSingleFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private FragmentStationSingleBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_station_single, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    private final Slider.OnSliderTouchListener touchListener =
            new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider slider) {

                }

                @Override
                public void onStopTrackingTouch(Slider slider) {
                    Station selectedStation = binding.getSelectedStation();
                    selectedStation.volume = (int) slider.getValue();
                    mViewModel.updateStationById(selectedStation.id, selectedStation);
                    NetworkService.sendMessage("Station," + selectedStation.id, "Station", "SetValue:volume:" + selectedStation.volume);
                    System.out.println(slider.getValue());
                }
            };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Slider stationVolumeSlider = view.findViewById(R.id.station_volume_slider);
        stationVolumeSlider.addOnSliderTouchListener(touchListener);

        Button pingStation = view.findViewById(R.id.ping_station);
        pingStation.setOnClickListener(v -> {
            List<Station> stations = Collections.singletonList(binding.getSelectedStation());
            Identifier.identifyStations(stations);
        });

        Button newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            SideMenuFragment.loadFragment(SteamSelectionFragment.class, "session");
            SteamSelectionFragment.setStationId(binding.getSelectedStation().id);
        });

        Button restartGame = view.findViewById(R.id.station_restart_session);
        restartGame.setOnClickListener(v -> {
            if (binding.getSelectedStation().gameId != null && binding.getSelectedStation().gameId.length() > 0) {
                NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "Steam", "Launch:" + binding.getSelectedStation().gameId);
                SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
                DialogManager.awaitStationGameLaunch(new int[] { binding.getSelectedStation().id }, SteamSelectionFragment.mViewModel.getSelectedSteamApplicationName(Integer.parseInt(binding.getSelectedStation().gameId)));
            }
        });

        Button restartVr = view.findViewById(R.id.station_restart_vr);
        restartVr.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "RestartVR");
            DialogManager.awaitStationRestartSession(new int[] { binding.getSelectedStation().id });
        });

        Button endGame = view.findViewById(R.id.station_end_session);
        endGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "StopGame");
        });

        Button button = view.findViewById(R.id.enter_url);
        button.setOnClickListener(v ->
                DialogManager.buildURLDialog(getContext(), binding)
        );

        MaterialButton shutdownButton = view.findViewById(R.id.shutdown_station);
        shutdownButton.setOnClickListener(v -> {
            int id = binding.getSelectedStation().id;
            Station station = mViewModel.getStationById(id);

            if (station.status.equals("Off")) {
                station.powerStatusCheck();

                MainActivity.runOnUI(() -> {
                    station.status = "Turning on";
                    mViewModel.updateStationById(id, station);
                });
                //As per the CBUS code value is hard coded to 2.
                NetworkService.sendMessage("NUC", "Automation", "Set:0:" + station.automationGroup + ":" + station.automationId  + ":" + station.id + ":" + 2 + ":" + station.room);

            } else if(station.status.equals("Turning on")) {
                Toast.makeText(getContext(), "Computer is starting", Toast.LENGTH_SHORT).show();

            } else {
                DialogManager.buildShutdownDialog(getContext(), new int[]{id});
            }
        });

        ImageView gameControlImage = view.findViewById(R.id.game_control_image);
        mViewModel.getSelectedStation().observe(getViewLifecycleOwner(), station -> {
            binding.setSelectedStation(station);
            if (station.gameId != null && station.gameId.length() > 0) {
                Glide.with(view).load(SteamApplication.getImageUrl(station.gameId)).into(gameControlImage);
            } else {
                gameControlImage.setImageDrawable(null);
            }
        });
    }
}