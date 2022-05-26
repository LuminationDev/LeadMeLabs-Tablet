package com.lumination.leadmelabs.ui.stations;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;
import com.lumination.leadmelabs.services.NetworkService;

public class StationSingleFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private View view;
    private FragmentStationSingleBinding binding;
    private androidx.appcompat.app.AlertDialog urlDialog;

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

        Slider stationVolumeSlider = view.findViewById(R.id.station_volume_slider);
        stationVolumeSlider.addOnSliderTouchListener(touchListener);

        Button stopGame = view.findViewById(R.id.station_stop_game);
        stopGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "StopGame");
        });

        Button newGame = view.findViewById(R.id.station_new_game);
        newGame.setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SteamSelectionFragment.class, null)
                    .commitNow();
            SteamSelectionFragment.setStationId(binding.getSelectedStation().id);
        });

        Button newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SteamSelectionFragment.class, null)
                    .commitNow();
            SteamSelectionFragment.setStationId(binding.getSelectedStation().id);
        });

        Button restartVr = view.findViewById(R.id.station_restart_vr);
        restartVr.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "RestartVR");
        });

        Button endVr = view.findViewById(R.id.station_end_vr);
        endVr.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "EndVR");
        });

        buildEnterUrlDialog();

        Button button = view.findViewById(R.id.enter_url);
        button.setOnClickListener(v -> {
            urlDialog.show();
        });
        ImageView gameControlImage = (ImageView) view.findViewById(R.id.game_control_image);

        SwitchCompat powerSwitch = view.findViewById(R.id.power_toggle);
        CompoundButton.OnCheckedChangeListener powerSwitchListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!isChecked) {
                    Station selectedStation = binding.getSelectedStation();
                    NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "Shutdown");

                    DialogInterface.OnClickListener cancelButtonFunction = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "CancelShutdown");
                            compoundButton.setChecked(true);
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
                } else {
                    // turn the station on
                }
            }
        };
        powerSwitch.setOnCheckedChangeListener(powerSwitchListener);

        mViewModel.getSelectedStation().observe(getViewLifecycleOwner(), station -> {
            binding.setSelectedStation(station);
            powerSwitch.setOnCheckedChangeListener(null);
            powerSwitch.setChecked(!station.status.equals("Off"));
            powerSwitch.setOnCheckedChangeListener(powerSwitchListener);
            if (station.gameId != null && station.gameId.length() > 0) {
                Glide.with(view).load(SteamApplication.getImageUrl(station.gameId)).into(gameControlImage);
            } else {
                gameControlImage.setImageDrawable(null);
            }
        });
    }

    private void buildEnterUrlDialog() {
        View view = View.inflate(getContext(), R.layout.dialog_enter_url, null);
        EditText url = view.findViewById(R.id.url_input);
        Button submit = view.findViewById(R.id.submit_button);
        TextView errorText = view.findViewById(R.id.error_text);
        submit.setOnClickListener(v -> {
            errorText.setVisibility(View.GONE);
            String input = url.getText().toString();
            if (Patterns.WEB_URL.matcher(input).matches()) {
                Station selectedStation = binding.getSelectedStation();
                selectedStation.gameName = input;
                NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "URL:" + input);
                this.mViewModel.updateStationById(selectedStation.id, selectedStation);
                urlDialog.dismiss();
            } else {
                errorText.setText("Invalid URL. Please check and try again.");
                errorText.setVisibility(View.VISIBLE);
            }
        });
        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            urlDialog.dismiss();
        });
        urlDialog = new androidx.appcompat.app.AlertDialog.Builder(getContext()).setView(view).create();
    }
}