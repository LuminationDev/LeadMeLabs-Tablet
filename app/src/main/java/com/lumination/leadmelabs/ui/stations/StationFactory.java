package com.lumination.leadmelabs.ui.stations;

import com.lumination.leadmelabs.models.stations.ContentStation;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.VirtualStation;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

import org.json.JSONException;
import org.json.JSONObject;

import io.sentry.Sentry;

/**
 * Factory class responsible for creating instances of different Station types
 * based on the provided JSON data and mode information. It includes methods for
 * creating ContentStation and VirtualStation objects, as well as setting game-related
 * details on a BaseStation object. The class ensures flexibility and extensibility
 * in handling diverse station types and their associated data.
 */
public class StationFactory {
    /**
     * Creates a Station object based on the provided JSON data, considering the specified mode.
     *
     * @param stationJson JSON object containing station details.
     * @return A Station object of the appropriate type based on the mode.
     * @throws JSONException If there is an issue parsing the JSON data.
     * @throws IllegalArgumentException If the specified mode is unsupported.
     */
    public static Station createStation(JSONObject stationJson) throws JSONException {
        //Backwards compatibility - if no status is sent set as Ready to go.
        String state;
        try {
            state = stationJson.getString("state");
        } catch (Exception e) {
            state = "Not set";
        }

        String mode = stationJson.optString("mode", "vr").toLowerCase();
        switch (mode) {
            case "content":
                return createContentStation(stationJson, state);
            case "vr":
                return createVirtualStation(stationJson, state);
            // Add more cases for other modes if needed
            default:
                String location = SettingsFragment.mViewModel.getLabLocation().getValue();
                String stationId = stationJson.optString("name", "Unknown");
                String message = location + " " + stationId + ": Unsupported Station mode: " + mode;
                Sentry.captureMessage(message);
                break;
        }

        return null;
    }

    /**
     * Creates a ContentStation object based on the provided JSON data.
     *
     * @param stationJson JSON object containing content station details.
     * @return A ContentStation object initialized with the provided data.
     * @throws JSONException If there is an issue parsing the JSON data.
     */
    private static ContentStation createContentStation(JSONObject stationJson, String state) throws JSONException {
        ContentStation station = new ContentStation(
                stationJson.getString("name"),
                stationJson.getString("installedApplications"),
                stationJson.getInt("id"),
                stationJson.getString("status"),
                state,
                stationJson.getInt("volume"),
                stationJson.getString("room"),
                stationJson.getString("macAddress"));

        setGameDetails(station, stationJson);

        return station;
    }

    /**
     * Creates a VirtualStation object based on the provided JSON data.
     *
     * @param stationJson JSON object containing virtual station details.
     * @return A VirtualStation object initialized with the provided data.
     * @throws JSONException If there is an issue parsing the JSON data.
     */
    private static VirtualStation createVirtualStation(JSONObject stationJson, String state) throws JSONException {
        VirtualStation station = new VirtualStation(
                stationJson.getString("name"),
                stationJson.getString("installedApplications"),
                stationJson.getInt("id"),
                stationJson.getString("status"),
                state,
                stationJson.getInt("volume"),
                stationJson.getString("room"),
                stationJson.getString("macAddress"),
                stationJson.getString("ledRingId"));

        setGameDetails(station, stationJson);

        return station;
    }

    /**
     * Sets game-related details on the given BaseStation object using the provided JSON data.
     *
     * @param station     The BaseStation object to update with game details.
     * @param stationJson JSON object containing game-related details.
     * @throws JSONException If there is an issue parsing the JSON data.
     */
    private static void setGameDetails(Station station, JSONObject stationJson) throws JSONException {
        if (!stationJson.getString("gameName").equals("")) {
            station.gameName = stationJson.getString("gameName");
        }
        if (!stationJson.getString("gameId").equals("null")) {
            station.gameId = stationJson.getString("gameId");
        }
        if (!stationJson.getString("gameType").equals("null")) {
            station.gameType = stationJson.getString("gameType");
        }
    }
}
