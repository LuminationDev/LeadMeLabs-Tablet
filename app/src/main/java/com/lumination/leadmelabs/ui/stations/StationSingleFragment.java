package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

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
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.handlers.StatusHandler;
import com.lumination.leadmelabs.models.stations.VrStation;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.details.Details;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.LibraryPageFragment;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.library.application.ApplicationLibraryFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.Identifier;
import com.segment.analytics.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class StationSingleFragment extends Fragment {

    public static StationsViewModel mViewModel;
    private FragmentStationSingleBinding binding;
    private static boolean cancelledShutdown = false;
    public static FragmentManager childManager;

    private LocalAudioDeviceAdapter audioDeviceAdapter;
    //Control how often idle mode can be clicked
    private boolean recentlyIdled = false;

    public static final String segmentClassification = "Station Single";

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

        //region VideoControl
        Slider stationVideoSlider = view.findViewById(R.id.station_video_slider);

        // Set custom label formatter
        stationVideoSlider.setLabelFormatter(value -> {
            // Format the hint string as "xx:xx" (minutes:seconds)
            int minutes = (int) value / 60;
            int seconds = (int) value % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        });

        stationVideoSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                Station selectedStation = binding.getSelectedStation();
                selectedStation.videoController.setSliderTracking(true);
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                Station selectedStation = binding.getSelectedStation();
                selectedStation.videoController.setVideoPlaybackTime((int) slider.getValue());
                selectedStation.videoController.setSliderTracking(false);
                Properties segmentProperties = new Properties();
                segmentProperties.put("name", "time_slider");
                trackStationEvent(SegmentConstants.Video_Playback_Control, segmentProperties);
            }
        });

        stationVideoSlider.addOnChangeListener((slider, value, fromUser) -> {
            Station selectedStation = binding.getSelectedStation();
            selectedStation.videoController.setSliderValue((int) slider.getValue());
        });
        //endregion

        //region AudioControl
        Slider stationVolumeSlider = view.findViewById(R.id.station_volume_slider);
        stationVolumeSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                Station selectedStation = binding.getSelectedStation();
                selectedStation.audioController.setVolume((int) slider.getValue());
                selectedStation.audioController.volume = (int) slider.getValue();
                mViewModel.updateStationById(selectedStation.id, selectedStation);

                //backwards compat - remove this after next update
                int currentVolume = ((long) selectedStation.audioController.audioDevices.size() == 0) ? selectedStation.audioController.volume : selectedStation.audioController.getVolume();
                NetworkService.sendMessage("Station," + selectedStation.id, "Station", "SetValue:volume:" + currentVolume);
                System.out.println(slider.getValue());
                trackStationEvent(SegmentConstants.Station_Volume_Control);
            }
        });

        if (newlySelectedStation != null) {
            setupAudioSpinner(view, newlySelectedStation);
            setupMuteButton(view);
            updateLayout(view, newlySelectedStation);
        }
        //endregion

        //region Button Setup
        // Open the help (guide) modals
        FlexboxLayout vrGuideButton = view.findViewById(R.id.guide_vr_library);
        vrGuideButton.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("VR Library", "Go to the VR library and press refresh. After approximately 1 minute, the experiences should be available in the list. If this doesn’t work, try restarting the station by shutting it down and then turning it back on. This can be done on the individual station screen.");

            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", segmentClassification);
            segmentProperties.put("name", "VR Library");
            Segment.trackEvent(SegmentConstants.Help_Question_Opened, segmentProperties);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "vr_library");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout steamErrorGuideButton = view.findViewById(R.id.guide_steam_errors);
        steamErrorGuideButton.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("SteamVR Errors", "Press ‘Restart VR System’ and wait while it restarts. This can be done on the individual station screen. Then try to launch the experience again. If this doesn’t work, try restarting the station by shutting it down and then turning it back on. This can be done on the individual station screen. If this still doesn’t work, contact your IT department.");

            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", segmentClassification);
            segmentProperties.put("name", "SteamVR Errors");
            Segment.trackEvent(SegmentConstants.Help_Question_Opened, segmentProperties);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "steam_vr_errors");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(HelpPageFragment.class, "help", null);
        });

        //TODO not for current release
        //Run the identify flow
