package com.lumination.leadmelabs.unique.snowHydro;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBoundBinding;
import com.lumination.leadmelabs.models.LocalAudioDevice;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.stations.ContentStation;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.LocalAudioDeviceAdapter;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;

import java.util.ArrayList;
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

        //Set the adapter for backdrops
        if (newlySelectedStation != null) {
            GridView backdropGridView = view.findViewById(R.id.backdrop_section);
            localBackdropAdapter = new BackdropAdapter(getContext());
            localBackdropAdapter.backdropList = (ArrayList<Video>) newlySelectedStation.videoController.getVideosOfType(Constants.VideoTypeBackdrop);
            backdropGridView.setAdapter(localBackdropAdapter);
        }

        // Inflate and Bind the VR devices layout if the selected station is a VirtualStation
        if (newlySelectedStation instanceof ContentStation) {
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
            updateExperienceImage(view, station);
            setupAudioSpinner(view, station);
            updateLayout(view, station);
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

    //TODO finish this off
    //region Layout Control
    private void inflateVRDevicesLayout() {
        ViewStub controlDevicesStub = binding.getRoot().findViewById(R.id.control_devices_section);
        controlDevicesStub.inflate();
    }

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
            controlTitle.setText("Playback");

            GridView guides = view.findViewById(R.id.backdrop_section);
            guides.setVisibility(View.GONE);

            FlexboxLayout controls = view.findViewById(R.id.video_controls_bound);
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
        controlTitle.setText("Backdrop");

        //Reset the video controls
        FlexboxLayout controls = view.findViewById(R.id.video_controls_bound);
        controls.setVisibility(View.GONE);

        //Reset the share code controls

        //Reset the guide section
        GridView guides = view.findViewById(R.id.backdrop_section);
        guides.setVisibility(View.VISIBLE);
    }
    //endregion
}
