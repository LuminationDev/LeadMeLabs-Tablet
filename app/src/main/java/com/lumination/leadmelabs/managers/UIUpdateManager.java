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
                        DialogManager.gameLaunchedOnStation(station.id);
                        String[] data = additionalData.split(":", 2);
                        if (!ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            MainActivity.runOnUI(() -> {
                                DialogManager.createBasicDialog(
                                        "Experience launch failed",
                                        "Launch of " + data[1] + " failed on " + station.name
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
                                        "A steam error occurred on " + station.name + ". Check the station for more details."
                                );
                            });
                        }
                    }
                    if (additionalData.startsWith("LostHeadset")) {
                        Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(Integer.parseInt(source.split(",")[1]));
                        if (station != null && !ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getHideStationControls().getValue()) {
                            MainActivity.runOnUI(() -> {
                                DialogManager.createBasicDialog(
                                        "Lost VR Headset Connection",
                                        station.name + " lost connection to the VR headset. This is normally caused by a flat battery."
                                );
                            });
                        }
                    }
                    break;
                case "Automation":
                    if (additionalData.startsWith("Appliances")) {
                        cbusActiveAppliances(additionalData);
                    }
                    if (additionalData.startsWith("Update")) {
                        syncAppliances(additionalData);
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
                    break;
                case "gameId":
                    station.gameId = value;
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
     * Update the appliance list with the most active directly from the CBUS.
     */
    private static void cbusActiveAppliances(String jsonString) throws JSONException {
        String requiredString = jsonString.substring(jsonString.indexOf("["), jsonString.indexOf("]") + 1);
        JSONArray json = new JSONArray(requiredString);

        MainActivity.runOnUI(() -> {
            try {
                ViewModelProviders.of(MainActivity.getInstance()).get(ApplianceViewModel.class).setActiveAppliances(json);
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
     *                      //- [5]msg (contains ID from CBUS in brackets [xxxxxxxx])
     */
    private static void syncAppliances(String additionalData) {
        String[] values = additionalData.split(":");

        Log.e("NUC DATA", values[1]);

        switch(values[1]) {
            case "computer":
                ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).syncStationStatus(values[3], values[4]);
                break;
            case "scene":
                ViewModelProviders.of(MainActivity.getInstance()).get(ApplianceViewModel.class).updateActiveSceneList(values[2], values[3]);
                break;
            case "appliance":
                ViewModelProviders.of(MainActivity.getInstance()).get(ApplianceViewModel.class).updateActiveApplianceList(values[3], values[4]);
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
