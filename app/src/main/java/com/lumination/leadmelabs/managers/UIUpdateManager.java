package com.lumination.leadmelabs.managers;

import android.util.Log;

import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.Gson;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.VrStation;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.classes.SegmentExperienceEvent;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.QaChecks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.view.View;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.sentry.Sentry;

/**
 * Expand this/change this in the future to individual namespace handlers, just here to stop
 * code creep within the main activity and network service.
 * Responsible for updating the UI related to fragments.
 */
public class UIUpdateManager {
    public final static String TAG = "UIUpdateManager";

    /**
     * Split a message into individual parts to determine what UI elements are in need of updating.
     * @param message A string of values separated by ':'.
     */
    public static void determineUpdate(String message) {
        String[] messageParts = message.split(":", 4);
        String source = messageParts[0];
        String destination = messageParts[1];
        String actionNamespace = messageParts[2];
        String additionalData = messageParts.length > 3 ? messageParts[3] : null;

        if (!destination.equals("Android")) {
            return;
        }

        // Reset connection status indicators
        resetConnectionStatusIndicators();

        // Dismiss reconnect dialog if showing
        dismissReconnectDialog();

        try {
            if (actionNamespace.equals("Ping") && additionalData != null && !additionalData.isEmpty()) {
                updateStationsFromJson(additionalData);
                return;
            }

            //Early exit, voids null checks
            if(additionalData == null) {
                return;
            }

            switch (actionNamespace) {
                case Constants.MESSAGE_TYPE:
                    handleMessageTypeUpdate(additionalData);
                    break;
                case Constants.LAB_LOCATION:
                    handleLabLocationUpdate(additionalData);
                    break;
                case Constants.STATIONS:
                    handleStationsUpdate(additionalData);
                    break;
                case Constants.APPLIANCES:
                    handleAppliancesUpdate(additionalData);
                    break;
                case Constants.STATION:
                    handleStationUpdate(additionalData, source);
                    break;
                case Constants.AUTOMATION:
                    handleAutomationUpdate(additionalData);
                    break;
                case Constants.SEGMENT:
                    handleSegmentUpdate(additionalData);
                    break;
                case Constants.QA:
                    handleQaUpdate(additionalData);
                    break;
                default:
                    break;
            }
        } catch(JSONException e) {
            Log.e(TAG, "Unable to handle JSON request");
            e.printStackTrace();
        }
    }

    /**
     * Helper method to reset connection status indicators
     */
    private static void resetConnectionStatusIndicators() {
        MainActivity.hasNotReceivedPing = 0;
        MainActivity.attemptedRefresh = false;
        MainActivity.reconnectionIgnored = false;
    }

    /**
     * Helper method to dismiss reconnect dialog
     */
    private static void dismissReconnectDialog() {
        if (DialogManager.reconnectDialog != null && DialogManager.reconnectDialog.isShowing()) {
            FlexboxLayout reconnect = DialogManager.reconnectDialog.findViewById(R.id.reconnect_loader);
            if (reconnect != null) {
                reconnect.setVisibility(View.GONE);
            }
            DialogManager.reconnectDialog.dismiss();
        }
    }

    //region Message Handlers
    /**
     * Handles the update for MessageType action namespace.
     * This method processes updates related to message types.
     *
     * @param additionalData The additional data associated with the update.
     */
    private static void handleMessageTypeUpdate(String additionalData) {
        if (additionalData.startsWith("Update")) {
            String[] split = additionalData.split(":");

            if (split.length > 1 && split[1].equals("Json")) {
                MainActivity.isNucJsonEnabled = true;
                Log.e("JSON", "JSON ENABLED");
            }
        }
    }

    /**
     * Handles the update for LabLocation action namespace.
     * This method processes updates related to laboratory locations.
     *
     * @param additionalData The additional data associated with the update.
     */
    private static void handleLabLocationUpdate(String additionalData) throws JSONException {
        JSONObject details = new JSONObject(additionalData);
        String location = details.optString("location", "Unknown");

        if (location.equals("Unknown")) {
            Sentry.captureMessage("Both the Tablet and NUC location is not set. NUC address is: " + SettingsFragment.mViewModel.getNucAddress());
            return;
        }

        MainActivity.runOnUI(() -> {
            SettingsFragment.mViewModel.setLabLocation(location);
            Segment.updateUserId();
        });
    }

