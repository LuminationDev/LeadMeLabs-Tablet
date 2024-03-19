package com.lumination.leadmelabs.ui.stations;

import android.util.Log;

import com.lumination.leadmelabs.models.stations.ContentStation;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.VrStation;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

        //BACKWARDS COMPATIBILITY - check for the JsonArray or the back string of applications
        String installedApplications = stationJson.optString("installedJsonApplications", "");
        Object applications;
        try {
            if (!installedApplications.isEmpty()) {
                applications = new JSONArray(installedApplications);
            } else {
                applications = stationJson.optString("installedApplications", "");
            }
        } catch (JSONException e) {
            applications = stationJson.optString("installedApplications", "");
        }

        String mode = stationJson.optString("mode", "vr").toLowerCase();
        switch (mode) {
            case "content":
                return createContentStation(stationJson, applications, state);
            case "vr":
                return createVrStation(stationJson, applications, state);
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
     * @param installedApplications An object, either a JSONArray or a string containing the details
     *                              of install application details.
     * @param state A string describing the current state of a station.
     * @return A ContentStation object initialized with the provided data.
     * @throws JSONException If there is an issue parsing the JSON data.
     */
    private static ContentStation createContentStation(JSONObject stationJson, Object installedApplications, String state) throws JSONException {
        ContentStation station = new ContentStation(
                stationJson.getString("name"),
                installedApplications,
                stationJson.getInt("id"),
                stationJson.getString("status"),
                state,
                stationJson.getString("room"),
                stationJson.getString("macAddress"));

        setNestedStations(station, stationJson);
        setExperienceDetails(station, stationJson);
        setAudioDetails(station, stationJson);
        setVideoDetails(station, stationJson);

        return station;
    }

    /**
     * Creates a VirtualStation object based on the provided JSON data.
     *
     * @param stationJson JSON object containing vr station details.
     * @param installedApplications An object, either a JSONArray or a string containing the details
     *                              of install application details.
     * @param state A string describing the current state of a station.
     * @return A VirtualStation object initialized with the provided data.
     * @throws JSONException If there is an issue parsing the JSON data.
     */
    private static VrStation createVrStation(JSONObject stationJson, Object installedApplications, String state) throws JSONException {
        VrStation station = new VrStation(
                stationJson.getString("name"),
                installedApplications,
                stationJson.getInt("id"),
                stationJson.getString("status"),
                state,
                stationJson.getString("room"),
                stationJson.getString("macAddress"),
                stationJson.getString("ledRingId"));

        setNestedStations(station, stationJson);
        setExperienceDetails(station, stationJson);
        setAudioDetails(station, stationJson);
        setVideoDetails(station, stationJson);

        return station;
    }

    /**
     * Set any nested stations.
     *
     * @param station     The BaseStation object to update with game details.
     * @param stationJson JSON object containing game-related details.
     * @throws JSONException If there is an issue parsing the JSON data.
     */
    private static void setNestedStations(Station station, JSONObject stationJson) throws JSONException {
        if (!stationJson.has("nestedStations")) {
            return;
        }

        JSONArray nestedStations = stationJson.getJSONArray("nestedStations");
        ArrayList<Integer> nestedStationsList = new ArrayList<>();

        for (int i = 0; i < nestedStations.length(); i++) {
            nestedStationsList.add(nestedStations.getInt(i));
        }
        if (nestedStationsList.isEmpty()) {
            return;
        }

        station.nestedStations = nestedStationsList;
    }

    /**
     * Sets game-related details on the given BaseStation object using the provided JSON data.
     *
     * @param station     The BaseStation object to update with game details.
     * @param stationJson JSON object containing game-related details.
     * @throws JSONException If there is an issue parsing the JSON data.
     */
    private static void setExperienceDetails(Station station, JSONObject stationJson) throws JSONException {
        if (!stationJson.getString("gameName").equals("")) {
            station.applicationController.setGameName(stationJson.getString("gameName"));
        }
        if (!stationJson.getString("gameId").equals("null")) {
            station.applicationController.setGameId(stationJson.getString("gameId"));
        }
        if (!stationJson.getString("gameType").equals("null")) {
            station.applicationController.setGameType(stationJson.getString("gameType"));
        }
    }

    /**
     * Sets the audio related details that have been saved on the NUC for the current session.
     *
     * @param station     The BaseStation object to update with game details.
     * @param stationJson JSON object containing audio-related details.
     */
    private static void setAudioDetails(Station station, JSONObject stationJson) {
        String audio = stationJson.optString("audioDevices", "");
        if (!audio.equals("")) {
            station.audioController.setAudioDevices(audio);
        }

        String activeAudio = stationJson.optString("ActiveAudioDevice", "");
        if (!activeAudio.equals("")) {
            station.audioController.setActiveAudioDevice(activeAudio);
        }
    }

    /**
     * Sets the video related details that have been saved on the NUC for the current session.
     *
     * @param station     The BaseStation object to update with game details.
     * @param stationJson JSON object containing video-related details.
     */
    private static void setVideoDetails(Station station, JSONObject stationJson) {
        String videos = stationJson.optString("videoFiles", "");
        if (!videos.equals("")) {
            station.videoController.setVideos(videos);
        }

        String activeVideo = stationJson.optString("CurrentVideo", "");
        if (!activeVideo.equals("")) {
            station.videoController.setActiveVideo(activeVideo);
        }

        Log.e("JSON", stationJson.toString());
    }
}
