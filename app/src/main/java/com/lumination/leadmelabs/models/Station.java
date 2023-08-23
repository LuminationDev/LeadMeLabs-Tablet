package com.lumination.leadmelabs.models;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.IconAnimator;
import com.lumination.leadmelabs.utilities.IconManager;

import java.util.ArrayList;
import java.util.Objects;

public class Station implements Cloneable {
    public String name;
    public int id;
    public String status;
    public String room;
    public String gameName = null;
    public String gameId;
    public String gameType;
    public int volume;
    public ArrayList<Application> applications = new ArrayList<>();
    public boolean selected = false;
    private CountDownTimer shutdownTimer;
    public String macAddress;
    public String ledRingId;
    public Boolean requiresSteamGuard = false;

    //VR Devices
    public CountDownTimer flashTimer;
    public Boolean animationFlag = false; //Determines if the VR UI is flashing
    public String thirdPartyHeadsetTracking; //Currently on Vive but may change in the future
    public String openVRHeadsetTracking;
    public String leftControllerTracking;
    public int leftControllerBattery;
    public String rightControllerTracking;
    public int rightControllerBattery;
    public int baseStationsActive;
    public int baseStationsTotal;


    //Track animation of icons
    IconManager iconManager = new IconManager();

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

    public Station(String name, String applications, int id, String status, int volume, String room, String ledRingId, String macAddress) {
        this.name = name;
        if (applications != null && applications.length() > 0 && !applications.equals("Off")) {
            this.setApplicationsFromJsonString(applications);
        }
        this.id = id;
        this.status = status;
        this.volume = volume;
        this.room = room;
        this.macAddress = macAddress;
        this.ledRingId = ledRingId;

        initiateVRDevices();
    }

    /**
     * Initiate or reset the VR device statuses associated with the Stations
     */
    public void initiateVRDevices() {
        this.thirdPartyHeadsetTracking = "Off"; //This is only checked when an experience is launched
        this.openVRHeadsetTracking = "Off";
        this.leftControllerTracking = "Off";
        this.leftControllerBattery = 0;
        this.rightControllerTracking = "Off";
        this.rightControllerBattery = 0;
        this.baseStationsActive = 0;
        this.baseStationsTotal = 0;
    }

    public void setName(String newName)
    {
        name = newName;
    }

