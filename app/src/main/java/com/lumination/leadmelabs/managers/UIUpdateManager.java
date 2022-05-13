package com.lumination.leadmelabs.managers;

import android.util.Log;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.Zone;
import com.lumination.leadmelabs.ui.appliance.LightFragment;
import com.lumination.leadmelabs.ui.zones.ZonesFragment;
import com.lumination.leadmelabs.ui.nuc.NucFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import org.json.JSONArray;
import org.json.JSONException;

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
                case "Stations":
                    if (additionalData.startsWith("List")) {
                        updateStations(additionalData.split(":", 2)[1]);
                    }
                    break;
                case "Zones":
                    if (additionalData.startsWith("List")) {
                        updateZones(additionalData.split(":", 2)[1]);
                    }
                    if (additionalData.startsWith("SetScene")) {
                        updateSelectedScene(additionalData);
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
                    break;
                case "Automation":
                    // todo need to handle a scene being updated from another tablet
                    if (additionalData.startsWith("lighting")) {
                        updateActiveAppliances(additionalData);
                    }
                    if (additionalData.startsWith("Set")) {
                        String[] values = additionalData.split(":");
                        MainActivity.runOnUI(() -> {
                            ZonesFragment.mViewModel.setActiveScene(values[2], values[3], values[4], true);
                        });
                    }
                    break;
                case "Scanner":
                    updateNUCAddress(additionalData);
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
                StationsFragment.mViewModel.setStations(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private static void updateStation(String stationId, String attribute, String value) throws JSONException {
        MainActivity.runOnUI(() -> {
            Station station = StationsFragment.mViewModel.getStationById(Integer.parseInt(stationId));
            switch (attribute) {
                case "status":
                    station.status = value;
                    break;
                case "volume":
                    station.volume = Integer.parseInt(value);
                    break;
                case "gameName":
                    station.gameName = value;
                    break;
                case "steamApplications":
                    station.setSteamApplicationsFromJsonString(value);
            }
            StationsFragment.mViewModel.updateStationById(Integer.parseInt(stationId), station);
        });
    }

    private static void updateZones(String jsonString) throws JSONException {
        JSONArray json = new JSONArray(jsonString);

        MainActivity.runOnUI(() -> {
            try {
                ZonesFragment.mViewModel.setZones(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    //Need cleaning up when there is access to CBUS
    private static void updateSelectedScene(String response) throws JSONException {
        String[] values = response.split(":");
        MainActivity.runOnUI(() -> {
            ZonesFragment.mViewModel.setActiveScene(values[1], values[2], values[3], true);
        });
    }

    private static void updateAppliances(String jsonString) throws JSONException {
        JSONArray json = new JSONArray(jsonString);

        MainActivity.runOnUI(() -> {
            try {
                LightFragment.mViewModel.setAppliances(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private static void updateActiveAppliances(String jsonString) throws JSONException {
        String requiredString = jsonString.substring(jsonString.indexOf("["), jsonString.indexOf("]") + 1);
        Log.e("ALL", requiredString);
        JSONArray json = new JSONArray(requiredString);

        MainActivity.runOnUI(() -> {
            try {
                LightFragment.mViewModel.setActiveAppliances(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Update the NUC address based on the results from the scanner.
     * @param response A string representing the IP address of the NUC.
     */
    private static void updateNUCAddress(String response) {
        MainActivity.runOnUI(() -> {
            NucFragment.mViewModel.setNucAddress(response);
        });
    }
}