//        FlexboxLayout identify = view.findViewById(R.id.identify_button);
//        identify.setOnClickListener(v -> {
//            List<Station> stations = Collections.singletonList(binding.getSelectedStation());
//            Identifier.identifyStations(stations);
//        });

        Button button = view.findViewById(R.id.enter_url);
        button.setOnClickListener(v ->
                DialogManager.buildURLDialog(getContext(), binding)
        );

        MaterialButton newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("station", String.valueOf(binding.getSelectedStation().name));
            StationsFragment.mViewModel.setSelectedStationId(binding.getSelectedStation().id);

            trackStationEvent(SegmentConstants.New_Session_Button);

            // Open the video library as default if the video player is active
            Application current = binding.getSelectedStation().applicationController.findCurrentApplication();
            if ((current instanceof EmbeddedApplication)) {
                String subtype = current.subtype.optString("category", "");
                if (subtype.equals(Constants.VideoPlayer)) {
                    bundle.putString("library", "videos");
                    SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
                    if (fragment == null) return;

                    fragment.loadFragment(LibraryPageFragment.class, "session", bundle);
                }
                return;
            }

            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(LibraryPageFragment.class, "session", bundle);
        });

        Button restartGame = view.findViewById(R.id.station_restart_session);
        restartGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            if(selectedStation.applicationController.getExperienceName() == null || selectedStation.applicationController.getExperienceName().isEmpty()) {
                return;
            }

            //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
            if (MainActivity.isNucJsonEnabled) {
                JSONObject message = new JSONObject();
                try {
                    message.put("Action", "Restart");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "Experience", message.toString());
            }
            else {
                NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "Experience", "Restart");
            }

            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(DashboardPageFragment.class, "dashboard", null);
            DialogManager.awaitStationApplicationLaunch(
                    new int[] { binding.getSelectedStation().id },
                    ApplicationLibraryFragment.mViewModel.getSelectedApplicationName(binding.getSelectedStation().applicationController.getExperienceId()),
                    true);

            trackExperienceEvent(SegmentConstants.Event_Experience_Restart);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(binding.getSelectedStation().id));
            }};
            FirebaseManager.logAnalyticEvent("session_restarted", analyticsAttributes);
        });

        Button endGame = view.findViewById(R.id.station_end_session);
        endGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            if(selectedStation.applicationController.getExperienceName() == null || selectedStation.applicationController.getExperienceName().isEmpty()) {
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
            trackExperienceEvent(SegmentConstants.Event_Experience_Stop);
        });

        View currentSessionBox = view.findViewById(R.id.current_session_box);
        currentSessionBox.setOnClickListener(v -> {
            trackStationEvent(SegmentConstants.Current_Session_Touch);
        });

        View stationStatusBox = view.findViewById(R.id.station_status_box);
        stationStatusBox.setOnClickListener(v -> {
            trackStationEvent(SegmentConstants.Station_Status_Touch);
        });

        //TODO not for current release
