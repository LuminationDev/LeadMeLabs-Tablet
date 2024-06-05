package com.lumination.leadmelabs.unique.snowHydro.stations;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleNestedBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.LocalAudioDevice;
import com.lumination.leadmelabs.models.Option;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.handlers.StatusHandler;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.pages.LibraryPageFragment;
import com.lumination.leadmelabs.ui.library.application.ApplicationLibraryFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.LocalAudioDeviceAdapter;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.unique.snowHydro.modal.ModalDialogFragment;
import com.lumination.leadmelabs.unique.snowHydro.modal.backdrop.BackdropAdapter;
import com.lumination.leadmelabs.unique.snowHydro.modal.backdrop.BackdropFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.Interlinking;
import com.segment.analytics.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * This class is designed specifically for the Snowy Hydro project. The single page handles a
 * primary computer (The Wall PC) and a bound computer (The Floor PC). There are many similarities
 * between this class and the StationSingleFragment but for ease of use this class is kept completely
 * separate.
 */
public class StationSingleNestedFragment extends Fragment {
    public static StationsViewModel mViewModel;
    public static BackdropAdapter localBackdropAdapter;

    public FragmentStationSingleNestedBinding binding;
    private static boolean cancelledShutdown = false;
    public static int primaryStationId = 0; //track how the side menu's back action is handled

    public static FragmentManager childManager;

    private LocalAudioDeviceAdapter audioDeviceAdapter;

