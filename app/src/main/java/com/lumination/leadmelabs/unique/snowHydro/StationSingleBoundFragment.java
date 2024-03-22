package com.lumination.leadmelabs.unique.snowHydro;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
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
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBoundBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.LocalAudioDevice;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.classes.SegmentExperienceEvent;
import com.lumination.leadmelabs.segment.classes.SegmentHelpEvent;
import com.lumination.leadmelabs.segment.classes.SegmentStationEvent;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * This class is designed specifically for the Snowy Hydro project. The single page handles a
 * primary computer (The Wall PC) and a bound computer (The Floor PC). There are many similarities
 * between this class and the StationSingleFragment but for ease of use this class is kept completely
 * separate.
 */
public class StationSingleBoundFragment extends Fragment {
    public static StationsViewModel mViewModel;
    public static BackdropAdapter localBackdropAdapter;

    public FragmentStationSingleBoundBinding binding;
    private static boolean cancelledShutdown = false;
    public static FragmentManager childManager;

    private LocalAudioDeviceAdapter audioDeviceAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_station_single_bound, container, false);
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
            // Set up the nested Station
            binding.setSelectedNestedStation(newlySelectedStation.getFirstNestedStationOrNull());

            // Set the adapter for backdrops
            GridView backdropGridView = view.findViewById(R.id.backdrop_section);
            localBackdropAdapter = new BackdropAdapter(getContext(), true);
            localBackdropAdapter.backdropList = (ArrayList<Video>) newlySelectedStation.videoController.getVideosOfType(Constants.VideoTypeBackdrop);
            backdropGridView.setAdapter(localBackdropAdapter);
        }

        //region Button Setup
        // Open to the nested Station screen
        FlexboxLayout nestedStationButton = view.findViewById(R.id.nested_station_button);
        nestedStationButton.setOnClickListener(v -> {
            if (binding.getSelectedNestedStation() == null) return;

            mViewModel.selectStation(binding.getSelectedNestedStation().id);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out)
                    .replace(R.id.main, StationSingleFragment.class, null)
                    .addToBackStack("menu:dashboard:stationSingle")
                    .commit();
            SideMenuFragment.currentType = "stationSingle";
        });

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            ((SideMenuFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(HelpPageFragment.class, "help", null);
            // Send data to Segment
            SegmentHelpEvent event = new SegmentHelpEvent(SegmentConstants.Event_Help_Page_Accessed, "Station Single Page");
            Segment.trackAction(SegmentConstants.Event_Type_Help, event);
        });

        FlexboxLayout seeMoreButton = view.findViewById(R.id.open_modal_text);
        seeMoreButton.setOnClickListener(v -> {
            mViewModel.setLayoutTab("backdrops");

            ModalDialogFragment modalFragment = new ModalDialogFragment();
            modalFragment.show(MainActivity.getInstance().getSupportFragmentManager(), "modalFragmentTag");
        });

        //TODO make sure this goes to the nested Stations as well? But only for the unity project??
        MaterialButton newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("station", String.valueOf(binding.getSelectedStation().name));
            StationsFragment.mViewModel.setSelectedStationId(binding.getSelectedStation().id);

            // Open the video library as default if the video player is active
            Application current = binding.getSelectedStation().applicationController.findCurrentApplication();
            if ((current instanceof EmbeddedApplication)) {
                String subtype = current.subtype.optString("category", "");
                if (subtype.equals(Constants.VideoPlayer)) {
                    bundle.putString("library", "videos");
                    ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(LibrarySelectionFragment.class, "session", bundle);
                }
                return;
            }

            ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(LibrarySelectionFragment.class, "session", bundle);
        });

        //TODO make sure this goes to the nested Stations as well? But only for the unity project??
        Button restartGame = view.findViewById(R.id.station_restart_session);
        restartGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            if(selectedStation.applicationController.getGameName() == null || selectedStation.applicationController.getGameName().equals("")) {
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

            ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(DashboardPageFragment.class, "dashboard", null);
            DialogManager.awaitStationApplicationLaunch(new int[] { binding.getSelectedStation().id }, ApplicationLibraryFragment.mViewModel.getSelectedApplicationName(binding.getSelectedStation().applicationController.getGameId()), true);

            // Send data to Segment
            SegmentExperienceEvent event = new SegmentExperienceEvent(
                    SegmentConstants.Event_Experience_Restart,
                    selectedStation.getId(),
                    selectedStation.applicationController.getGameName(),
                    selectedStation.applicationController.getGameId(),
                    selectedStation.applicationController.getGameType()
            );
            Segment.trackAction(SegmentConstants.Event_Type_Experience, event);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(binding.getSelectedStation().id));
            }};
            FirebaseManager.logAnalyticEvent("session_restarted", analyticsAttributes);
        });

        //TODO make sure this goes to the nested Stations as well
        Button endGame = view.findViewById(R.id.station_end_session);
        endGame.setOnClickListener(v -> {
            Station selectedStation = binding.getSelectedStation();
            if(selectedStation.applicationController.getGameName() == null || selectedStation.applicationController.getGameName().equals("")) {
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

        Button changeLayout = view.findViewById(R.id.change_layout);
        changeLayout.setOnClickListener(v -> {
            mViewModel.setLayoutTab("layouts");

            ModalDialogFragment modalFragment = new ModalDialogFragment();
            modalFragment.show(MainActivity.getInstance().getSupportFragmentManager(), "modalFragmentTag");
        });

        //TODO make sure this goes to the nested Stations as well
        Button restartVr = view.findViewById(R.id.station_restart_vr);
        restartVr.setOnClickListener(v -> {
            //Start flashing the VR icons
            ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).setStationFlashing(binding.getSelectedStation().id, true);
            NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "RestartVR");
            DialogManager.awaitStationRestartVRSystem(new int[] { binding.getSelectedStation().id });

            // Send data to Segment
            SegmentStationEvent event = new SegmentStationEvent(SegmentConstants.Event_Station_VR_Restart, binding.getSelectedStation().id);
            Segment.trackAction(SegmentConstants.Event_Type_Station, event);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(binding.getSelectedStation().id));
            }};
            FirebaseManager.logAnalyticEvent("station_vr_system_restarted", analyticsAttributes);
        });

        //TODO make sure this shuts down nested Stations as well (also turns them on)
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
                if(SettingsFragment.checkAdditionalExitPrompts() && station.applicationController.getGameName() != null) {
                    //No game is present, shutdown is okay to continue
                    if(station.applicationController.getGameName().length() == 0) {
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

                    // Send data to Segment
                    SegmentStationEvent event = new SegmentStationEvent(SegmentConstants.Event_Station_Shutdown, binding.getSelectedStation().id);
                    Segment.trackAction(SegmentConstants.Event_Type_Station, event);
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
            }
        });

        if (newlySelectedStation != null) {
            setupAudioSpinner(view, newlySelectedStation);
            setupMuteButton(view);
            updateLayout(view, newlySelectedStation);
        }
        //endregion

        mViewModel.getSelectedStation().observe(getViewLifecycleOwner(), station -> {
            binding.setSelectedStation(station);
            binding.setSelectedNestedStation(station.getFirstNestedStationOrNull());
            updateExperienceImage(view, station);
            setupAudioSpinner(view, station);
            updateLayout(view, station);

            if (BackdropFragment.localBackdropAdapter != null) {
                BackdropFragment.localBackdropAdapter.notifyDataSetChanged();
            }
            if (localBackdropAdapter != null) {
                localBackdropAdapter.notifyDataSetChanged();
            }
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
        if (Helpers.isNullOrEmpty(station.applicationController.getGameType()) || Helpers.isNullOrEmpty(station.applicationController.getGameId()) || Helpers.isNullOrEmpty(station.applicationController.getGameName())) {
            ImageView experienceControlImage = view.findViewById(R.id.placeholder_image);
            experienceControlImage.setImageDrawable(null);
        } else {
            Helpers.SetExperienceImage(station.applicationController.getGameType(), station.applicationController.getGameName(), station.applicationController.getGameId(), view);
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
                ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(LibrarySelectionFragment.class, "session", bundle);
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
            controlTitle.setText(R.string.playback_title);

            GridView guides = view.findViewById(R.id.backdrop_section);
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
        GridView guides = view.findViewById(R.id.backdrop_section);
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
}