    public void setApplicationsFromJsonString(String applicationsJson) {
        ArrayList<Application> newApplications = new ArrayList<>();
        String[] apps = applicationsJson.split("/");
        for (String app: apps) {
            String[] appData = app.split("\\|");
            if (appData.length > 1) {
                switch (appData[0]) {
                    case "Custom":
                        newApplications.add(new CustomApplication(appData[0], appData[2].replace("\"", ""), appData[1]));
                        break;
                    case "Steam":
                        newApplications.add(new SteamApplication(appData[0], appData[2].replace("\"", ""), appData[1]));
                        break;
                    case "Vive":
                        newApplications.add(new ViveApplication(appData[0], appData[2].replace("\"", ""), appData[1]));
                        break;
                }
            }
        }
        if (newApplications.size() > 0) {
            newApplications.sort((application, application2) -> application.name.compareToIgnoreCase(application2.name));
            this.applications = newApplications;

            //Check for missing thumbnails
            ImageManager.CheckLocalCache(applicationsJson);
        }
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
    public void powerStatusCheck() {
        //Cancel any previous power checks before starting a new one
        cancelStatusCheck();

        shutdownTimer = new CountDownTimer(3 * 1000 * 60, 1000) {
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

    public String getControllerTracking(String controllerType) {
        if (controllerType.equals("Left")) {
            return leftControllerTracking;
        } else if (controllerType.equals("Right")) {
            return rightControllerTracking;
        } else {
            return null;
        }
    }

    public int getControllerBattery(String controllerType) {
        if (controllerType.equals("Left")) {
            return leftControllerBattery;
        } else if (controllerType.equals("Right")) {
            return rightControllerBattery;
        } else {
            return 0;
        }
    }

    /**
     * Determine if the supplied Icon is required to start flashing, stop, or do nothing.
     * @param imageView The associated Icon.
     * @param selectedStation The station the icon is linked to.
     * @param iconName A string of to use as the key in the icon manager.
     */
    public void handleIconAnimation(ImageView imageView, Station selectedStation, String iconName) {
        if(selectedStation.animationFlag) {
            //Check if the image view is already saved before re-writing.
            ImageView temp = selectedStation.iconManager.getIconAnimator(iconName);
            if(temp != imageView) {
                selectedStation.iconManager.addIconAnimator(iconName, new IconAnimator(imageView));
                selectedStation.iconManager.startFlashing(iconName);
            }
        } else {
            selectedStation.iconManager.stopFlashing(iconName);
        }
    }

    /**
     * As a backup, when the VR icons start flashing, begin a countdown timer. At the end of the
     * timer, if the animation is still flashing (animationFlag), alert the user that the restart
     * was unsuccessful.
     */
    public void handleAnimationTimer() {
        if(flashTimer != null) {
            flashTimer.cancel();
            flashTimer = null;
        }

        flashTimer = new CountDownTimer(2 * 1000 * 60, 1000) {
            @Override
            public void onTick(long l) {}

            @Override
            public void onFinish() {
                if(!animationFlag) {
                    return;
                }

                //Close the restart system
                DialogManager.vrSystemRestartedOnStation(id);
                DialogManager.createBasicDialog("Station error", name + " has not restarted the VR system. Try restarting again, and if this does not work please restart the Station.");
                MainActivity.runOnUI(() -> {
                    Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);
                    station.animationFlag = false;
                    ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(id, station);
                });
            }
        }.start();
    }

    // DATA BINDING FUNCTIONS BELOW

    /**
     * Data binding to update the Vive status image view source.
     */
    @BindingAdapter("headsetStatus")
    public static void setHeadsetStatusImage(ImageView imageView, Station selectedStation) {
        if (selectedStation == null) return;

        String headsetTracking = selectedStation.openVRHeadsetTracking;
        boolean isStatusOff = selectedStation.status != null && selectedStation.status.equals("Off");
        boolean trackingOff = selectedStation.openVRHeadsetTracking.equals("Off") && selectedStation.thirdPartyHeadsetTracking.equals("Off");
        int visibility = isStatusOff || trackingOff ? View.INVISIBLE : View.VISIBLE;
        imageView.setVisibility(visibility);

        if (headsetTracking.equals("Connected") && selectedStation.thirdPartyHeadsetTracking.equals("Connected")) {
            //Station is on - OpenVR & the Third party headset software is connected
            imageView.setImageResource(R.drawable.vr_status_tick);
        } else {
            //Station is on - openVR or vive is lost/off
            imageView.setImageResource(R.drawable.vr_status_issue);
        }
    }

    /**
     * Data binding to update the Base Station's image view source.
     */
    @BindingAdapter("headset")
    public static void setHeadsetImage(ImageView imageView, Station selectedStation) {
        if (selectedStation == null) return;
        boolean isStatusOff = selectedStation.status != null && selectedStation.status.equals("Off");

        if (isStatusOff) {
            //Station is off - no headset expected
            imageView.setImageResource(R.drawable.vr_headset_gray);
            return;
        }

        if(selectedStation.openVRHeadsetTracking.equals("Connected") && selectedStation.thirdPartyHeadsetTracking.equals("Connected") ||
                (selectedStation.openVRHeadsetTracking.equals("Connected") && selectedStation.thirdPartyHeadsetTracking.equals("Off"))) {
            //Station is on - OpenVR & the Third party headset software is connected
            imageView.setImageResource(R.drawable.vr_headset_on);
        } else {
            //Station is on - openVR is off
            imageView.setImageResource(R.drawable.vr_headset_off);
        }

        selectedStation.handleIconAnimation(imageView, selectedStation, "Headset");
    }

    /**
     * Data binding to update the Controller's battery text visibility and value.
     */
    @BindingAdapter({"station", "controllerType"})
    public static void setBatteryVisibilityAndText(TextView textView, Station selectedStation, String controllerType) {
        if (selectedStation == null) return;

        String connectedController = selectedStation.getControllerTracking(controllerType);
        String headsetTracking = selectedStation.openVRHeadsetTracking;

        //Station is off
        boolean isStatusOff = selectedStation.status != null && selectedStation.status.equals("Off");
        //Headset is not tracking or has not been initiated
        boolean tracking = (headsetTracking.equals("Lost") || headsetTracking.equals("Off")) || (headsetTracking.equals("Connected") && connectedController.equals("Lost"));
        //Controller is connected
        boolean isControllerConnected = connectedController != null && !connectedController.equals("Connected");
        int batteryValue = selectedStation.getControllerBattery(controllerType);

        int visibility = isStatusOff || isControllerConnected || batteryValue == 0 || tracking ? View.INVISIBLE : View.VISIBLE;
        textView.setVisibility(visibility);

        //Set the battery text value
        String batteryText;
        if (connectedController != null && connectedController.equals("Connected")) {
            batteryText = selectedStation.getControllerBattery(controllerType) + "%";
        } else {
            batteryText = "0%";
        }

        //Set the battery text colour (relates to battery value, below 15 is getting low)
        int color = ContextCompat.getColor(MainActivity.getInstance(), R.color.grey_card);
        if(batteryValue > 25) {
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.blue);
        } else if (batteryValue > 0)  {
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.orange);
        }

        textView.setTextColor(color);
        textView.setText(batteryText);
    }