    public static final String segmentClassification = "Station Single Nested";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_station_single_nested, container, false);
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

        if (newlySelectedStation != null) {
            primaryStationId = newlySelectedStation.getId();

            // Set up the nested Station
            Station nestedStation = newlySelectedStation.getFirstNestedStationOrNull();
            if (nestedStation != null) {
                binding.setSelectedNestedStation(nestedStation);

                // Set the adapter for backdrops
                GridView backdropGridView = view.findViewById(R.id.backdrop_grid);
                localBackdropAdapter = new BackdropAdapter(getContext(), true);
                localBackdropAdapter.backdropList = (ArrayList<Video>) nestedStation.fileController.getVideosOfType(Constants.VIDEO_TYPE_BACKDROP);
                backdropGridView.setAdapter(localBackdropAdapter);
            }
        }

        //region Button Setup
        // Open to the nested Station screen
        FlexboxLayout nestedStationButton = view.findViewById(R.id.nested_station_button);
        nestedStationButton.setOnClickListener(v -> {
            if (binding.getSelectedNestedStation() == null) return;
            MainActivity.getInstance().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out)
                    .replace(R.id.main, StationSingleFragment.class, null)
                    .addToBackStack("menu:stations:nested")
                    .commit();
            Segment.trackScreen("menu:stations:nested");

            // Delay the change over so the nested xml section does not switch
            new Handler().postDelayed(() -> mViewModel.selectStation(binding.getSelectedNestedStation().id), 200);
            SideMenuFragment.currentType = "stationSingle";
        });

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(HelpPageFragment.class, "help", null);
        });

        FlexboxLayout seeMoreButton = view.findViewById(R.id.open_modal_text);
        seeMoreButton.setOnClickListener(v -> {
            mViewModel.setLayoutTab("backdrops");

            ModalDialogFragment modalFragment = new ModalDialogFragment();
            modalFragment.show(MainActivity.getInstance().getSupportFragmentManager(), "modalFragmentTag");
        });

        MaterialButton newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("station", String.valueOf(binding.getSelectedStation().name));
            StationsFragment.mViewModel.setSelectedStationId(binding.getSelectedStation().id);

            trackStationEvent(SegmentConstants.New_Session_Button);

            // Open the video library as default if the video player is active
            Application current = binding.getSelectedStation().applicationController.findCurrentApplication();
            if ((current instanceof EmbeddedApplication)) {
                String subtype = current.HasCategory();
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

        Button restartExperience = view.findViewById(R.id.station_restart_session);
        restartExperience.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            if(selectedStation.applicationController.getExperienceName() == null || selectedStation.applicationController.getExperienceName().equals("")) {
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
                NetworkService.sendMessage("Station," +  Interlinking.collectNestedStations(selectedStation, String.class), "Experience", message.toString());
            }
            else {
                NetworkService.sendMessage("Station," +  Interlinking.collectNestedStations(selectedStation, String.class), "Experience", "Restart");
            }

            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(DashboardPageFragment.class, "dashboard", null);
            DialogManager.awaitStationApplicationLaunch(
                    Interlinking.collectNestedStations(selectedStation, int[].class),
                    ApplicationLibraryFragment.mViewModel.getSelectedApplicationName(selectedStation.applicationController.getExperienceId()),
                    true);

            trackExperienceEvent(SegmentConstants.Event_Experience_Restart);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(selectedStation.getId()));
            }};
            FirebaseManager.logAnalyticEvent("session_restarted", analyticsAttributes);
        });

        View currentSessionBox = view.findViewById(R.id.current_session_box);
        currentSessionBox.setOnClickListener(v -> {
            trackStationEvent(SegmentConstants.Current_Session_Touch);
        });

        View stationStatusBox = view.findViewById(R.id.station_status_box);
        stationStatusBox.setOnClickListener(v -> {
            trackStationEvent(SegmentConstants.Station_Status_Touch);
        });

        Button endGame = view.findViewById(R.id.station_end_session);
        endGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            if(selectedStation.applicationController.getExperienceName() == null || selectedStation.applicationController.getExperienceName().equals("")) {
                return;
            }

            if(SettingsFragment.checkAdditionalExitPrompts()) {
                BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                    if (confirmationResult) {
                        NetworkService.sendMessage("Station," + Interlinking.collectNestedStations(selectedStation, String.class), "CommandLine", "StopGame");
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
                NetworkService.sendMessage("Station," + Interlinking.collectNestedStations(selectedStation, String.class), "CommandLine", "StopGame");
            }
        });

        Button changeLayout = view.findViewById(R.id.change_layout);
        changeLayout.setOnClickListener(v -> {
            mViewModel.setLayoutTab("layouts");

            ModalDialogFragment modalFragment = new ModalDialogFragment();
            modalFragment.show(MainActivity.getInstance().getSupportFragmentManager(), "modalFragmentTag");
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
            String joinedStations = Interlinking.collectNestedStations(station, String.class);

            if (station.isOff()) {
                station.statusHandler.powerStatusCheck(station.getId(),3 * 1000 * 60);

                //value hardcoded to 2 as per the CBUS requirements - only ever turns the station on
                //additionalData break down
                //Action : [cbus unit : group address : id address : value] : [type : room : id station]
                NetworkService.sendMessage("NUC",
                        "WOL",
                        joinedStations + ":"
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
                        joinedStations + ":"
                                + NetworkService.getIPAddress());
            } else {
                if(SettingsFragment.checkAdditionalExitPrompts() && station.applicationController.getExperienceName() != null) {
                    //No game is present, shutdown is okay to continue
                    if(station.applicationController.getExperienceName().length() == 0) {
                        shutdownStation(shutdownButton, station);
                        return;
                    }

                    BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                        if (confirmationResult) {
                            shutdownStation(shutdownButton, station);
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
                    shutdownStation(shutdownButton, station);

                    trackStationEvent(SegmentConstants.Event_Station_Shutdown);
                }
            }
        });
        //endregion

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

                //This is only sent to the primary Station as it is assumed that the bound Station does not have sounds required
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

        /*
         * Observes changes in the list of stations and updates the UI accordingly.
         * If the selected station or the list of stations is null, no action is taken.
         * For each station in the list, if it is a nested station of the selected primary station,
         * it updates the UI and notifies the backdrop adapters.
         */
        mViewModel.getStations().observe(getViewLifecycleOwner(), stations -> {
            Station primary = mViewModel.getSelectedStation().getValue();
            if (primary == null || stations == null) return;

            stations.forEach(station -> {
                //Update the primary station
                if (primary.getId() == station.getId()) {
                    binding.setSelectedStation(station);
                    updateExperienceImage(view, station);
                    setupAudioSpinner(view, station);
                    updateLayout(view, station);
                }
                //Update any nested stations
                if (primary.nestedStations.contains(station.getId())) {
                    binding.setSelectedNestedStation(primary.getFirstNestedStationOrNull());
                    if (BackdropFragment.localBackdropAdapter != null) {
                        BackdropFragment.localBackdropAdapter.backdropList = (ArrayList<Video>) primary.getFirstNestedStationOrNull().fileController.getVideosOfType(Constants.VIDEO_TYPE_BACKDROP);
                        BackdropFragment.localBackdropAdapter.notifyDataSetChanged();
                    }
                    if (localBackdropAdapter != null) {
                        localBackdropAdapter.backdropList = (ArrayList<Video>) primary.getFirstNestedStationOrNull().fileController.getVideosOfType(Constants.VIDEO_TYPE_BACKDROP);
                        localBackdropAdapter.notifyDataSetChanged();

                        //Update the sections visibility
                        if (localBackdropAdapter.backdropList.isEmpty()) {
                            view.findViewById(R.id.backdrop_grid).setVisibility(View.GONE);
                            view.findViewById(R.id.backdrop_empty_state).setVisibility(View.VISIBLE);
                        } else {
                            view.findViewById(R.id.backdrop_grid).setVisibility(View.VISIBLE);
                            view.findViewById(R.id.backdrop_empty_state).setVisibility(View.GONE);
                        }
                    }
                }
            });
        });
    }

    /**
     * If the selected Station is of type VirtualStation, inflate the Vr devices stub layout. The
     * main fragment_station_single.xml passes the current selectedStation binding down to the stub
     * as to pass on the data binding from the VirtualStation class.
     */

    //region AudioControl
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
    //endregion

    //region Layout Control
    /**
     * Update the experience image to reflect what the selected Station is currently processing.
     * @param view The current fragment view.
     * @param station The currently selected Station.
     */
    private void updateExperienceImage(View view, Station station) {
        if (Helpers.isNullOrEmpty(station.applicationController.getExperienceType()) || Helpers.isNullOrEmpty(station.applicationController.getExperienceId()) || Helpers.isNullOrEmpty(station.applicationController.getExperienceName())) {
            ImageView experienceControlImage = view.findViewById(R.id.novastar_image);

            Appliance ledWall = null;

            List<Appliance> allAppliances = ApplianceFragment.mViewModel.getAppliances().getValue();
            if (allAppliances == null) return;

            //NOTE: Currently there is only ever 1 instance of a LED wall.
            for (Appliance appliance : allAppliances) {
                if (appliance.matchesDisplayCategory(Constants.LED_WALLS)) {
                    ledWall = appliance;
                    break;
                }
            }

            if (ledWall == null) {
                experienceControlImage.setImageDrawable(null);
                return;
            }

            for (Option option : ledWall.options) {
                if (Objects.equals(option.id, ledWall.value)) {
                    Helpers.SetOptionImage(option.getName(), view.findViewById(R.id.novastar_image), view);
                    return;
                }
            }

            //Backup for when options have not loaded yet
            Helpers.SetOptionImage("", view.findViewById(R.id.novastar_image), view);
        } else {
            Helpers.setExperienceImage(station.applicationController.getExperienceType(), station.applicationController.getExperienceName(), station.applicationController.getExperienceId(), view);
        }

        // Add an on click listener to the image if the video player is active
        Application current = station.applicationController.findCurrentApplication();
        if (!(current instanceof EmbeddedApplication)) {
            resetLayout(view);
            return;
        }
        String subtype = current.HasCategory();
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

        String subtype = current.HasCategory();
        if (subtype.isEmpty()) {
            resetLayout(view);
            return;
        }

        if (subtype.equals(Constants.ShareCode)) {
            Log.e("STATION", "SHARE CODE LAYOUT");
        }
        else if (subtype.equals(Constants.VideoPlayer)) {
            TextView controlTitle = view.findViewById(R.id.custom_controls_title);
            controlTitle.setText(R.string.playback_title);

            RelativeLayout guides = view.findViewById(R.id.backdrop_section);
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
        controlTitle.setText(R.string.backdrop_title);

        //Reset the video controls
        FlexboxLayout controls = view.findViewById(R.id.video_controls);
        controls.setVisibility(View.GONE);

        //Reset the share code controls

        //Reset the guide section
        RelativeLayout guides = view.findViewById(R.id.backdrop_section);
        guides.setVisibility(View.VISIBLE);
    }
    //endregion

    /**
     * Shutdown a station, allowing a user to cancel the command within a set period of time.
     * @param shutdownButton A Material button which has the text changed to reflect the current
     *                       shutdown status.
     * @param station A station object that is going to be shutdown
     */
    private void shutdownStation(MaterialButton shutdownButton, Station station) {
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
            DialogManager.buildShutdownOrRestartDialog(getContext(), "Shutdown", Interlinking.collectNestedStations(station, int[].class), shutdownCountDownCallback);
        } else {
            cancelledShutdown = true;
            String stationIdsString = Interlinking.collectNestedStations(station, String.class);
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
