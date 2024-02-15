package com.lumination.leadmelabs.models.stations;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.models.LocalAudioDevice;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.applications.ReviveApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.IconManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import io.sentry.Sentry;

public class Station implements Cloneable {
    public String name;
    public int id;
    public String status; //Describes the computer status (Off, On, Turning On)
    public String state; //Describes the state of the LeadMe software
    public String room;
    public String gameName = null;
    public String gameId;
    public String gameType;
    public ArrayList<Application> applications = new ArrayList<>();
    public boolean selected = false;
    private CountDownTimer shutdownTimer;
    public String macAddress;

    public Boolean requiresSteamGuard = false;

    //Track animation of icons
    IconManager iconManager = new IconManager();

    //Track the animation of the dots for Awaiting headset connection
    private Timer timer;
    int dotsCount = 0;

    //Track the different audio devices and the active device
    public int volume; //backwards compat - remove after next update
    private String activeAudioDevice;
    public List<LocalAudioDevice> audioDevices = new ArrayList<>();

    public Station(String name, Object applications, int id, String status, String state, String room, String macAddress) {
        this.name = name;
        this.id = id;
        this.status = status;
        this.state = state;
        this.room = room;
        this.macAddress = macAddress;

        this.setApplications(applications);
    }

    /**
     * Sets the applications for the station.
     *
     * @param applications The applications to be set. This can be either a JSON string
     *                     representing application data or a JSONArray containing application
     *                     objects.
     */
    private void setApplications(Object applications) {
        if (applications == null) return;

        if (applications instanceof String) {
            this.setApplicationsFromJsonString((String) applications);
        } else if (applications instanceof JSONArray) {
            try {
                this.setApplicationsFromJson((JSONArray) applications);
            } catch (JSONException e) {
                Sentry.captureException(e);
            }
        }
    }

