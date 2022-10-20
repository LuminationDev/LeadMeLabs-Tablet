package com.lumination.leadmelabs.managers;

import android.util.Log;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import android.view.View;

import java.util.HashMap;

/**
 * Expand this/change this in the future to individual namespace handlers, just here to stop
 * code creep within the main activity and network service.
 *
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

        try {
            switch (actionNamespace) {
                case "Ping":
                    MainActivity.hasNotReceivedPing = 0;
                    if (DialogManager.reconnectDialog != null) {
                        DialogManager.reconnectDialog.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
                        DialogManager.reconnectDialog.dismiss();
                    }
                    break;
                case "Stations":
                    if (additionalData.startsWith("List")) {
                        updateStations(additionalData.split(":", 2)[1]);
                    }
                    break;
                case "Appliances":
                    if (additionalData.startsWith("List")) {
                        updateAppliances(additionalData.split(":", 2)[1]);
                    }
                    break;
                case "Station":
                    if (additionalData.startsWith("SetValue")) {
                        String[] keyValue = additionalData.split(":", 3);
                        String key = keyValue[1];
                        String value = keyValue[2];
                        updateStation(source.split(",")[1], key, value);
                    }
                    if (additionalData.startsWith("GameLaunchFailed")) {
                        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
                        if (station != null && !ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            DialogManager.gameLaunchedOnStation(station.id);
                            String[] data = additionalData.split(":", 2);
                            MainActivity.runOnUI(() -> {
                                DialogManager.createBasicDialog(
                                        "Experience launch failed",
                                        "Launch of " + data[1] + " failed on " + station.name
                                );
                            });
                        }

                        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                            put("station_id", String.valueOf(station.id));
                        }};
                        FirebaseManager.logAnalyticEvent("experience_launch_failed", analyticsAttributes);
                    }
                    if (additionalData.startsWith("AlreadyLaunchingGame")) {
                        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
                        if (station != null && !ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            DialogManager.gameLaunchedOnStation(station.id);
                            MainActivity.runOnUI(() -> {
                                DialogManager.createBasicDialog(
                                        "Cannot launch experience",
                                        "Unable to launch experience on " + station.name + " as it is already attempting to launch an experience. You must wait until an experience has launched before launching another one. If this issue persists, try restarting the VR system."
                                );
                            });
                        }
                    }
                    if (additionalData.startsWith("SteamError")) {
                        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
                        if (station != null && !ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            MainActivity.runOnUI(() -> {
                                DialogManager.createBasicDialog(
                                        "Steam error",
                                        "A Steam error occurred on " + station.name + ". Check the station for more details."
                                );
                            });

                            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                                put("station_id", String.valueOf(station.id));
                            }};
                            FirebaseManager.logAnalyticEvent("steam_error", analyticsAttributes);
                        }
                    }
                    if (additionalData.startsWith("LostHeadset")) {
                        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
                        if (station != null && !ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            MainActivity.runOnUI(() -> {
                                DialogManager.createBasicDialog(
                                        MainActivity.getInstance().getResources().getString(R.string.oh_no),
                                        station.name + "'s headset has disconnected. Please check the battery is charged.",
                                        station.name
                                );
                            });

                            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                                put("station_id", String.valueOf(station.id));
                            }};
                            FirebaseManager.logAnalyticEvent("headset_disconnected", analyticsAttributes);
                        }
                    }
                    if (additionalData.startsWith("FoundHeadset")) {
                        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
                        if (station != null && !ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            //Close the dialog relating to that
                            DialogManager.closeOpenDialog(
                                    MainActivity.getInstance().getResources().getString(R.string.oh_no),
                                    station.name);

                            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                                put("station_id", String.valueOf(station.id));
                            }};
                            FirebaseManager.logAnalyticEvent("headset_reconnected", analyticsAttributes);
                        }
                    }
                    if (additionalData.startsWith("HeadsetTimeout")) {
                        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
                        if (station != null && !ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            MainActivity.runOnUI(() -> {
                                DialogManager.createBasicDialog(
                                        MainActivity.getInstance().getResources().getString(R.string.oh_no),
                                        station.name + "'s headset connection has timed out. Please connect the battery and launch the experience again."
                                );
                            });
                        }
                    }
                    if (additionalData.startsWith("FailedRestart")) {
                        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
                        if (station != null && !ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            MainActivity.runOnUI(() -> {
                                DialogManager.createBasicDialog(
                                        "Failed to restart station",
                                        station.name + " was not able to restart all required VR processes. Try again, or shut down and reboot the station."
                                );
                            });

                            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                                put("station_id", String.valueOf(station.id));
                            }};
                            FirebaseManager.logAnalyticEvent("restart_failed", analyticsAttributes);
                        }
                    }
                    break;
                case "Automation":
                    if (additionalData.startsWith("Update")) {
                        syncAppliances(additionalData);
                        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{}};
                        FirebaseManager.logAnalyticEvent("appliances_updated", analyticsAttributes);
                    }

                    break;
                case "Scanner":
                    updateNUCAddress(additionalData);
                    MainActivity.getInstance().findViewById(R.id.reconnect_overlay).setVisibility(View.GONE);
            }
        } catch(JSONException e) {
            Log.e(TAG, "Unable to handle JSON request");
            e.printStackTrace();
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
                    if (value.equals("Ended")) {
                        DialogManager.sessionEndedOnStation(station.id);

                        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                            put("station_id", String.valueOf(station.id));
                        }};
                        FirebaseManager.logAnalyticEvent("session_ended", analyticsAttributes);
                    }
                    if (value.equals("Restarted")) {
                        DialogManager.sessionRestartedOnStation(station.id);
                    }
                    break;
                case "status":
                    station.status = value;
                    if(value.equals("On")) { station.cancelStatusCheck(); }
                    break;
                case "volume":
                    station.volume = Integer.parseInt(value);

                    HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                        put("station_id", String.valueOf(station.id));
                        put("volume_level", value);
                    }};
                    FirebaseManager.logAnalyticEvent("volume_changed", analyticsAttributes);
                    break;
                case "gameId":
                    station.gameId = value;
                    break;
                case "name":
                    station.setName(value);
                    break;
                case "gameName":
                    station.gameName = value;
                    if (value.length() > 0 && !value.equals("No session running")) {
                        DialogManager.gameLaunchedOnStation(station.id);
                    }
                    break;
                case "steamApplications":
                    station.setSteamApplicationsFromJsonString(value);
            }
            ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(Integer.parseInt(stationId), station);
        });
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
                ViewModelProviders.of(MainActivity.getInstance()).get(ApplianceViewModel.class).updateActiveApplianceList(id, value, ipAddress);
                break;
        }
    }

    /**
     * Update the NUC address based on the results from the scanner.
     * @param response A string representing the IP address of the NUC.
     */
    private static void updateNUCAddress(String response) {
        MainActivity.runOnUI(() ->
                ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).setNucAddress(response)
        );
    }
}