    /**
     * Data binding to update a Controller's image view source.
     */
    @BindingAdapter(value = {"station", "controllerType"})
    public static void setControllerImage(ImageView imageView, Station selectedStation, String controllerType) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.status != null && selectedStation.status.equals("Off");

        if (isStatusOff) {
            //Station is off - no controller set to default
            imageView.setImageResource(R.drawable.vr_controller_gray);
            return;
        }

        int batteryValue = selectedStation.getControllerBattery(controllerType);
        String tracking = selectedStation.getControllerTracking(controllerType);
        String headsetTracking = selectedStation.openVRHeadsetTracking;

        // Set the actual image based on the active status
        //If OpenVR is off or lost, OR if the OpenVR is connected but the controller is lost.
        if ((headsetTracking.equals("Lost") || headsetTracking.equals("Off")) || (headsetTracking.equals("Connected") && tracking.equals("Off"))) {
            // Headset is tracking, controller is not - Controller is Off
            imageView.setImageResource(R.drawable.vr_controller_off);

        } else if (headsetTracking.equals("Connected") && tracking.equals("Lost")) {
            //Station is on - headset connected and the controller has connected
            imageView.setImageResource(R.drawable.vr_controller_off);

        } else if (tracking.equals("Connected") && batteryValue <= 25) {
            // Headset is tracking, controller is tracking - Controller has low battery
            imageView.setImageResource(R.drawable.vr_controller_lost);

        } else if (selectedStation.getControllerTracking(controllerType).equals("Connected")) {
            //Station is on - active base stations equals total.
            imageView.setImageResource(R.drawable.vr_controller_on);
        }

