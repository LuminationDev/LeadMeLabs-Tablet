package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;
import com.lumination.leadmelabs.models.applications.details.Details;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.application.ApplicationSelectionFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.utilities.Identifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class StationSingleFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private FragmentStationSingleBinding binding;
    private static boolean cancelledShutdown = false;
    public static FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_station_single, container, false);
        childManager = getChildFragmentManager();
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

        if (savedInstanceState == null) {
            childManager.beginTransaction()
                    .replace(R.id.logo, LogoFragment.class, null)
                    .commitNow();
        }

        Slider stationVolumeSlider = view.findViewById(R.id.station_volume_slider);
        stationVolumeSlider.addOnSliderTouchListener(touchListener);

        Button menuButton = view.findViewById(R.id.station_single_menu_button);
        menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getActivity(), menuButton);
            PopupMenu.OnMenuItemClickListener onMenuItemClickListener = menuItem -> {
                if (menuItem.getItemId() == R.id.rename) {
                    DialogManager.buildRenameStationDialog(getContext(), binding);
                    return true;
                }
                return false;
            };
            popupMenu.setOnMenuItemClickListener(onMenuItemClickListener);
            popupMenu.inflate(R.menu.station_single_menu_actions);
            popupMenu.show();
        });

        Button pingStation = view.findViewById(R.id.ping_station);
        pingStation.setOnClickListener(v -> {
            List<Station> stations = Collections.singletonList(binding.getSelectedStation());
            Identifier.identifyStations(stations);
        });

        Button newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            SideMenuFragment.loadFragment(ApplicationSelectionFragment.class, "session");
            ApplicationSelectionFragment.setStationId(binding.getSelectedStation().id);
        });

        Button restartGame = view.findViewById(R.id.station_restart_session);
        restartGame.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "Experience", "Restart");


            SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
            DialogManager.awaitStationGameLaunch(new int[] { binding.getSelectedStation().id }, ApplicationSelectionFragment.mViewModel.getSelectedApplicationName(binding.getSelectedStation().gameId), true);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(binding.getSelectedStation().id));
            }};
            FirebaseManager.logAnalyticEvent("session_restarted", analyticsAttributes);
        });

        Button restartVr = view.findViewById(R.id.station_restart_vr);
        restartVr.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "RestartVR");
            DialogManager.awaitStationRestartSession(new int[] { binding.getSelectedStation().id });
            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(binding.getSelectedStation().id));
            }};
            FirebaseManager.logAnalyticEvent("station_vr_system_restarted", analyticsAttributes);
        });

        Button endGame = view.findViewById(R.id.station_end_session);
        endGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            if(selectedStation.gameName == null || selectedStation.gameName.equals("")) {
                return;
            }

            if(SettingsFragment.checkAdditionalExitPrompts()) {
                BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                    if (confirmationResult) {
                        NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "StopGame");
                    }
                };

                DialogManager.createConfirmationDialog(
                        "Confirm experience exit",
                        "Are you sure you want to exit? Some users may require saving their progress. Please confirm this action.",
                        confirmAppExitCallback,
                        "Cancel",
                        "Confirm");
            } else {
                NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "StopGame");
            }
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

                //value hardcoded to 2 as per the CBUS requirements - only ever turns the station on
                //additionalData break down
                //Action : [cbus unit : group address : id address : value] : [type : room : id station]
                NetworkService.sendMessage("NUC",
                        "WOL",
                        station.id + ":"
                                + NetworkService.getIPAddress());

                MainActivity.runOnUI(() -> {
                    station.status = "Turning on";
                    mViewModel.updateStationById(id, station);
                });

            } else if(station.status.equals("Turning On")) {
                Toast.makeText(getContext(), "Computer is starting", Toast.LENGTH_SHORT).show();

                //Send the WOL command again, in case a user shutdown and started up to quickly
                NetworkService.sendMessage("NUC",
                        "WOL",
                        station.id + ":"
                                + NetworkService.getIPAddress());
            } else {
                if(SettingsFragment.checkAdditionalExitPrompts() && station.gameName != null) {
                    //No game is present, shutdown is okay to continue
                    if(station.gameName.length() == 0) {
                        shutdownStation(shutdownButton, id);
                    }

                    BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                        if (confirmationResult) {
                            shutdownStation(shutdownButton, id);
                        }
                    };

                    DialogManager.createConfirmationDialog(
                            "Confirm shutdown",
                            "Are you sure you want to shutdown? An experience is still running. Please confirm this action.",
                            confirmAppExitCallback,
                            "Cancel",
                            "Confirm");
                } else {
                    shutdownStation(shutdownButton, id);
                }
            }
        });

        MaterialButton configureSteamCMDButton = view.findViewById(R.id.configure_steamcmd);
        configureSteamCMDButton.setOnClickListener(v -> {
            BooleanCallbackInterface confirmConfigCallback = confirmationResult -> {
                if (confirmationResult) {
                    int id = binding.getSelectedStation().id;
                    DialogManager.steamGuardKeyEntry(id);
                }
            };

            DialogManager.createConfirmationDialog(
                    "Confirm configuration",
                    "A Steam guard key is required to be entered to configure the selected station, " +
                            "the station will not show experiences until this is complete. You must have access " +
                            "to the Steam's email account address to proceed.",
                    confirmConfigCallback,
                    "Cancel",
                    "Proceed");
        });

        ImageView gameControlImage = view.findViewById(R.id.game_control_image);
        mViewModel.getSelectedStation().observe(getViewLifecycleOwner(), station -> {
            binding.setSelectedStation(station);
            if (station.gameId != null && station.gameId.length() > 0) {

                String filePath;
                switch(station.gameType) {
                    case "Custom":
                        filePath = CustomApplication.getImageUrl(station.gameName);
                        break;
                    case "Steam":
                        filePath = SteamApplication.getImageUrl(station.gameId);
                        break;
                    case "Vive":
                        filePath = ViveApplication.getImageUrl(station.gameId);
                        break;
                    default:
                        filePath = "";
                }

                //Load the image url or a default image if nothing is available
                if(Objects.equals(filePath, "")) {
                    Glide.with(view).load(R.drawable.default_header).into(gameControlImage);
                } else {
                    Glide.with(view).load(filePath).into(gameControlImage);
                }
            } else {
                gameControlImage.setImageDrawable(null);
            }
        });

        //Open up the experience options modal
        gameControlImage.setOnClickListener(v -> {
            String gameName = mViewModel.getSelectedStation().getValue().gameName;

            Details details = null;

            for (Application application: mViewModel.getSelectedStation().getValue().applications) {
                if (Objects.equals(application.name, gameName)) {
                    details = application.details;
                }
            }

            if(details == null) return;

            DialogManager.showExperienceOptions(gameName, details);
        });
    }

    /**
     * Shutdown a station, allowing a user to cancel the command within a set period of time.
     * @param shutdownButton A Material button which has the text changed to reflect the current
     *                       shutdown status.
     * @param id An integer representing the ID of the station.
     */
    private void shutdownStation(MaterialButton shutdownButton, int id) {
        CountdownCallbackInterface shutdownCountDownCallback = seconds -> {
            if (seconds <= 0) {
                shutdownButton.setText(R.string.shut_down_station);
            } else {
                if (!cancelledShutdown) {
                    shutdownButton.setText("Cancel (" + seconds + ")");
                }
            }
        };
        if (shutdownButton.getText().toString().startsWith("Shut Down")) {
            cancelledShutdown = false;
            DialogManager.buildShutdownDialog(getContext(), new int[]{id}, shutdownCountDownCallback);
        } else {
            cancelledShutdown = true;
            String stationIdsString = String.join(", ", Arrays.stream(new int[]{id}).mapToObj(String::valueOf).toArray(String[]::new));
            NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", "CancelShutdown");
            if (DialogManager.shutdownTimer != null) {
                DialogManager.shutdownTimer.cancel();
            }
            shutdownButton.setText(R.string.shut_down_station);
        }
    }
}