//        Button idleMode = view.findViewById(R.id.idle_mode);
//        idleMode.setOnClickListener(v -> {
//            Station selectedStation = binding.getSelectedStation();
//
//            //Disable the button for 5 seconds if going from normal to idle
//            if (recentlyIdled) {
//                DialogManager.createBasicDialog("Warning", "Please wait 5 seconds before attempting to exit Idle mode after initialising it.");
//                return;
//            }
//
//            if (selectedStation.statusHandler.isStationOn()) {
//                recentlyIdled = true;
//                new java.util.Timer().schedule(
//                        new java.util.TimerTask() {
//                            @Override
//                            public void run() {
//                                recentlyIdled = false;
//                            }
//                        },
//                        5000
//                );
//            }
//
//            String value = selectedStation.statusHandler.isStationIdle() ? "normal" : "idle";
//
//            NetworkService.sendMessage("Station," + selectedStation.id, "Station", "SetValue:idleMode:" + value);
//        });

        //TODO only for current release
        Button identify = view.findViewById(R.id.identify_button);
        identify.setOnClickListener(v -> {
            List<Station> stations = Collections.singletonList(binding.getSelectedStation());
            Identifier.identifyStations(stations);
        });

        Button restartVr = view.findViewById(R.id.station_restart_vr);
        restartVr.setOnClickListener(v -> {
            //Start flashing the VR icons
            ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).setStationFlashing(binding.getSelectedStation().id, true);
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "RestartVR");
            DialogManager.awaitStationRestartVRSystem(new int[] { binding.getSelectedStation().id });

            trackExperienceEvent(SegmentConstants.Event_Station_VR_Restart);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(binding.getSelectedStation().id));
            }};
            FirebaseManager.logAnalyticEvent("station_vr_system_restarted", analyticsAttributes);
        });

        MaterialButton shutdownButton = view.findViewById(R.id.shutdown_station);
        shutdownButton.setOnClickListener(v -> {
            int id = binding.getSelectedStation().id;
            Station station = mViewModel.getStationById(id);

            if (station.isOff()) {
                station.statusHandler.powerStatusCheck(station.getId(),3 * 1000 * 60);

                //value hardcoded to 2 as per the CBUS requirements - only ever turns the station on
                //additionalData break down
                //Action : [cbus unit : group address : id address : value] : [type : room : id station]
                NetworkService.sendMessage("NUC",
                        "WOL",
                        station.id + ":"
                                + NetworkService.getIPAddress());

                MainActivity.runOnUI(() -> {
                    station.setStatus(StatusHandler.TURNING_ON);
                    mViewModel.updateStationById(id, station);
                });
                trackStationEvent(SegmentConstants.Event_Station_Power_On);

            } else if(station.getStatus().equals(StatusHandler.TURNING_ON)) {
                Toast.makeText(getContext(), "Computer is starting", Toast.LENGTH_SHORT).show();

                //Send the WOL command again, in case a user shutdown and started up to quickly
                NetworkService.sendMessage("NUC",
                        "WOL",
                        station.id + ":"
                                + NetworkService.getIPAddress());
            } else {
                if(SettingsFragment.checkAdditionalExitPrompts() && station.applicationController.getExperienceName() != null) {
                    //No game is present, shutdown is okay to continue
                    if(station.applicationController.getExperienceName().isEmpty()) {
                        shutdownStation(shutdownButton, id);
                        return;
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

                    trackStationEvent(SegmentConstants.Event_Station_Shutdown);
                }
            }
        });

        MaterialButton experienceDetailsButton = view.findViewById(R.id.control_experience);
        experienceDetailsButton.setOnClickListener(v -> {
            if (mViewModel.getSelectedStation().getValue() == null) {
                return;
            }

            String gameName = mViewModel.getSelectedStation().getValue().applicationController.getExperienceName();

            Details details = null;

            for (Application application: mViewModel.getSelectedStation().getValue().applicationController.applications) {
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
            trackStationEvent(SegmentConstants.Open_Configure_SteamCMD);
        });

        MaterialButton headsetVolumeSelect = view.findViewById(R.id.headset_volume_select);
        headsetVolumeSelect.setOnClickListener(v -> {
            if (mViewModel.getSelectedStation().getValue() == null || !mViewModel.getSelectedStation().getValue().audioController.hasAudioDevice(LocalAudioDevice.headsetAudioDeviceNames)) {
                return;
            }

            LocalAudioDevice selectedValue = mViewModel.getSelectedStation().getValue().audioController.findAudioDevice(LocalAudioDevice.headsetAudioDeviceNames);

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

            mViewModel.getSelectedStation().getValue().audioController.setActiveAudioDevice(selectedValue.getName());

            NetworkService.sendMessage("Station," + mViewModel.getSelectedStation().getValue().id, "Station", "SetValue:activeAudioDevice:" + selectedValue.getName());

            Properties segmentProperties = new Properties();
            segmentProperties.put("name", selectedValue.getName());
            segmentProperties.put("element", "Headset Source");
            trackStationEvent(SegmentConstants.Volume_Source_Select, segmentProperties);
        });

        MaterialButton projectorVolumeSelect = view.findViewById(R.id.projector_volume_select);
        projectorVolumeSelect.setOnClickListener(v -> {
            if (mViewModel.getSelectedStation().getValue() == null || !mViewModel.getSelectedStation().getValue().audioController.hasAudioDevice(LocalAudioDevice.projectorAudioDeviceNames)) {
                return;
            }

            LocalAudioDevice selectedValue = mViewModel.getSelectedStation().getValue().audioController.findAudioDevice(LocalAudioDevice.projectorAudioDeviceNames);

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

            mViewModel.getSelectedStation().getValue().audioController.setActiveAudioDevice(selectedValue.getName());

            NetworkService.sendMessage("Station," + mViewModel.getSelectedStation().getValue().id, "Station", "SetValue:activeAudioDevice:" + selectedValue.getName());
            Properties segmentProperties = new Properties();
            segmentProperties.put("name", selectedValue.getName());
            segmentProperties.put("element", "Projector Source");
            trackStationEvent(SegmentConstants.Volume_Source_Select, segmentProperties);
        });
        //endregion

        mViewModel.getSelectedStation().observe(getViewLifecycleOwner(), station -> {
            binding.setSelectedStation(station);
            updateExperienceImage(view, station);
            setupAudioSpinner(view, station);
            updateLayout(view, station);
        });
    }

    private void setupMuteButton(View view) {
        MaterialButton muteButton = view.findViewById(R.id.station_mute);
        muteButton.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            boolean currentValue = selectedStation.audioController.getMuted();
            selectedStation.audioController.setMuted(!currentValue);
            mViewModel.updateStationById(selectedStation.id, selectedStation);
            NetworkService.sendMessage("Station," + selectedStation.id, "Station", "SetValue:muted:" + selectedStation.audioController.getMuted());
            trackStationEvent(SegmentConstants.Station_Mute);
        });
    }

    private void setupAudioSpinner(View view, Station station) {
        // Create/Update the custom adapter if it's different from the existing one based on names
        if (this.audioDeviceAdapter != null) {
            List<String> currentNames = getDeviceNames(audioDeviceAdapter.getAudioDevices());
            List<String> newNames = getDeviceNames(station.audioController.audioDevices);

            if (currentNames.equals(newNames)) {
                return;
            }
        }

        // Create a custom adapter
        audioDeviceAdapter = new LocalAudioDeviceAdapter(getContext(), station.audioController.audioDevices);

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
        if (station.audioController.getActiveAudioDevice() != null) {
            String activeName = station.audioController.getActiveAudioDevice().getName();

            for (int i = 0; i < audioDeviceAdapter.getCount(); i++) {
                LocalAudioDevice device = audioDeviceAdapter.getItem(i);

                if (device != null && device.getName().equals(activeName)) {
                    position = i;
                    break;
                }
            }
        }

        // Do not attempt to set the listeners if there is nothing to attach them to
        if (audioDeviceAdapter.getCount() == 0) return;

        // Set the selection if the item was found
        spinner.setSelection(position);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                LocalAudioDevice selectedValue = (LocalAudioDevice) parentView.getItemAtPosition(position);

                NetworkService.sendMessage("Station," + station.id, "Station", "SetValue:activeAudioDevice:" + selectedValue.getName());
                Properties segmentProperties = new Properties();
                segmentProperties.put("name", selectedValue.getName());
                segmentProperties.put("element", "Other Sources");
                trackStationEvent(SegmentConstants.Volume_Source_Select, segmentProperties);
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

    //region Layout Control
    /**
     * If the selected Station is of type VirtualStation, inflate the Vr devices stub layout. The
     * main fragment_station_single.xml passes the current selectedStation binding down to the stub
     * as to pass on the data binding from the VirtualStation class.
     */
    private void inflateVRDevicesLayout() {
        ViewStub vrDevicesStub = binding.getRoot().findViewById(R.id.vr_devices_section);
        vrDevicesStub.inflate();
        this.setupVrDeviceClickTracking(binding.getRoot().findViewById(R.id.headset_connection_status), "Headset Connection");
        this.setupVrDeviceClickTracking(binding.getRoot().findViewById(R.id.left_controller_status), "Left Controller");
        this.setupVrDeviceClickTracking(binding.getRoot().findViewById(R.id.right_controller_status), "Right Controller");
        this.setupVrDeviceClickTracking(binding.getRoot().findViewById(R.id.base_station_status), "Base Station");
        this.setupVrDeviceClickTracking(binding.getRoot().findViewById(R.id.vive_connection_status), "Vive Connection");
        this.setupVrDeviceClickTracking(binding.getRoot().findViewById(R.id.openvr_connection_status), "OpenVR Connection");
    }

    private void setupVrDeviceClickTracking(View view, String name) {
        if (view != null) {
            view.setOnTouchListener((v, x) -> {
                Properties segmentProperties = new Properties();
                segmentProperties.put("name", name);
                trackStationEvent(SegmentConstants.VR_Device_Touch, segmentProperties);
                return false;
            });
        }
    }

    /**
     * Update the experience image to reflect what the selected Station is currently processing.
     * @param view The current fragment view.
     * @param station The currently selected Station.
     */
    private void updateExperienceImage(View view, Station station) {
        if (Helpers.isNullOrEmpty(station.applicationController.getExperienceType()) || Helpers.isNullOrEmpty(station.applicationController.getExperienceId()) || Helpers.isNullOrEmpty(station.applicationController.getExperienceName())) {
            ImageView experienceControlImage = view.findViewById(R.id.placeholder_image);
            experienceControlImage.setImageDrawable(null);
        } else {
            Helpers.setExperienceImage(station.applicationController.getExperienceType(), station.applicationController.getExperienceName(), station.applicationController.getExperienceId(), view);
        }

        // Add an on click listener to the image if the video player is active
        Application current = station.applicationController.findCurrentApplication();
        if (!(current instanceof EmbeddedApplication)) {
            resetLayout(view);
            return;
        }
        String subtype = current.subtype.optString("category", "");
        if (subtype.equals(Constants.VideoPlayer)) {
            ImageView experienceControlImage = view.findViewById(R.id.placeholder_image);
            experienceControlImage.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("station", String.valueOf(binding.getSelectedStation().name));
                StationsFragment.mViewModel.setSelectedStationId(binding.getSelectedStation().id);
                bundle.putString("library", "videos");
                SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
                if (fragment == null) return;

                fragment.loadFragment(LibraryPageFragment.class, "session", bundle);
            });
        }
    }

    /**
     * Depending on the experience that is being play update the layout to show the correct controls.
     */
    private void updateLayout(View view, Station station) {
        // Check the current experience
        Application current = station.applicationController.findCurrentApplication();

        if (!(current instanceof EmbeddedApplication)) {
            resetLayout(view);
            return;
        }

        String subtype = current.subtype.optString("category", "");
        if (subtype.isEmpty()) {
            resetLayout(view);
            return;
        }

        if (subtype.equals(Constants.ShareCode)) {
            Log.e("STATION", "SHARE CODE LAYOUT");
        }
        else if (subtype.equals(Constants.VideoPlayer)) {
            TextView controlTitle = view.findViewById(R.id.custom_controls_title);
            controlTitle.setText("Playback");

            FlexboxLayout guides = view.findViewById(R.id.guide_section);
            guides.setVisibility(View.GONE);

            FlexboxLayout controls = view.findViewById(R.id.video_controls);
            controls.setVisibility(View.VISIBLE);
        } else {
            resetLayout(view);
        }
    }

    /**
     * Convert the layout back to it's original view (No video or share code controls present).
     */
    private void resetLayout(View view) {
        //Reset the control title
        TextView controlTitle = view.findViewById(R.id.custom_controls_title);
        controlTitle.setText("Guides");

        //Reset the video controls
        FlexboxLayout controls = view.findViewById(R.id.video_controls);
        controls.setVisibility(View.GONE);

        //Reset the share code controls

        //Reset the guide section
        FlexboxLayout guides = view.findViewById(R.id.guide_section);
        guides.setVisibility(View.VISIBLE);
    }
    //endregion

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
                    shutdownButton.setText(MessageFormat.format("Cancel ({0})", seconds));
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

    private void trackExperienceEvent(String eventConstant) {
        Station selectedStation = binding.getSelectedStation();

        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("stationId", selectedStation.getId());
        segmentProperties.put("name", selectedStation.applicationController.getExperienceName());
        segmentProperties.put("id", selectedStation.applicationController.getExperienceId());
        segmentProperties.put("type", selectedStation.applicationController.getExperienceType());

        Segment.trackEvent(eventConstant, segmentProperties);
    }

    private void trackStationEvent(String eventConstant) {
        Station selectedStation = binding.getSelectedStation();

        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("stationId", selectedStation.getId());

        Segment.trackEvent(eventConstant, segmentProperties);
    }

    private void trackStationEvent(String eventConstant, Properties properties) {
        Station selectedStation = binding.getSelectedStation();

        properties.put("classification", segmentClassification);
        properties.put("stationId", selectedStation.getId());

        Segment.trackEvent(eventConstant, properties);
    }
}