    /**
     * Handles the update for Stations action namespace.
     * This method processes updates related to stations.
     *
     * @param additionalData The additional data associated with the update.
     */
    private static void handleStationsUpdate(String additionalData) throws JSONException {
        if (additionalData.startsWith("List")) {
            updateStations(additionalData.split(":", 2)[1]);
        }
    }

    /**
     * Handles the update for Appliances action namespace.
     * This method processes updates related to appliances.
     *
     * @param additionalData The additional data associated with the update.
     */
    private static void handleAppliancesUpdate(String additionalData) throws JSONException {
        if (additionalData.startsWith("List")) {
            updateAppliances(additionalData.split(":", 2)[1]);
        }
        if (additionalData.startsWith("ProjectorSlowDown")) {
            String projectorName = additionalData.split(",")[1];
            String projectorId = additionalData.split(",")[2];

            List<Appliance> applianceList = ApplianceFragment.mViewModel.getAppliances().getValue();
            if(applianceList == null) {
                return;
            }

            //Check if the projector is in a locked room
            for(Appliance appliance: applianceList) {
                if(Objects.equals(appliance.id, projectorId)) {
                    if(SettingsFragment.checkLockedRooms(appliance.room)) {
                        MainActivity.runOnUI(() ->
                                DialogManager.createBasicDialog(
                                        "Warning",
                                        projectorName + " has already been powered on or off in the last 10 seconds. The automation system performs best when appliances are not repeatedly turned on and off. Projectors need up to 10 seconds between turning on and off."
                                )
                        );
                    }
                }
            }
        }
    }

