package com.lumination.leadmelabs.unique.snowHydro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Spinner;

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
import com.lumination.leadmelabs.models.stations.ContentStation;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.stations.LocalAudioDeviceAdapter;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is designed specifically for the Snowy Hydro project. The single page handles a
 * primary computer (The Wall PC) and a bound computer (The Floor PC). There are many similarities
 * between this class and the StationSingleFragment but for ease of use this class is kept completely
 * separate.
 */
public class StationSingleBoundFragment extends Fragment {
    public static StationsViewModel mViewModel;
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

        // Inflate and Bind the VR devices layout if the selected station is a VirtualStation
        if (newlySelectedStation instanceof ContentStation) {
            inflateVRDevicesLayout();
        }


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

            //TODO adjust the layout?
            //updateLayout(view, newlySelectedStation);
        }
        //endregion
    }

    /**
     * If the selected Station is of type VirtualStation, inflate the Vr devices stub layout. The
     * main fragment_station_single.xml passes the current selectedStation binding down to the stub
     * as to pass on the data binding from the VirtualStation class.
     */
    private void inflateVRDevicesLayout() {
        ViewStub controlDevicesStub = binding.getRoot().findViewById(R.id.control_devices_section);
        controlDevicesStub.inflate();
    }

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
}