    @NonNull
    @Override
    public Station clone() {
        Station clonedStation = null;
        try {
            clonedStation = (Station) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return clonedStation;
    }

    //region Audio Devices
    /**
     * Retrieves the active audio device from the list based on a specified name.
     *
     * @return The active audio device or null if not found.
     */
    public LocalAudioDevice GetActiveAudioDevice() {
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
    public LocalAudioDevice FindAudioDevice(String[] deviceNames) {
        return audioDevices.stream()
                .filter(device -> Arrays.asList(deviceNames).contains(device.getName()))
                .findFirst()
                .orElse(null);
    }

    public Boolean HasAudioDevice(String[] deviceNames) {
        return audioDevices.stream().anyMatch(device -> Arrays.asList(deviceNames).contains(device.getName()));
    }

    /**
     * Sets the active audio device name to the specified value.
     *
     * @param name The name to set as the active audio device.
     */
    public void SetActiveAudioDevice(String name) {
        this.activeAudioDevice = name;
    }

    /**
     * Retrieves the active audio device and sets its volume if found.
     *
     * @param volume The volume value to set.
     */
    public void SetVolume(int volume) {
        LocalAudioDevice foundDevice = GetActiveAudioDevice();

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
    public void SetMuted(boolean isMuted) {
        LocalAudioDevice foundDevice = GetActiveAudioDevice();

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
    public boolean GetMuted() {
        LocalAudioDevice foundDevice = GetActiveAudioDevice();

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
    public int GetVolume() {
        LocalAudioDevice foundDevice = GetActiveAudioDevice();

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
    public void SetAudioDevices(String jsonData) {
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

        if ((long) selectedStation.audioDevices.size() == 0) {
            slider.setValue(selectedStation.volume);
            return;
        }

        int value = selectedStation.GetVolume();
        slider.setValue(value);
    }

    @BindingAdapter("stationMuted")
    public static void setStationMuted(MaterialButton materialButton, Station selectedStation) {
        // Get the context from the MaterialButton's View
        Context context = materialButton.getContext();

        if (selectedStation == null || (long) selectedStation.audioDevices.size() == 0) {
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.station_volume);
            materialButton.setIcon(drawable);
            return;
        };

        Drawable drawable;
        boolean isMuted = selectedStation.GetMuted();
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

        if (selectedStation.HasAudioDevice(deviceNames)) {
            materialButton.setEnabled(true);
        } else {
            materialButton.setEnabled(false);
            materialButton.setStrokeColor(ContextCompat.getColorStateList(context, R.color.grey_card));
            materialButton.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_card));
            materialButton.setStrokeWidth(0);
            materialButton.setIconTint(ContextCompat.getColorStateList(context, R.color.grey_titles));
        }

        if(selectedStation.GetActiveAudioDevice() != null) {
            LocalAudioDevice activeDevice = selectedStation.GetActiveAudioDevice();

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
    //endregion

    public void setName(String newName)
    {
        name = newName;
    }

    //BACKWARDS COMPATIBILITY
    /**
     * Parses a JSON string containing application data and updates the list of applications.
     *
     * @param applicationsJson The JSON string containing application data (string list).
     */
    public void setApplicationsFromJsonString(String applicationsJson) {
        // Wrap with a catch for unforeseen characters - this will method will be removed in the future
        try {
            ArrayList<Application> newApplications = new ArrayList<>();
            String[] apps = applicationsJson.split("/");

            for (String app : apps) {
                String[] appData = app.split("\\|");
                if (appData.length <= 1) continue;

                String appType = appData[0];
                String appName = appData[2].replace("\"", "");
                String appId = appData[1];
                boolean isVr = appData.length >= 4 && Boolean.parseBoolean(appData[3]); // Backwards compatibility
                String subtype = appData.length >= 5 ? appData[4]: ""; // Backwards compatibility

                JSONObject appSubtype = null;
                try {
                    appSubtype = new JSONObject(subtype);
                }
                catch (Exception ignored) {}

                Application newApplication = createNewApplication(appType, appName, appId, isVr, appSubtype);
                if (newApplication == null) continue;
                newApplications.add(newApplication);
            }

            if (newApplications.isEmpty()) {
                return;
            }

            newApplications.sort((application, application2) -> application.name.compareToIgnoreCase(application2.name));
            this.applications = newApplications;

            //Check for missing thumbnails
            ImageManager.CheckLocalCache(applicationsJson);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    /**
     * Parses a JSON string containing application data and updates the list of applications.
     *
     * @param jsonArray The JSON array containing application data (JsonObjects).
     */
    public void setApplicationsFromJson(JSONArray jsonArray) throws JSONException {
        ArrayList<Application> newApplications = new ArrayList<>();

        // Iterate over each entry in the JSONArray using a traditional for loop
        for (int i = 0; i < jsonArray.length(); i++) {
            // Get the JSONObject at the current index
            JSONObject entry = jsonArray.getJSONObject(i);

            // Extract values for WrapperType, Name, Id, and IsVr
            String appType = entry.getString("WrapperType");
            String appName = entry.getString("Name");
            String appId = entry.getString("Id");
            boolean isVr = entry.getBoolean("IsVr");
            JSONObject appSubtype = entry.optJSONObject("Subtype");

            Application newApplication = createNewApplication(appType, appName, appId, isVr, appSubtype);
            if (newApplication == null) continue;
            newApplications.add(newApplication);
        }

        if (newApplications.isEmpty()) {
            return;
        }

        newApplications.sort((application, application2) -> application.name.compareToIgnoreCase(application2.name));
        this.applications = newApplications;

        //Check for missing thumbnails
        ImageManager.CheckLocalCache(jsonArray);
    }

    private Application createNewApplication(String appType, String appName, String appId, boolean isVr, JSONObject appSubtype) {
        Application temp = null;

        switch (appType) {
            case "Custom":
                temp = new CustomApplication(appType, appName, appId, isVr, appSubtype);
                break;
            case "Embedded":
                temp = new EmbeddedApplication(appType, appName, appId, isVr, appSubtype);
                break;
            case "Steam":
                temp = new SteamApplication(appType, appName, appId, isVr, appSubtype);
                break;
            case "Vive":
                temp = new ViveApplication(appType, appName, appId, isVr, appSubtype);
                break;
            case "Revive":
                temp = new ReviveApplication(appType, appName, appId, isVr, appSubtype);
                break;
        }

        return temp;
    }

    /**
     * Detect if a particular station has an application installed on it
     * @param applicationId A long that represents the ID of an experience.
     * @return A boolean if the application is installed.
     */
    public boolean hasApplicationInstalled(String applicationId) {
        for (Application application:this.applications) {
            if (Objects.equals(application.id, applicationId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Start a countdown to check the station status, if the station has not contacted the NUC
     * within the time limit (3mins) then something has gone wrong and alert the user.
     */
    public void powerStatusCheck(long delay) {
        //Cancel any previous power checks before starting a new one
        cancelStatusCheck();

        shutdownTimer = new CountDownTimer(delay, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                if(!SettingsFragment.checkLockedRooms(room)) {
                    return;
                }
                DialogManager.createBasicDialog("Station error", name + " has not powered on correctly. Try starting again, and if this does not work please contact your IT department for help");
                MainActivity.runOnUI(() -> {
                    Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);
                    station.status = "Off";
                    NetworkService.sendMessage("NUC", "UpdateStation", id + ":SetValue:status:Off");
                    ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(id, station);
                });
            }
        }.start();
    }

    /**
     * The station has turned on so cancel the automatic station check.
     */
    public void cancelStatusCheck() {
        if(shutdownTimer != null) {
            shutdownTimer.cancel();
        }
    }

    /**
     * Data binding to update the Station content flexbox background.
     */
    @BindingAdapter("stationState")
    public static void setStationStateBackground(FlexboxLayout flexbox, Station selectedStation) {
        if (selectedStation == null) return;

        boolean isStatusOn = selectedStation.status != null && (!selectedStation.status.equals("Off") && !selectedStation.status.equals("Turning On") && !selectedStation.status.equals("Restarting"));
        boolean hasState = selectedStation.state != null && selectedStation.state.length() != 0;
        boolean hasGame = selectedStation.gameName != null && selectedStation.gameName.length() != 0 && !selectedStation.gameName.equals("null");

        //Station is On and has either a State or a Game running
        if(isStatusOn && (hasState || hasGame)) {
            flexbox.setBackgroundResource(R.drawable.card_station_ripple_white);
        } else {
            flexbox.setBackgroundResource(R.drawable.card_station_ripple_empty);
        }
    }

    /**
     * Data binding to update the Station content text (Status or Game name)
     */
    @BindingAdapter("stationState")
    public static void setStationStateTextAndVisibility(TextView textView, Station selectedStation) {
        if (selectedStation == null) return;

        //Set the visibility value
        boolean isStatusOn = selectedStation.status != null && (!selectedStation.status.equals("Off") && !selectedStation.status.equals("Turning On") && !selectedStation.status.equals("Restarting"));
        boolean hasState = selectedStation.state != null && selectedStation.state.length() != 0;
        boolean hasGame = selectedStation.gameName != null && selectedStation.gameName.length() != 0 && !selectedStation.gameName.equals("null");
        int visibility = isStatusOn && (hasState || hasGame) ? View.VISIBLE : View.INVISIBLE;
        textView.setVisibility(visibility);

        //Stop the dot animation if it is anything besides Awaiting headset connection
        if(selectedStation.state == null || !selectedStation.state.equals("Awaiting headset connection...")) {
            selectedStation.stopAnimateDots();
        }

        //Set the text value ('Not set' - backwards compatibility, default state when it is not sent across)
        if(selectedStation.state != null && (!selectedStation.state.equals("Ready to go") || !hasGame) && !selectedStation.state.equals("Not set")) {
            //Show the state if the state is anything but Ready to go
            textView.setText(selectedStation.state);

            //Start the dot animation if awaiting connection and animator is not already running
            if(selectedStation.state.equals("Awaiting headset connection...")) {
                selectedStation.startAnimateDots(textView);
            }
        } else {
            selectedStation.stopAnimateDots();
            textView.setText(selectedStation.gameName);
        }
    }

    /// <summary>
    /// Starts an animation that updates a TextView with a sequence of dots, creating a loading effect.
    /// The animation displays the text "Awaiting headset connection" followed by 1, 2, 3 dots in a loop,
    /// with each dot appearing at one-second intervals. The sequence restarts after reaching three dots.
    /// </summary>
    /// <param name="textView">The TextView to animate.</param>
    private void startAnimateDots(final TextView textView) {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                textView.post(() -> {
                    StringBuilder animatedText = new StringBuilder("Awaiting headset connection");
                    for (int i = 0; i < dotsCount; i++) {
                        animatedText.append('.');
                    }

                    //Collect the current station state
                    VrStation station = (VrStation) ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);

                    //Make sure the state is the same before updating the dots
                    if(station.state.equals("Awaiting headset connection...")) {
                        textView.setText(animatedText.toString());
                        dotsCount = (dotsCount + 1) % 4; // Change the number of dots as needed
                    } else {
                        this.cancel();
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopAnimateDots() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