        selectedStation.handleIconAnimation(imageView, selectedStation, controllerType + "Controller");
    }

    /**
     * Data binding to update the Base Station's active total text visibility and value.
     */
    @BindingAdapter("baseStationText")
    public static void setBaseStationVisibilityAndText(TextView textView, Station selectedStation) {
        if (selectedStation == null) return;

        String headsetTracking = selectedStation.openVRHeadsetTracking;

        boolean isStatusOff = selectedStation.status != null && selectedStation.status.equals("Off");
        boolean lessThanOne = selectedStation.baseStationsActive == 0;
        //Headset is not tracking or has not been initiated
        boolean tracking = (headsetTracking.equals("Lost") || headsetTracking.equals("Off"));
        int visibility = isStatusOff || lessThanOne || tracking ? View.INVISIBLE : View.VISIBLE;
        textView.setVisibility(visibility);

        String active = String.valueOf(selectedStation.baseStationsActive);

        // Set the text colour on the active status
        int color = ContextCompat.getColor(MainActivity.getInstance(), R.color.grey_card);
        if (selectedStation.baseStationsActive == 0) {
            //Station is on - no active base stations found
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.red);
        } else if (selectedStation.baseStationsActive < 2) {
            //Station is on - active base stations less than 2
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.orange);
        } else {
            //Station is on - active base stations greater than 2
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.blue);
        }

        //Display the active number of base stations
        textView.setTextColor(color);
        textView.setText(active);
    }

    /**
     * Data binding to update the Base Station's image view source.
     */
    @BindingAdapter("baseStation")
    public static void setBaseStationImage(ImageView imageView, Station selectedStation) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.status != null && selectedStation.status.equals("Off");

        if (isStatusOff) {
            //Station is off - no base stations expected
            imageView.setImageResource(R.drawable.vr_base_station_gray);
            return;
        }

        // Set the actual image based on the active status
        if (selectedStation.baseStationsActive == 0 || selectedStation.openVRHeadsetTracking.equals("Lost") ||
                selectedStation.openVRHeadsetTracking.equals("Off")) {
            //Station is on - no active base stations found
            imageView.setImageResource(R.drawable.vr_base_station_off);
        } else if (selectedStation.baseStationsActive < 2) {
            //Station is on - active base stations less than 2
            imageView.setImageResource(R.drawable.vr_base_station_lost);
        } else {
            //Station is on - active base stations greater than 2.
            imageView.setImageResource(R.drawable.vr_base_station_on);
        }

        selectedStation.handleIconAnimation(imageView, selectedStation, "BaseStation");
    }

    /**
     * Data binding to update the Vive connection image view source.
     */
    @BindingAdapter(value = {"station", "headsetManagerType"})
    public static void setSoftwareImage(ImageView imageView, Station selectedStation, String headsetManagerType) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.status != null && selectedStation.status.equals("Off");
        if (isStatusOff) {
            //Station is off - no connection expected
            imageView.setImageResource(headsetManagerType.equals("OpenVR") ? R.drawable.vr_steam_connection_gray : R.drawable.vr_vive_connection_gray);
            return;
        }

        String trackingType = headsetManagerType.equals("OpenVR") ? selectedStation.openVRHeadsetTracking : selectedStation.thirdPartyHeadsetTracking;
        // Set the actual image based on the active status
        switch (trackingType) {
            case "Connected":
                //Station is on - vive and/or openVR is connected
                imageView.setImageResource(headsetManagerType.equals("OpenVR") ? R.drawable.vr_steam_connection_on :R.drawable.vr_vive_connection_on);
                break;
            case "Lost":
            case "Off":
                //Station is on - vive and/or openVR is off
                imageView.setImageResource(headsetManagerType.equals("OpenVR") ? R.drawable.vr_steam_connection_off :R.drawable.vr_vive_connection_off);
                break;
        }

        selectedStation.handleIconAnimation(imageView, selectedStation, headsetManagerType);
    }

    /**
     * Data binding to update the Vive status image view source.
     */
    @BindingAdapter(value = {"station", "headsetManagerIssue"})
    public static void setSoftwareStatusImage(ImageView imageView, Station selectedStation, String headsetManagerIssue) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.status != null && selectedStation.status.equals("Off");
        boolean trackingOff = headsetManagerIssue.equals("OpenVR") ? selectedStation.openVRHeadsetTracking.equals("Off") : selectedStation.thirdPartyHeadsetTracking.equals("Off");
        int visibility = isStatusOff || trackingOff ? View.INVISIBLE : View.VISIBLE;
        imageView.setVisibility(visibility);

        String trackingType = headsetManagerIssue.equals("OpenVR") ? selectedStation.openVRHeadsetTracking : selectedStation.thirdPartyHeadsetTracking;

        // Set the actual image based on the active status
        switch (trackingType) {
            case "Connected":
                //Station is on - vive is connected
                imageView.setImageResource(R.drawable.vr_status_tick);
                break;
            case "Lost":
            case "Off":
                //Station is on - vive has an issue
                imageView.setImageResource(R.drawable.vr_status_issue);
                break;
        }
    }
}
