package com.lumination.leadmelabs.ui.stations.controllers;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.LocalAudioDevice;
import com.lumination.leadmelabs.models.stations.Station;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.sentry.Sentry;

public class AudioController {
    //Track the different audio devices and the active device
    public int volume; //backwards compat - remove after next update
    private String activeAudioDevice;
    public List<LocalAudioDevice> audioDevices = new ArrayList<>();

    /**
     * Retrieves the active audio device from the list based on a specified name.
     *
     * @return The active audio device or null if not found.
     */
    public LocalAudioDevice getActiveAudioDevice() {
        return audioDevices.stream()
                .filter(device -> device.getName().equals(activeAudioDevice))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves the active audio device from the list based on a specified name.
     *
     * @return The active audio device or null if not found.
     */
    public LocalAudioDevice findAudioDevice(String[] deviceNames) {
        return audioDevices.stream()
                .filter(device -> Arrays.asList(deviceNames).contains(device.getName()))
                .findFirst()
                .orElse(null);
    }

    public Boolean hasAudioDevice(String[] deviceNames) {
        return audioDevices.stream().anyMatch(device -> Arrays.asList(deviceNames).contains(device.getName()));
    }

    /**
     * Sets the active audio device name to the specified value.
     *
     * @param name The name to set as the active audio device.
     */
    public void setActiveAudioDevice(String name) {
        this.activeAudioDevice = name;
    }

    /**
     * Retrieves the active audio device and sets its volume if found.
     *
     * @param volume The volume value to set.
     */
    public void setVolume(int volume) {
        LocalAudioDevice foundDevice = getActiveAudioDevice();

        // Now 'foundDevice' contains the device with the supplied name, or it's null if not found
        if (foundDevice != null) {
            foundDevice.SetVolume(volume);
        }
    }

    /**
     * Retrieves the active audio device and sets its muted value.
     *
     * @param isMuted A boolean representing the muted value (true = muted).
     */
    public void setMuted(boolean isMuted) {
        LocalAudioDevice foundDevice = getActiveAudioDevice();

        // Now 'foundDevice' contains the device with the supplied name, or it's null if not found
        if (foundDevice != null) {
            foundDevice.SetMuted(isMuted);
        }
    }

    /**
     * Retrieves the muted value of the active audio device, or false if not found.
     *
     * @return The muted value of the active audio device.
     */
    public boolean getMuted() {
        LocalAudioDevice foundDevice = getActiveAudioDevice();

        // Now 'foundDevice' contains the device with the supplied name, or it's null if not found
        if (foundDevice != null) {
            return foundDevice.GetMuted();
        }

        return false;
    }

    /**
     * Retrieves the volume of the active audio device, or 0 if not found.
     *
     * @return The volume of the active audio device.
     */
    public int getVolume() {
        LocalAudioDevice foundDevice = getActiveAudioDevice();

        // Now 'foundDevice' contains the device with the supplied name, or it's null if not found
        if (foundDevice != null) {
            return foundDevice.GetVolume();
        }

        return 0;
    }

    /**
     * Parses JSON data to create a list of LocalAudioDevice objects.
     * The JSON data should contain an array of objects with "Name" and "Id" properties.
     *
     * @param jsonData The JSON data to parse.
     */
    public void setAudioDevices(String jsonData) {
        List<LocalAudioDevice> audioDevices = new ArrayList<>();

        try {
            JSONArray devices = new JSONArray(jsonData);

            for (int i = 0; i < devices.length(); i++) {
                JSONObject audioJson = devices.getJSONObject(i);

                String name = audioJson.optString("Name", "");
                String id = audioJson.optString("Id", "");
                String volume = audioJson.optString("Volume", "");
                String muted = audioJson.optString("Muted", "");
                if (name.equals("") || id.equals("")) continue;

                LocalAudioDevice temp = new LocalAudioDevice(name, id);
                //Set volume if it is present or default to 0
                temp.SetVolume(volume.equals("") ? 0 : (int) Double.parseDouble(volume));
                temp.SetMuted(muted.equals("") ? false : Boolean.parseBoolean(muted));

                audioDevices.add(temp);
            }

            this.audioDevices = audioDevices;

        } catch (JSONException e) {
            Sentry.captureException(e);
        }
    }

    @BindingAdapter("stationVolume")
    public static void setStationVolume(Slider slider, Station selectedStation) {
        if (selectedStation == null) {
            slider.setValue(0);
            return;
        };

        if ((long) selectedStation.audioController.audioDevices.size() == 0) {
            slider.setValue(selectedStation.audioController.volume);
            return;
        }

        int value = selectedStation.audioController.getVolume();
        slider.setValue(value);
    }

    @BindingAdapter("stationMuted")
    public static void setStationMuted(MaterialButton materialButton, Station selectedStation) {
        // Get the context from the MaterialButton's View
        Context context = materialButton.getContext();

        if (selectedStation == null || (long) selectedStation.audioController.audioDevices.size() == 0) {
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.station_volume);
            materialButton.setIcon(drawable);
            return;
        };

        Drawable drawable;
        boolean isMuted = selectedStation.audioController.getMuted();
        if (isMuted) {
            drawable = ContextCompat.getDrawable(context, R.drawable.station_mute);
        } else {
            drawable = ContextCompat.getDrawable(context, R.drawable.station_volume);
        }
        materialButton.setIcon(drawable);
    }

    @BindingAdapter("headsetAudioDevice")
    public static void setHeadsetAudioDevice(MaterialButton materialButton, Station selectedStation) {
        handleAudioButton(materialButton, selectedStation, LocalAudioDevice.headsetAudioDeviceNames);
    }

    @BindingAdapter("projectorAudioDevice")
    public static void setProjectorAudioDevice(MaterialButton materialButton, Station selectedStation) {
        handleAudioButton(materialButton, selectedStation, LocalAudioDevice.projectorAudioDeviceNames);
    }

    private static void handleAudioButton(MaterialButton materialButton, Station selectedStation, String[] deviceNames)
    {
        // Get the context from the MaterialButton's View
        Context context = materialButton.getContext();

        if (selectedStation.audioController.hasAudioDevice(deviceNames)) {
            materialButton.setEnabled(true);
        } else {
            materialButton.setEnabled(false);
            materialButton.setStrokeColor(ContextCompat.getColorStateList(context, R.color.grey_card));
            materialButton.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_card));
            materialButton.setStrokeWidth(0);
            materialButton.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_titles));
        }

        if(selectedStation.audioController.getActiveAudioDevice() != null) {
            LocalAudioDevice activeDevice = selectedStation.audioController.getActiveAudioDevice();

            if (Arrays.stream(deviceNames).anyMatch(activeDevice.getName()::equals)) {
                // active
                materialButton.setStrokeColor(ContextCompat.getColorStateList(context, R.color.blue_darkest));
                materialButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue_even_lighter));
                materialButton.setStrokeWidth(2);
                materialButton.setIconTint(ContextCompat.getColorStateList(context, R.color.blue_darkest));
                return;
            }
        }
        // inactive
        materialButton.setStrokeColor(ContextCompat.getColorStateList(context, R.color.white));
        materialButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.white));
        materialButton.setStrokeWidth(0);
        materialButton.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_titles));
    }
}
