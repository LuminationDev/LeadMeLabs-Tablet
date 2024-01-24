package com.lumination.leadmelabs.ui.stations;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.LocalAudioDevice;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.VrStation;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.ReviveApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;
import com.lumination.leadmelabs.models.applications.details.Details;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.application.ApplicationSelectionFragment;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.utilities.Identifier;

import java.util.ArrayList;
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

    private LocalAudioDeviceAdapter audioDeviceAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_station_single, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setStations(mViewModel);

        //Specifically set the selected station
        Station newlySelectedStation = mViewModel.getSelectedStation().getValue();
        binding.setSelectedStation(newlySelectedStation);

        // Inflate and Bind the VR devices layout if the selected station is a VirtualStation
        if (newlySelectedStation instanceof VrStation) {
            inflateVRDevicesLayout();
        }

        if (savedInstanceState == null) {
            childManager.beginTransaction()
                    .replace(R.id.logo, LogoFragment.class, null)
                    .commitNow();
        }

        //region AudioControl
        Slider stationVolumeSlider = view.findViewById(R.id.station_volume_slider);
        stationVolumeSlider.addOnSliderTouchListener(touchListener);

        if (newlySelectedStation != null) {
            setupAudioSpinner(view, newlySelectedStation);
            setupMuteButton(view);
        }
        //endregion

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            ((SideMenuFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(HelpPageFragment.class, "help", null);
        });

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

        MaterialButton newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("station", String.valueOf(binding.getSelectedStation().name));
            ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(ApplicationSelectionFragment.class, "session", bundle);
            ApplicationSelectionFragment.setStationId(binding.getSelectedStation().id);
        });

        Button restartGame = view.findViewById(R.id.station_restart_session);
        restartGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            if(selectedStation.gameName == null || selectedStation.gameName.equals("")) {
                return;
            }

            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "Experience", "Restart");

            ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(DashboardPageFragment.class, "dashboard", null);
            DialogManager.awaitStationGameLaunch(new int[] { binding.getSelectedStation().id }, ApplicationSelectionFragment.mViewModel.getSelectedApplicationName(binding.getSelectedStation().gameId), true);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(binding.getSelectedStation().id));
            }};
            FirebaseManager.logAnalyticEvent("session_restarted", analyticsAttributes);
        });

        Button restartVr = view.findViewById(R.id.station_restart_vr);
        restartVr.setOnClickListener(v -> {
            //Start flashing the VR icons
            ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).setStationFlashing(binding.getSelectedStation().id, true);
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "RestartVR");
            DialogManager.awaitStationRestartVRSystem(new int[] { binding.getSelectedStation().id });
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
                        "Confirm",
                        false);
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
                station.powerStatusCheck(3 * 1000 * 60);

                //value hardcoded to 2 as per the CBUS requirements - only ever turns the station on
                //additionalData break down
                //Action : [cbus unit : group address : id address : value] : [type : room : id station]
                NetworkService.sendMessage("NUC",
                        "WOL",
                        station.id + ":"
                                + NetworkService.getIPAddress());

                MainActivity.runOnUI(() -> {
                    station.status = "Turning On";
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
                            "Confirm",
                            false);
                } else {
                    shutdownStation(shutdownButton, id);
                }
            }
        });

        MaterialButton experienceDetailsButton = view.findViewById(R.id.control_experience);
        experienceDetailsButton.setOnClickListener(v -> {
            String gameName = mViewModel.getSelectedStation().getValue().gameName;

            Details details = null;

            for (Application application: mViewModel.getSelectedStation().getValue().applications) {
                if (Objects.equals(application.name, gameName)) {
                    details = application.details;
                }
            }

            if(details == null) {
                Toast.makeText(getContext(), "No details exist for this experience.", Toast.LENGTH_SHORT).show();
                return;
            }

            DialogManager.showExperienceOptions(gameName, details);
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
                    "Proceed",
                    false);
        });

        MaterialButton headsetVolumeSelect = view.findViewById(R.id.headset_volume_select);
        headsetVolumeSelect.setOnClickListener(v -> {
            if (!mViewModel.getSelectedStation().getValue().HasAudioDevice(LocalAudioDevice.headsetAudioDeviceNames)) {
                return;
            }

            LocalAudioDevice selectedValue = mViewModel.getSelectedStation().getValue().FindAudioDevice(LocalAudioDevice.headsetAudioDeviceNames);

            Spinner deviceSelection = view.findViewById(R.id.audio_spinner);
            int position = 0;  // Initialize with a value that indicates item not found
            String activeName = selectedValue.getName();

            for (int i = 0; i < audioDeviceAdapter.getCount(); i++) {
                LocalAudioDevice device = audioDeviceAdapter.getItem(i);

                if (device != null && device.getName().equals(activeName)) {
                    position = i;
                    break;
                }
            }

            // Set the selection if the item was found
            deviceSelection.setSelection(position);

            mViewModel.getSelectedStation().getValue().SetActiveAudioDevice(selectedValue.getName());

            NetworkService.sendMessage("Station," + mViewModel.getSelectedStation().getValue().id, "Station", "SetValue:activeAudioDevice:" + selectedValue.getName());
        });

        MaterialButton projectorVolumeSelect = view.findViewById(R.id.projector_volume_select);
        projectorVolumeSelect.setOnClickListener(v -> {
            if (!mViewModel.getSelectedStation().getValue().HasAudioDevice(LocalAudioDevice.projectorAudioDeviceNames)) {
                return;
            }

            LocalAudioDevice selectedValue = mViewModel.getSelectedStation().getValue().FindAudioDevice(LocalAudioDevice.projectorAudioDeviceNames);

            Spinner deviceSelection = view.findViewById(R.id.audio_spinner);
            int position = 0;  // Initialize with a value that indicates item not found
            String activeName = selectedValue.getName();

            for (int i = 0; i < audioDeviceAdapter.getCount(); i++) {
                LocalAudioDevice device = audioDeviceAdapter.getItem(i);

                if (device != null && device.getName().equals(activeName)) {
                    position = i;
                    break;
                }
            }

            // Set the selection if the item was found
            deviceSelection.setSelection(position);

            mViewModel.getSelectedStation().getValue().SetActiveAudioDevice(selectedValue.getName());

            NetworkService.sendMessage("Station," + mViewModel.getSelectedStation().getValue().id, "Station", "SetValue:activeAudioDevice:" + selectedValue.getName());
        });

        ImageView experienceControlImage = view.findViewById(R.id.game_control_image);
        mViewModel.getSelectedStation().observe(getViewLifecycleOwner(), station -> {
            binding.setSelectedStation(station);
            updateExperienceImage(view, experienceControlImage, station);
            setupAudioSpinner(view, station);
        });
    }

    private void setupMuteButton(View view) {
        MaterialButton muteButton = view.findViewById(R.id.station_mute);
        muteButton.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            boolean currentValue = selectedStation.GetMuted();
            selectedStation.SetMuted(!currentValue);
            mViewModel.updateStationById(selectedStation.id, selectedStation);
            NetworkService.sendMessage("Station," + selectedStation.id, "Station", "SetValue:muted:" + selectedStation.GetMuted());
        });
    }

    private final Slider.OnSliderTouchListener touchListener =
        new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                Station selectedStation = binding.getSelectedStation();
                selectedStation.SetVolume((int) slider.getValue());
                selectedStation.volume = (int) slider.getValue();
                mViewModel.updateStationById(selectedStation.id, selectedStation);

                //backwards compat - remove this after next update
                int currentVolume = ((long) selectedStation.audioDevices.size() == 0) ? selectedStation.volume : selectedStation.GetVolume();
                NetworkService.sendMessage("Station," + selectedStation.id, "Station", "SetValue:volume:" + currentVolume);
                System.out.println(slider.getValue());
            }
        };

    private void setupAudioSpinner(View view, Station station) {
        // Create/Update the custom adapter if it's different from the existing one based on names
        if (this.audioDeviceAdapter != null) {
            List<String> currentNames = getDeviceNames(audioDeviceAdapter.getAudioDevices());
            List<String> newNames = getDeviceNames(station.audioDevices);

            if (currentNames.equals(newNames)) {
                return;
            }
        }

        // Create a custom adapter
        audioDeviceAdapter = new LocalAudioDeviceAdapter(getContext(), station.audioDevices);

        // Get the Spinner reference from the layout
        Spinner spinner = view.findViewById(R.id.audio_spinner);
        FlexboxLayout spinnerContainer = view.findViewById(R.id.audio_spinner_container);
        spinnerContainer.setOnClickListener(v -> {
            spinner.performClick();
        });

        // Set the custom adapter to the Spinner
        spinner.setAdapter(audioDeviceAdapter);

        // Set the selected to the active (if it exists)
        int position = 0;  // Initialize with a value that indicates item not found
        if (station.GetActiveAudioDevice() != null) {
            String activeName = station.GetActiveAudioDevice().getName();

            for (int i = 0; i < audioDeviceAdapter.getCount(); i++) {
                LocalAudioDevice device = audioDeviceAdapter.getItem(i);

                if (device != null && device.getName().equals(activeName)) {
                    position = i;
                    break;
                }
            }
        }

        // Set the selection if the item was found
        spinner.setSelection(position);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                LocalAudioDevice selectedValue = (LocalAudioDevice) parentView.getItemAtPosition(position);

                NetworkService.sendMessage("Station," + station.id, "Station", "SetValue:activeAudioDevice:" + selectedValue.getName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });
    }

    // Helper method to extract names from a list of LocalAudioDevice objects
    private List<String> getDeviceNames(List<LocalAudioDevice> devices) {
        List<String> names = new ArrayList<>();
        for (LocalAudioDevice device : devices) {
            if (device != null) {
                names.add(device.getName());
            }
        }
        return names;
    }

    /**
     * If the selected Station is of type VirtualStation, inflate the Vr devices stub layout. The
     * main fragment_station_single.xml passes the current selectedStation binding down to the stub
     * as to pass on the data binding from the VirtualStation class.
     */
    private void inflateVRDevicesLayout() {
        ViewStub vrDevicesStub = binding.getRoot().findViewById(R.id.vr_devices_stub);
        vrDevicesStub.inflate();
    }

    /**
     * Update the experience image to reflect what the selected Station is currently processing.
     * @param view The current fragment view.
     * @param experienceControlImage The ImageView to be changed.
     * @param station The currently selected Station.
     */
    private void updateExperienceImage(View view, ImageView experienceControlImage, Station station) {
        if (station.gameId != null && station.gameId.length() > 0) {
            String filePath;
            switch(station.gameType) {
                case "Custom":
                    filePath = CustomApplication.getImageUrl(station.gameName);
                    break;
                case "Steam":
                    filePath = SteamApplication.getImageUrl(station.gameName, station.gameId);
                    break;
                case "Vive":
                    filePath = ViveApplication.getImageUrl(station.gameId);
                    break;
                case "Revive":
                    filePath = ReviveApplication.getImageUrl(station.gameId);
                    break;
                default:
                    filePath = "";
            }

            //Load the image url or a default image if nothing is available
            if(Objects.equals(filePath, "")) {
                Glide.with(view).load(R.drawable.default_header).into(experienceControlImage);
            } else {
                Glide.with(view).load(filePath)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                // Error occurred while loading the image, change the imageUrl to the fallback image
                                Glide.with(view)
                                        .load(R.drawable.default_header)
                                        .into((ImageView) view.findViewById(R.id.experience_image));
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                // Image loaded successfully
                                return false;
                            }
                        })
                        .into(experienceControlImage);
            }
        } else {
            experienceControlImage.setImageDrawable(null);
        }
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
            DialogManager.buildShutdownOrRestartDialog(getContext(), "Shutdown", new int[]{id}, shutdownCountDownCallback);
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
