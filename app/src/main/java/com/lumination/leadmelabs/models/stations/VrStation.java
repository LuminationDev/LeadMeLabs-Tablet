package com.lumination.leadmelabs.models.stations;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.IconAnimator;

import java.util.Timer;
import java.util.TimerTask;

public class VrStation extends Station {
    public String ledRingId;

    //VR Devices
    public String headsetType;
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

    public int trackersActive;
    public int trackersTotal;

    //Track the animation of the dots for Awaiting headset connection
    private Timer timer;
    int dotsCount = 0;

    public VrStation(String name, Object applications, int id, String status, String state, String room, String macAddress, boolean isHiddenStation, String ledRingId, String headsetType) {
        super(name, applications, id, status, state, room, macAddress, isHiddenStation);

        this.ledRingId = ledRingId;
        this.headsetType = headsetType;

        initiateVRDevices();
    }

    /**
     * Set the type of headset the Station is using.
     * @param headsetType A string describing the headset.
     */
    public void setHeadsetType(String headsetType) {
        this.headsetType = headsetType;
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
        this.trackersActive = 0;
        this.trackersTotal = 0;
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
     * @param flash A boolean signifying if the icon should start flashing
     * @param imageView The associated Icon.
     * @param selectedStation The station the icon is linked to.
     * @param iconName A string of to use as the key in the icon manager.
     */
    public void handleIconAnimation(Boolean flash, ImageView imageView, VrStation selectedStation, String iconName) {
        if(flash) {
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
                    VrStation station = (VrStation) ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);
                    station.animationFlag = false;
                    ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(id, station);
                });
            }
        }.start();
    }

    // DATA BINDING FUNCTIONS BELOW
    //region VR Device Binding
    /**
     * Data binding to update the Vive status image view source.
     */
    @BindingAdapter("headsetStatus")
    public static void setHeadsetStatusImage(ImageView imageView, VrStation selectedStation) {
        if (selectedStation == null) return;

        String headsetTracking = selectedStation.openVRHeadsetTracking;
        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
        boolean isIdle = selectedStation.statusHandler.isStationIdle();
        boolean trackingOff = selectedStation.openVRHeadsetTracking.equals("Off") && selectedStation.thirdPartyHeadsetTracking.equals("Off");
        int visibility = isStatusOff || isIdle || trackingOff ? View.INVISIBLE : View.VISIBLE;
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
    public static void setHeadsetImage(ImageView imageView, VrStation selectedStation) {
        if (selectedStation == null) return;
        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();

        if (isStatusOff) {
            //Station is off - no headset expected
            imageView.setImageResource(R.drawable.vr_headset_gray);
            return;
        }

        boolean isConnected = selectedStation.openVRHeadsetTracking.equals("Connected") && selectedStation.thirdPartyHeadsetTracking.equals("Connected");

        boolean isOff = selectedStation.openVRHeadsetTracking.equals("Off") && selectedStation.thirdPartyHeadsetTracking.equals("Off");

        if (selectedStation.statusHandler.isStationIdle()) {
            //Station is on - in Idle mode
            imageView.setImageResource(R.drawable.vr_headset_idle);

        } else if(selectedStation.animationFlag) {
            imageView.setImageResource(R.drawable.vr_headset_outline);

        } else if(isConnected) {
            //Station is on - OpenVR & the Third party headset software is connected and an experience is running
            imageView.setImageResource(R.drawable.vr_headset_active);

        } else if (isOff) {
            //Station is on - OpenVR or Third party headset software is off.
            imageView.setImageResource(R.drawable.vr_headset_gray);

        } else {
            //Station is on - openVR is off
            imageView.setImageResource(R.drawable.vr_headset_off);
        }

        selectedStation.handleIconAnimation(selectedStation.animationFlag, imageView, selectedStation, "Headset");
    }

    /**
     * Data binding to update the Controller's battery text visibility and value.
     */
    @BindingAdapter({"station", "controllerType"})
    public static void setBatteryVisibilityAndText(TextView textView, VrStation selectedStation, String controllerType) {
        if (selectedStation == null) return;

        String connectedController = selectedStation.getControllerTracking(controllerType);
        String headsetTracking = selectedStation.openVRHeadsetTracking;

        //Station is off
        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
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
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.green);
        } else if (batteryValue > 0)  {
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.orange);
        }

        textView.setTextColor(color);
        textView.setText(batteryText);
    }

    /**
     * Data binding to update the Controller's battery status visibility and value on the Station
     * cards.
     */
    @BindingAdapter({"battery", "controllerType"})
    public static void setBatteryVisibilityAndImage(ImageView imageView, VrStation selectedStation, String controllerType) {
        if (selectedStation == null) return;

        String connectedController = selectedStation.getControllerTracking(controllerType);
        String headsetTracking = selectedStation.openVRHeadsetTracking;

        //Station is off
        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
        //Headset is not tracking or has not been initiated
        boolean tracking = (headsetTracking.equals("Lost") || headsetTracking.equals("Off")) || (headsetTracking.equals("Connected") && connectedController.equals("Lost"));
        //Controller is connected
        boolean isControllerConnected = connectedController != null && !connectedController.equals("Connected");
        int batteryValue = selectedStation.getControllerBattery(controllerType);

        if(isStatusOff || isControllerConnected || batteryValue == 0 || tracking) {
            imageView.setImageResource(R.drawable.vr_battery_unknown);
            return;
        }

        if (connectedController == null) return;

        //Reset the flashing before setting a new image view
        selectedStation.handleIconAnimation(false, imageView, selectedStation, controllerType + "Battery");

        //Set the battery status image colour (relates to battery value, below 15 is getting low)
        if(batteryValue > 50) {
            imageView.setImageResource(R.drawable.vr_battery_full);
        } else if (batteryValue > 15)  {
            imageView.setImageResource(R.drawable.vr_battery_two_bar);
        } else if (batteryValue > 5)  {
            imageView.setImageResource(R.drawable.vr_battery_one_bar);
        } else if (batteryValue > 0)  {
            imageView.setImageResource(R.drawable.vr_battery_empty);
            selectedStation.handleIconAnimation(true, imageView, selectedStation, controllerType + "Battery");
        }
    }

    /**
     * Data binding to update a Controller's image view source.
     */
    @BindingAdapter(value = {"station", "controllerType"})
    public static void setControllerImage(ImageView imageView, VrStation selectedStation, String controllerType) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
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
        if (headsetTracking.equals("Off") || selectedStation.thirdPartyHeadsetTracking.equals("Off")) {
            // Headset is tracking, controller is not - Controller is Off
            imageView.setImageResource(R.drawable.vr_controller_gray);

        } else if (selectedStation.statusHandler.isStationIdle()) {
            //Station is on - in Idle mode
            imageView.setImageResource(R.drawable.vr_controller_idle);

        } else if (headsetTracking.equals("Connected") && tracking.equals("Lost")) {
            //Station is on - headset connected and the controller has connected then disconnected
            imageView.setImageResource(R.drawable.vr_controller_gray);

        } else if (tracking.equals("Connected")) {
            // Headset is tracking, controller is tracking and an experience
            imageView.setImageResource(R.drawable.vr_controller_active);

        } else {
            imageView.setImageResource(R.drawable.vr_controller_gray);
        }

        selectedStation.handleIconAnimation(selectedStation.animationFlag, imageView, selectedStation, controllerType + "Controller");
    }

    /**
     * Data binding to update the Base Station's active total text visibility and value.
     */
    @BindingAdapter("baseStationText")
    public static void setBaseStationVisibilityAndText(TextView textView, VrStation selectedStation) {
        if (selectedStation == null) return;

        String headsetTracking = selectedStation.openVRHeadsetTracking;

        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
        boolean isIdle = selectedStation.statusHandler.isStationIdle();
        boolean lessThanOne = selectedStation.baseStationsActive == 0;
        //Headset is not tracking or has not been initiated
        boolean tracking = (headsetTracking.equals("Lost") || headsetTracking.equals("Off"));
        int visibility = isStatusOff || isIdle || lessThanOne || tracking ? View.INVISIBLE : View.VISIBLE;
        textView.setVisibility(visibility);

        String active = String.valueOf(selectedStation.baseStationsActive);

        // Set the text colour on the active status
        int color;
        if (selectedStation.baseStationsActive == 0) {
            //Station is on - no active base stations found
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.red);
        } else if (selectedStation.baseStationsActive < 2) {
            //Station is on - active base stations less than 2
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.orange);
        } else {
            //Station is on - active base stations greater than 2
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.green);
        }

        //Display the active number of base stations
        textView.setTextColor(color);
        textView.setText(active);
    }

    /**
     * Data binding to update the Base Station's image view source.
     */
    @BindingAdapter("baseStation")
    public static void setBaseStationImage(ImageView imageView, VrStation selectedStation) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
        if (isStatusOff) {
            //Station is off - no base stations expected
            imageView.setImageResource(R.drawable.vr_base_station_gray);
            return;
        }

        // Set the actual image based on the active status
        if (selectedStation.openVRHeadsetTracking.equals("Off") || selectedStation.thirdPartyHeadsetTracking.equals("Off")) {
            //Station is on - software has not started yet
            imageView.setImageResource(R.drawable.vr_base_station_gray);

        } else if (selectedStation.statusHandler.isStationIdle()) {
            //Station is on - in Idle mode
            imageView.setImageResource(R.drawable.vr_base_station_idle);

        } else if (selectedStation.baseStationsActive == 0 || selectedStation.openVRHeadsetTracking.equals("Lost")) {
            //Station is on - no active base stations found
            imageView.setImageResource(R.drawable.vr_base_station_off);

        } else if (selectedStation.baseStationsActive < 2) {
            //Station is on - active base stations less than 2
            imageView.setImageResource(R.drawable.vr_base_station_lost);

        } else {
            //Station is on - active base stations greater than 2 and an experience is running.
            imageView.setImageResource(R.drawable.vr_base_station_active);
        }

        selectedStation.handleIconAnimation(selectedStation.animationFlag, imageView, selectedStation, "BaseStation");
    }

    /**
     * Data binding to update the tracker's active total text visibility and value.
     */
    @BindingAdapter("trackerText")
    public static void setTrackerVisibilityAndText(TextView textView, VrStation selectedStation) {
        if (selectedStation == null) return;

        String headsetTracking = selectedStation.openVRHeadsetTracking;

        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
        boolean isIdle = selectedStation.statusHandler.isStationIdle();
        boolean lessThanOne = selectedStation.baseStationsActive == 0;
        //Headset is not tracking or has not been initiated
        boolean tracking = (headsetTracking.equals("Lost") || headsetTracking.equals("Off"));
        int visibility = isStatusOff || isIdle || lessThanOne || tracking ? View.INVISIBLE : View.VISIBLE;
        textView.setVisibility(visibility);

        String active = String.valueOf(selectedStation.trackersActive);

        // Set the text colour on the active status
        int color;
        if (selectedStation.baseStationsActive < 1) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            color = ContextCompat.getColor(MainActivity.getInstance(), R.color.green);
            textView.setTextColor(color);
        }
        textView.setText(active);
    }

    /**
     * Data binding to update the Base Station's image view source.
     */
    @BindingAdapter("tracker")
    public static void setTrackerImage(ImageView imageView, VrStation selectedStation) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
        if (isStatusOff) {
            //Station is off - no base stations expected
            imageView.setVisibility(View.GONE);
            return;
        }

        if (selectedStation.trackersActive < 1) {
            // Don't show unless we have active ones, only for users instructed to use them
            imageView.setVisibility(View.GONE);
            return;
        }

        if (selectedStation.trackersActive < selectedStation.trackersTotal) {
            //Station is on - active base stations less than 2
            imageView.setImageResource(R.drawable.vr_base_station_lost);

        } else {
            //Station is on - active base stations greater than 2 and an experience is running.
            imageView.setImageResource(R.drawable.vr_base_station_active);
        }
        imageView.setVisibility(View.VISIBLE);

        selectedStation.handleIconAnimation(selectedStation.animationFlag, imageView, selectedStation, "Trackers");
    }

    /**
     * Data binding to update the Vive connection image view source.
     */
    @BindingAdapter(value = {"station", "headsetManagerType"})
    public static void setSoftwareImage(ImageView imageView, VrStation selectedStation, String headsetManagerType) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
        boolean isIdle = selectedStation.statusHandler.isStationIdle();
        if (isStatusOff) {
            //Station is off - no connection expected
            imageView.setImageResource(headsetManagerType.equals("OpenVR") ? R.drawable.vr_steam_connection_gray : R.drawable.vr_vive_connection_gray);
            return;
        } else if (isIdle) {
            //Station is on - idle
            imageView.setImageResource(headsetManagerType.equals("OpenVR") ? R.drawable.vr_steam_connection_idle : R.drawable.vr_vive_connection_idle);
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
                //Station is on - vive and/or openVR is lost
                imageView.setImageResource(headsetManagerType.equals("OpenVR") ? R.drawable.vr_steam_connection_off :R.drawable.vr_vive_connection_off);
                break;
            case "Off":
                //Station is on - vive and/or openVR is off
                imageView.setImageResource(headsetManagerType.equals("OpenVR") ? R.drawable.vr_steam_connection_gray :R.drawable.vr_vive_connection_gray);
                break;
        }

        selectedStation.handleIconAnimation(selectedStation.animationFlag, imageView, selectedStation, headsetManagerType);
    }

    /**
     * Data binding to update the Vive status image view source.
     */
    @BindingAdapter(value = {"station", "headsetManagerIssue"})
    public static void setSoftwareStatusImage(ImageView imageView, VrStation selectedStation, String headsetManagerIssue) {
        if (selectedStation == null) return;

        boolean isStatusOff = selectedStation.statusHandler.isStationOffOrChanging();
        boolean isIdle = selectedStation.statusHandler.isStationIdle();
        boolean trackingOff = headsetManagerIssue.equals("OpenVR") ? selectedStation.openVRHeadsetTracking.equals("Off") : selectedStation.thirdPartyHeadsetTracking.equals("Off");
        int visibility = isStatusOff || isIdle || trackingOff ? View.INVISIBLE : View.VISIBLE;
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
    //endregion

    //region Station State & GameName Binding
    /**
     * Data binding to update the Station content flexbox background.
     */
    @BindingAdapter("stationState")
    public static void setStationStateBackground(FlexboxLayout flexbox, VrStation selectedStation) {
        if (selectedStation == null) return;

        boolean isStatusOn = selectedStation.statusHandler.isStationOnOrIdle();
        boolean hasState = selectedStation.stateHandler.hasState();
        boolean hasGame = selectedStation.applicationController.hasGame();

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
    public static void setStationStateTextAndVisibility(TextView textView, VrStation selectedStation) {
        if (selectedStation == null) return;

        //Set the visibility value
        boolean isStatusOn = selectedStation.statusHandler.isStationOnOrIdle();
        boolean hasState = selectedStation.stateHandler.hasState();
        boolean hasGame = selectedStation.applicationController.hasGame();
        int visibility = isStatusOn && (hasState || hasGame) ? View.VISIBLE : View.INVISIBLE;
        textView.setVisibility(visibility);

        //Stop the dot animation if it is anything besides Awaiting headset connection
        if(selectedStation.getState() == null || !selectedStation.stateHandler.isAwaitingHeadset()) {
            selectedStation.stopAnimateDots();
        }

        //Set the text value ('Not set' - backwards compatibility, default state when it is not sent across)
        if(selectedStation.stateHandler.isAvailable() || !hasGame) {
            //Show the state if the state is anything but Ready to go
            textView.setText(selectedStation.getState());

            //Start the dot animation if awaiting connection and animator is not already running
            if(selectedStation.stateHandler.isAwaitingHeadset()) {
                selectedStation.startAnimateDots(textView);
            }
        } else {
            selectedStation.stopAnimateDots();
            textView.setText(selectedStation.applicationController.getExperienceName());
        }
    }

    /**
     * Only enable the Idle Mode button if the Station is On and SteamVR has been opened, otherwise
     * there is internal processing occurring that should not be interrupted.
     */
    @BindingAdapter("stationIdleMode")
    public static void setStationIdleModeVisuals(MaterialButton materialButton, Station selectedStation) {
        if (!(selectedStation instanceof VrStation)) return;

        //Check if it should be enabled
        VrStation vrStation = (VrStation) selectedStation;
        boolean isOnOrIdle = vrStation.statusHandler.isStationOnOrIdle();
        boolean isAwaitingOrReady = vrStation.stateHandler.isAwaitingHeadsetOrReady();
        boolean isProcessing = vrStation.thirdPartyHeadsetTracking.equals("Lost") || vrStation.thirdPartyHeadsetTracking.equals("Connected");

        //Get the correct background colour - override if it is not enabled
        int colour;
        if (isOnOrIdle && isAwaitingOrReady && isProcessing) {
            colour = vrStation.statusHandler.getIdleModeColour(materialButton.getContext());
        } else {
            colour = ContextCompat.getColor(materialButton.getContext(), R.color.grey_card);
        }

        materialButton.setBackgroundColor(colour);
        materialButton.setEnabled(isOnOrIdle && isAwaitingOrReady && isProcessing);
    }

    /**
     * Starts an animation that updates a TextView with a sequence of dots, creating a loading effect.
     * The animation displays the text "Awaiting headset connection" followed by 1, 2, 3 dots in a loop,
     * with each dot appearing at one-second intervals. The sequence restarts after reaching three dots.
     * @param textView The TextView to animate.
     */
    private void startAnimateDots(final TextView textView) {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
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
                    if(station.stateHandler.isAwaitingHeadset()) {
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
    //endregion
}
