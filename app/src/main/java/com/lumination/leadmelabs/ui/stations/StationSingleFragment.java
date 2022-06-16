package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;
import com.lumination.leadmelabs.services.NetworkService;
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

        Button stopGame = view.findViewById(R.id.station_stop_game);
        stopGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "StopGame");
        });

        Button newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            SideMenuFragment.loadFragment(SteamSelectionFragment.class, "session");
            SteamSelectionFragment.setStationId(binding.getSelectedStation().id);
        });

        Button restartVr = view.findViewById(R.id.station_restart_vr);
        restartVr.setOnClickListener(v ->
                NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "RestartVR")
        );

        Button endVr = view.findViewById(R.id.station_end_vr);
        endVr.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "EndVR");
            DialogManager.awaitStationEndSession(new int[] { binding.getSelectedStation().id });
        });

        Button button = view.findViewById(R.id.enter_url);
        button.setOnClickListener(v ->
                DialogManager.buildURLDialog(getContext(), binding)
        );

        MaterialButton shutdownButton = view.findViewById(R.id.shutdown_station);
        shutdownButton.setOnClickListener(v ->
                DialogManager.buildShutdownDialog(getContext(), new int[] { binding.getSelectedStation().id })
        );

        ImageView gameControlImage = (ImageView) view.findViewById(R.id.game_control_image);
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