    /**
     * Handles the update for Station action namespace.
     * This method processes updates related to individual stations.
     *
     * @param additionalData The additional data associated with the update.
     * @param source         The source identifier of the station.
     */
    private static void handleStationUpdate(String additionalData, String source) throws JSONException {
        if (additionalData.startsWith("SetValue")) {
            Log.e("Apps", additionalData);

            String[] keyValue = additionalData.split(":", 3);
            String key = keyValue[1];
            String value = keyValue[2];
            updateStation(source.split(",")[1], key, value);
        }

        if (additionalData.startsWith("DeviceStatus")) {
            //[0] - 'DeviceStatus'
            //[1] - DeviceType
            //[2] - Values
            String[] keyValue = additionalData.split(":", 3); //Only split the key off
            String key = keyValue[1];
            String value = keyValue[2];
            updateStationDevices(source.split(",")[1], key, value);
        }

        //Everything below should only trigger if within the locked room
        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
        HashSet<String> rooms = SettingsFragment.mViewModel.getLockedIfEnabled().getValue();

        //Check if the computer is in the locked room
        if(rooms != null) {
            if(rooms.size() != 0 && !rooms.contains(station.room)) {
                return;
            }
        }

        //Early return statement
        if(station == null || Boolean.TRUE.equals(ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue())) {
            return;
        }

        if (additionalData.startsWith("SteamappsCorrupted")) {
            MainActivity.runOnUI(() ->
                    DialogManager.createBasicDialog(
                            "Steam application corruption",
                            "Place a battery in the headset of " + station.name + ". After the headset is connected please select Restart VR System on " + station.name
                    )
            );
        }
        if (additionalData.startsWith("SteamVRError")) {
            MainActivity.runOnUI(() -> {
                BooleanCallbackInterface confirmAppRestartCallback = confirmationResult -> {
                    if (confirmationResult) {
                        DialogManager.buildShutdownOrRestartDialog(MainActivity.getInstance(), "Restart", new int[]{station.id}, null);
                    }
                };

                DialogManager.createConfirmationDialog(
                        "SteamVR Error",
                        "SteamVR has encountered an error and cannot recover automatically. Please restart the Station 102",
                        confirmAppRestartCallback,
                        "Cancel",
                        "Restart",
                        false);
            });
        }
        if (additionalData.startsWith("GameLaunchFailed")) {
            DialogManager.gameLaunchedOnStation(station.id);
            String[] data = additionalData.split(":", 2);

            if (data.length < 2) {
                return;
            }

            String experienceName = data[1];
            MainActivity.runOnUI(() ->
                    DialogManager.createBasicDialog(
                            "Experience launch failed",
                            "Launch of " + experienceName + " failed on " + station.name
                    )
            );

            //Collect the station to find the extra experience details required
            Application application = station.applicationController.findApplicationByName(experienceName);
            if (application == null) {
                return;
            }

            //Send data to Segment
            SegmentExperienceEvent event = new SegmentExperienceEvent(
                    SegmentConstants.Event_Experience_Failed,
                    station.id,
                    experienceName,
                    application.getId(),
                    application.getType());
            Segment.trackAction(SegmentConstants.Event_Type_Experience, event);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(station.id));
                put("experience_name", experienceName);
            }};
            FirebaseManager.logAnalyticEvent("experience_launch_failed", analyticsAttributes);
        }
        if (additionalData.startsWith("PopupDetected")) {
            MainActivity.runOnUI(() ->
                    DialogManager.createBasicDialog(
                            "Cannot launch experience",
                            "The experience launching on " + station.name + " requires additional input from the keyboard."
                    )
            );
        }
        if (additionalData.startsWith("AlreadyLaunchingGame")) {
            DialogManager.gameLaunchedOnStation(station.id);
            MainActivity.runOnUI(() ->
                    DialogManager.createBasicDialog(
                            "Cannot launch experience",
                            "Unable to launch experience on " + station.name + " as it is already attempting to launch an experience. You must wait until an experience has launched before launching another one. If this issue persists, try restarting the VR system."
                    )
            );
        }
        if (additionalData.startsWith("SteamError")) {
            MainActivity.runOnUI(() ->
                    DialogManager.createBasicDialog(
                            "Steam error",
                            "A Steam error occurred on " + station.name + ". Check the station for more details."
                    )
            );

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(station.id));
            }};
            FirebaseManager.logAnalyticEvent("steam_error", analyticsAttributes);
        }
        if (additionalData.startsWith("LostHeadset")) {
            MainActivity.runOnUI(() ->
                    DialogManager.createBasicDialog(
                            MainActivity.getInstance().getResources().getString(R.string.oh_no),
                            station.name + "'s headset has disconnected. Please check the battery is charged.",
                            station.name
                    )
            );

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(station.id));
            }};
            FirebaseManager.logAnalyticEvent("headset_disconnected", analyticsAttributes);
        }
        if (additionalData.startsWith("FoundHeadset")) {
            //Close the dialog relating to that
            DialogManager.closeOpenDialog(
                    MainActivity.getInstance().getResources().getString(R.string.oh_no),
                    station.name);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(station.id));
            }};
            FirebaseManager.logAnalyticEvent("headset_reconnected", analyticsAttributes);
        }
        if (additionalData.startsWith("HeadsetTimeout")) {
            MainActivity.runOnUI(() ->
                    DialogManager.createBasicDialog(
                            MainActivity.getInstance().getResources().getString(R.string.oh_no),
                            station.name + "'s headset connection has timed out. Please connect the battery and launch the experience again."
                    )
            );
        }
        if (additionalData.startsWith("FailedRestart")) {
            MainActivity.runOnUI(() ->
                    DialogManager.createBasicDialog(
                            "Failed to restart station",
                            station.name + " was not able to restart all required VR processes. Try again, or shut down and reboot the station."
                    )
            );

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_id", String.valueOf(station.id));
            }};
            FirebaseManager.logAnalyticEvent("restart_failed", analyticsAttributes);
        }
    }

    /**
     * Handles the update for Automation action namespace.
     * This method processes updates related to automation tasks.
     *
     * @param additionalData The additional data associated with the update.
     */
    private static void handleAutomationUpdate(String additionalData) {
        if (additionalData.startsWith("Update")) {
            syncAppliances(additionalData);
        }
    }

    /**
     * Handles the update for Analytics action namespace.
     * This method processes updates related to analytics data.
     *
     * @param additionalData The additional data associated with the update.
     */
    private static void handleSegmentUpdate(String additionalData) throws JSONException {
        JSONObject details = new JSONObject(additionalData);
        String sessionId = details.optString("SessionId", "");
        String sessionStart = details.optString("SessionStart", "");


        if (sessionStart.isEmpty()) {
            sessionStart = null;
        }

        if (sessionId.isEmpty()) return;
        Segment.setSessionFromExternal(sessionId, sessionStart);
    }

    /**
     * Handles the update for QA action namespace.
     * This method processes updates related to quality assurance tasks.
     *
     * @param additionalData The additional data associated with the update.
     */
    private static void handleQaUpdate(String additionalData) throws JSONException {
        JSONObject request = new JSONObject(additionalData);
        if (request.getString("action").equals("Connect")) {
            JSONObject response = new JSONObject();
            response.put("response", "TabletConnected");
            response.put("responseData", new JSONObject());
            response.put("ipAddress", NetworkService.getIPAddress());
            NetworkService.sendMessage("NUC", "QA", (new Gson().toJson(response.toString())));
            return;
        }

        QaChecks qaChecks = new QaChecks();

        if (request.getString("action").equals("RunAuto")) {
            List<QaChecks.QaCheck> qaCheckList = qaChecks.runQa();

            JSONObject response = new JSONObject();
            response.put("response", "TabletChecks");
            response.put("responseData", (new Gson().toJson(qaCheckList)));
            response.put("ipAddress", NetworkService.getIPAddress());
            NetworkService.sendMessage("NUC", "QA", (new Gson().toJson(response.toString())));
        }

        if (request.getString("action").equals("RunGroup")) {
            String group = request.getJSONObject("actionData").getString("group");
            JSONObject response = new JSONObject();
            response.put("response", "RunTabletGroup");
            response.put("ipAddress", NetworkService.getIPAddress());
            JSONObject responseData = new JSONObject();
            responseData.put("group", group);
            switch (group) {
                case "network_checks": {
                    List<QaChecks.QaCheck> qaCheckList = qaChecks.runNetworkChecks();
                    responseData.put("data", (new Gson().toJson(qaCheckList)));
                    break;
                }
                case "security_checks": {
                    List<QaChecks.QaCheck> qaCheckList = qaChecks.runSecurityChecks();
                    responseData.put("data", (new Gson().toJson(qaCheckList)));
                    break;
                }
            }
            response.put("responseData", responseData);

            NetworkService.sendMessage("NUC", "QA", (new Gson().toJson(response.toString())));
        }

        if (request.getString("action").equals("RunAuto")) {
            List<QaChecks.QaCheck> qaCheckList = qaChecks.runQa();

            JSONObject response = new JSONObject();
            response.put("response", "RunTabletGroup");
            response.put("responseData", (new Gson().toJson(qaCheckList)));
            response.put("ipAddress", NetworkService.getIPAddress());
            NetworkService.sendMessage("NUC", "QA", (new Gson().toJson(response.toString())));
        }
    }
    //endregion

    /**
     * Updates the Station objects with data parsed from JSON entries.
     *
     * @param additionalData The data sent from the NUC.
     */
    private static void updateStationsFromJson(String additionalData) {
        try {
            JSONObject data = new JSONObject(additionalData);
            JSONObject responseObject = data.getJSONObject("responseData");

            StationsViewModel stationsViewModel = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class);

            Iterator<String> keys = responseObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject entry = responseObject.getJSONObject(key);

                Station station = stationsViewModel.getStationById(Integer.parseInt(key));
                if (station == null) {
                    return;
                }

                // General Station statuses
                updateStationField(station, entry, "state");
                updateStationField(station, entry, "status");
                updateStationField(station, entry, "gameName");
                updateStationField(station, entry, "gameId");
                updateStationField(station, entry, "gameType");

                // Stop here if it is a content station
                if (!(station instanceof VrStation)) {
                    MainActivity.runOnUI(() -> stationsViewModel.updateStationById(Integer.parseInt(key), station));
                    continue;
                }

                VrStation vrStation = (VrStation) station;

                // VR device statuses
                JSONObject devices = entry.optJSONObject("devices");
                if (devices != null) {
                    updateDeviceField(vrStation, devices, "thirdPartyHeadsetTracking");
                    updateDeviceField(vrStation, devices, "openVRHeadsetTracking");
                    updateDeviceField(vrStation, devices, "leftControllerTracking");
                    updateDeviceField(vrStation, devices, "rightControllerTracking");

                    // Handle integer fields separately
                    vrStation.leftControllerBattery = devices.optInt("leftControllerBattery", vrStation.leftControllerBattery);
                    vrStation.rightControllerBattery = devices.optInt("rightControllerBattery", vrStation.rightControllerBattery);
                    vrStation.baseStationsActive = devices.optInt("baseStationsActive", vrStation.baseStationsActive);
                    vrStation.baseStationsTotal = devices.optInt("baseStationsTotal", vrStation.baseStationsTotal);
                }

                MainActivity.runOnUI(() -> stationsViewModel.updateStationById(Integer.parseInt(key), vrStation));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates a specific field in a Station object based on the provided JSON data.
     * @param station The Station object to update.
     * @param entry JSON object containing data for the specified field.
     * @param field The field to update in the Station object.
     */
    private static void updateStationField(Station station, JSONObject entry, String field) {
        if (entry.has(field)) {
            String value = entry.optString(field, "NA");
            if (value.equals("null") || value.equals("NA")) {
                value = null;
            }

            switch (field) {
                case "state":
                    station.state = value;
                    break;
                case "status":
                    station.status = value;
                    break;
                case "gameName":
                    station.applicationController.setExperienceName(value);
                    break;
                case "gameId":
                    station.applicationController.setExperienceId(value);
                    break;
                case "gameType":
                    station.applicationController.setExperienceType(value);
                    break;
            }
        }
    }

    /**
     * Updates a specific field in a VirtualStation object based on the provided JSON data.
     * @param vrStation The VirtualStation object to update.
     * @param devices JSON object containing data for the specified field.
     * @param field The field to update in the VirtualStation object.
     */
    private static void updateDeviceField(VrStation vrStation, JSONObject devices, String field) {
        if (devices.has(field)) {
            String value = devices.optString(field, "NA");
            if (!value.equals("NA")) {
                switch (field) {
                    case "thirdPartyHeadsetTracking":
                        vrStation.thirdPartyHeadsetTracking = value;
                        break;
                    case "openVRHeadsetTracking":
                        vrStation.openVRHeadsetTracking = value;
                        break;
                    case "leftControllerTracking":
                        vrStation.leftControllerTracking = value;
                        break;
                    case "rightControllerTracking":
                        vrStation.rightControllerTracking = value;
                        break;
                }
            }
        }
    }


    //Simplify the functions below to a generic one
    private static void updateStations(String jsonString) throws JSONException {
        JSONArray json = new JSONArray(jsonString);

        MainActivity.runOnUI(() -> {
            try {
                ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).setStations(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private static void updateStation(String stationId, String attribute, String value) throws JSONException {
        MainActivity.runOnUI(() -> {
            Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(stationId));
            if (station == null) {
                return;
            }
            switch (attribute) {
                case "session":
                    //Do no notify if the station is in another room
                    if(!SettingsFragment.checkLockedRooms(station.room)) {
                        return;
                    }

                    if (value.equals("Ended")) {
                        DialogManager.sessionEndedOnStation(station.id);

                        if (Boolean.TRUE.equals(ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue())) {
                            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                                put("station_id", String.valueOf(station.id));
                            }};
                            FirebaseManager.logAnalyticEvent("session_ended", analyticsAttributes);
                        }
                    }
                    if (value.equals("Restarted")) {
                        //Stop flashing the VR icons
                        ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).setStationFlashing(station.id, false);
                        DialogManager.vrSystemRestartedOnStation(station.id);
                    }
                    break;

                case "status":
                    station.status = value;
                    if(value.equals("On")) { station.cancelStatusCheck(); }
                    if(value.equals("Off")) {
                        if (station instanceof VrStation) {
                            VrStation vrStation = (VrStation) station; //safe cast
                            vrStation.initiateVRDevices();
                        }
                        station.state = "";
                    }
                    break;

                case "state":
                    station.state = value;
                    break;

                case "name":
                    station.setName(value);
                    break;

                case "gameId":
                    station.applicationController.setExperienceId(value);
                    break;

                case "gameType":
                    station.applicationController.setExperienceType(value);
                    break;

                case "gameName":
                    station.applicationController.setExperienceName(value);

                    //Reset the selected application
                    if  ((value != null ? value.length() : 0) == 0) {
                        ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).setSelectedApplication(null);
                    }

                    //Do no notify if the station is in another room
                    if(!SettingsFragment.checkLockedRooms(station.room)) {
                        return;
                    }

                    if ((value != null ? value.length() : 0) > 0 && !value.equals("No session running")) {
                        DialogManager.gameLaunchedOnStation(station.id);
                    }
                    break;

                //BACKWARDS COMPATIBILITY
                case "installedApplications":
                    station.applicationController.setApplicationsFromJsonString(value);
                    break;

                case "installedJsonApplications":
                    try {
                        JSONArray jsonArray = new JSONArray(value);
                        station.applicationController.setApplicationsFromJson(jsonArray);
                    } catch (JSONException e) {
                        Sentry.captureException(e);
                    }
                    break;

                case "volume":
                    station.audioController.setVolume(Integer.parseInt(value));

                    if (Boolean.TRUE.equals(ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue())) {
                        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                            put("station_id", String.valueOf(station.id));
                            put("volume_level", value);
                        }};
                        FirebaseManager.logAnalyticEvent("volume_changed", analyticsAttributes);
                    }
                    break;

                case "muted":
                    station.audioController.setMuted(Boolean.parseBoolean(value));
                    break;

                case "activeAudioDevice":
                    station.audioController.setActiveAudioDevice(value);
                    break;

                case "audioDevices":
                    station.audioController.setAudioDevices(value);
                    break;

                case "activeVideoPlaybackTime":
                    station.videoController.updateVideoPlaybackTime(value);
                    break;

                case "activeVideoFile":
                    station.videoController.setActiveVideo(value);
                    break;

                case "videoPlayerDetails":
                    station.videoController.updateVideoPlayerDetails(value);
                    break;

                case "videoFiles":
                    station.videoController.setVideos(value);
                    break;

                case "details":
                    MainActivity.runOnUI(() -> {
                        try {
                            ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).setApplicationDetails(new JSONObject(value));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                    break;
                case "steamCMD":
                    if(value.equals("required")) { station.requiresSteamGuard = true; }
                    if(value.equals("configured") && station.requiresSteamGuard) {
                        if(DialogManager.steamGuardEntryDialog == null) return;

                        if(DialogManager.steamGuardEntryDialog.isShowing()) {
                            DialogManager.steamGuardEntryDialog.dismiss();
                        }
                        DialogManager.createUpdateDialog("Steam configuration", "SteamCMD has been successfully configured on station " + stationId);
                        station.requiresSteamGuard = false;
                    }
                    if(value.equals("failure")) {
                        if(DialogManager.steamGuardEntryDialog == null) return;

                        if(DialogManager.steamGuardEntryDialog.isShowing()) {
                            DialogManager.steamGuardEntryDialog.dismiss();
                        }

                        BooleanCallbackInterface confirmConfigCallback = confirmationResult -> {
                            if (confirmationResult) {
                                DialogManager.steamGuardKeyEntry(Integer.parseInt(stationId));
                            }
                        };

                        DialogManager.createConfirmationDialog(
                                "Configuration Failure",
                                "An incorrect key has been provided, please try again or cancel",
                                confirmConfigCallback,
                                "Cancel",
                                "Try again",
                                false);
                    }
                    break;
            }
            ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(Integer.parseInt(stationId), station);
        });
    }

    /**
     * Specifically update a Station's VR devices. This includes:
     *      - Headset:              Tracking
     *      - Left Controller:      Tracking & Battery
     *      - Right Controller:     Tracking & Battery
     *      - Base Stations:        Active & Total
     * @param stationId A string of an ID associated to a Station.
     * @param attribute A string of the device type to update.
     * @param value A string of values separated by ':'.
     */
    private static void updateStationDevices(String stationId, String attribute, String value) throws JSONException {
        MainActivity.runOnUI(() -> {
            Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(stationId));

            //Check for safe cast
            if (!(station instanceof VrStation)) {
                return;
            }

            VrStation vrStation = (VrStation)station;

            String[] values = value.split(":", 3);
            switch (attribute) {
                case "Headset":
                    if(validateLength(values.length, 3)) {
                        Sentry.captureMessage(
                                ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getLabLocation()
                                + ": Update Headset, not a valid argument - " + value);
                        return;
                    }

                    updateHeadset(vrStation, values[0], values[1], values[2]);
                    break;

                case "Controller":
                    if(validateLength(values.length, 3)) {
                        Sentry.captureMessage(
                                ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getLabLocation()
                                + ": Update Controller, not a valid argument - " + value);
                        return;
                    }

                    updateController(vrStation, values[0], values[1], values[2]);
                    break;

                case "BaseStation":
                    if(validateLength(values.length, 2)) {
                        Sentry.captureMessage(
                                ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getLabLocation()
                                + ": Update BaseStation, not a valid argument - " + value);
                        return;
                    }

                    updateBaseStations(vrStation, values[0], values[1]);
                    break;
            }

            ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(Integer.parseInt(stationId), vrStation);
        });
    }

    private static void updateHeadset(VrStation station, String trackingType, String propertyType, String value) {
        if (propertyType.equals("tracking")) {
            if(trackingType.equals("OpenVR")) {
                station.openVRHeadsetTracking = value;
            } else {
                station.thirdPartyHeadsetTracking = value;
            }
        }
    }

    private static void updateController(VrStation station, String controllerType, String propertyType, String value) {
        if (propertyType.equals("tracking")) {
            if (controllerType.equals("Left")) {
                station.leftControllerTracking = value;
            } else if (controllerType.equals("Right")) {
                station.rightControllerTracking = value;
            }
        }
        else if (propertyType.equals("battery")) {
            int batteryLevel = Integer.parseInt(value);
            if (controllerType.equals("Left")) {
                station.leftControllerBattery = batteryLevel;
            } else if (controllerType.equals("Right")) {
                station.rightControllerBattery = batteryLevel;
            }
        }
    }

    private static void updateBaseStations(VrStation station, String active, String total) {
        station.baseStationsActive = Integer.parseInt(active);
        station.baseStationsTotal = Integer.parseInt(total);
    }

    private static Boolean validateLength(int length, int limit) {
        return length < limit;
    }

    private static void updateAppliances(String jsonString) throws JSONException {
        JSONArray json = new JSONArray(jsonString);

        MainActivity.runOnUI(() -> {
            try {
                ViewModelProviders.of(MainActivity.getInstance()).get(ApplianceViewModel.class).setAppliances(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Another tablet has set a new value for a CBUS object, determine what it was and what cards
     * need updating.
     * @param additionalData A string containing the values necessary to update the cards.
     *                      //- [1]type (scene, appliance, computer)
     *                      //- [2]room
     *                      //- [3]id
     *                      //- [4]value
     *                      //- [5]ip address of the tablet that the command originated at
     *                      //- [6]msg (contains ID from CBUS in brackets [xxxxxxxx])
     */
    private static void syncAppliances(String additionalData) {
        String[] values = additionalData.split(":");
        String id = values[1];
        String value = values[2];
        String ipAddress = values.length > 3 ? values[3] : null;
        String group = id.split("-")[0];

        switch(group) {
            case "scenes":
                ViewModelProviders.of(MainActivity.getInstance()).get(ApplianceViewModel.class).updateActiveSceneList(id);
                break;
            case "projectors":
            case "computers":
            case "LED rings":
            case "lights":
            case "sources":
            case "splicers":
            case "blinds":
            case "LED walls":
                ViewModelProviders.of(MainActivity.getInstance()).get(ApplianceViewModel.class).updateActiveApplianceList(id, value, ipAddress);
                break;
        }
    }
}
