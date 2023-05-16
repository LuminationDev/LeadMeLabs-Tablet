package com.lumination.leadmelabs.ui.stations;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.applications.details.Actions;
import com.lumination.leadmelabs.models.applications.details.Details;
import com.lumination.leadmelabs.models.applications.details.Levels;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StationsViewModel extends ViewModel {
    private MutableLiveData<List<Station>> stations;
    private MutableLiveData<Station> selectedStation;
    private MutableLiveData<String> selectedApplicationId = new MutableLiveData<>();

    public LiveData<List<Station>> getStations() {
        if (stations == null) {
            stations = new MutableLiveData<>();
            loadStations();
        }
        return stations;
    }

    public List<String> getStationNames(int[] stationIds) {
        ArrayList<Station> stations = (ArrayList<Station>) this.getStations().getValue();
        stations = (ArrayList<Station>) stations.clone();
        List<Integer> stationIdsList =  new ArrayList<Integer>(stationIds.length);
        for (int i : stationIds)
        {
            stationIdsList.add(i);
        }
        stations.removeIf(station -> !stationIdsList.contains(station.id));
        return stations.stream().map(station -> station.name).collect(Collectors.toList());
    }

    public String getSelectedApplicationId() {
        return selectedApplicationId.getValue();
    }

    public String getSelectedApplicationName(String applicationId) {
        List<Application> allApps = getAllApplications();
        allApps.removeIf(application -> !Objects.equals(application.id, applicationId));
        if(allApps.size() == 0) return "experience";
        return allApps.get(0).name;
    }

    public void selectSelectedApplication(String id) {
        selectedApplicationId.setValue(id);
    }

    public int[] getSelectedStationIds() {
        ArrayList<Station> selectedStations = getSelectedStations();
        int[] selectedIds = new int[selectedStations.size()];
        for (int i = 0; i < selectedStations.size(); i++) {
            selectedIds[i] = selectedStations.get(i).id;
        }
        return selectedIds;
    }

    public int[] getAllStationIds() {
        ArrayList<Station> stations = (ArrayList<Station>) getStations().getValue();
        if (stations == null) {
            return new int[0];
        }
        int[] ids = new int[stations.size()];
        for (int i = 0; i < stations.size(); i++) {
            ids[i] = stations.get(i).id;
        }
        return ids;
    }

    public ArrayList<Station> getSelectedStations() {
        ArrayList<Station> selectedStations = (ArrayList<Station>) stations.getValue();
        selectedStations = (ArrayList<Station>) selectedStations.clone();
        selectedStations.removeIf(station -> !station.selected);
        return selectedStations;
    }

    public Station getStationById(int id) {
        ArrayList<Station> stationsData = (ArrayList<Station>) stations.getValue();
        if (stationsData == null) {
            return null;
        }
        for (Station station:stationsData) {
            if (station.id == id) {
                return station;
            }
        }
        return null;
    }

    /**
     * Set the details of an application on a specified experience. The details can include, global
     * actions and different levels. As there is no full list of experiences it needs to set the
     * details for each instance of that experience. I.e one for each station it is installed on.
     * @param applicationDetails A string representing the name of the application to update.
     */
    public void setApplicationDetails(JSONObject applicationDetails) throws JSONException {
        if(stations.getValue() == null) {
            return;
        }

        //Deconstruct the JSON object into the new details class and associated subclasses.
        Details details = parseDetails(applicationDetails);

        for (Station station: stations.getValue()) {
            //Check each station for the experience
            for (Application application: station.applications) {
                if (Objects.equals(application.name, details.name)) {
                    application.details = details;
                }
            }
        }
    }

    /**
     * Turn the JSON object that is supplied into the Details class with its associated Global actions,
     * levels and level specific actions.
     * @param JSONvalue A JSON object that has been sent from a Station.
     * @return A Details class instantiation
     */
    private Details parseDetails(JSONObject JSONvalue) throws JSONException {
        Details details = new Details(JSONvalue.getString("name"));

        JSONArray globalActions = JSONvalue.getJSONArray("globalActions");
        for (int i = 0; i < globalActions.length(); i++) {
            JSONObject temp = globalActions.getJSONObject(i);
            details.addGlobalAction(new Actions(temp.getString("name"), temp.getString("trigger")));
        }

        JSONArray levels = JSONvalue.getJSONArray("levels");
        for (int i = 0; i < levels.length(); i++) {
            JSONObject temp = levels.getJSONObject(i);
            Levels level = new Levels(temp.getString("name"), temp.getString("trigger"));

            //Add the level trigger as it's first action
            level.addAction(new Actions("Set", level.trigger));

            //Add the rest of the actions
            JSONArray actions = temp.getJSONArray("actions");
            for (int y = 0; y < actions.length(); y++) {
                JSONObject action = actions.getJSONObject(y);
                level.addAction(new Actions(action.getString("name"), action.getString("trigger")));
            }

            details.addLevels(level);
        }

        return details;
    }

    public List<Application> getAllApplications() {
        HashSet<String> idSet = new HashSet<>();

        if(stations.getValue() == null) {
            return new ArrayList<>();
        }

        //Only add applications with a unique id
        return stations.getValue().stream()
                .flatMap(station -> station.applications.stream())
                .filter(app -> idSet.add(app.id))
                .distinct()
                .sorted((application, application2) -> application.name.compareToIgnoreCase(application2.name))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Application> getStationApplications(int stationId) {
        ArrayList<Application> list = new ArrayList<>();
        if(stations.getValue() == null) {
            return list;
        }
        for (Station station: stations.getValue()) {
            if (station.id == stationId) {
                if(SettingsFragment.checkLockedRooms(station.room)) {
                    list = new ArrayList<>(station.applications);
                    list.sort((application, application2) -> application.name.compareToIgnoreCase(application2.name));
                }
            }
        }
        return list;
    }

    public void updateStationById(int id, Station station) {
        List<Station> stationsData = stations.getValue();
        int index = -1;
        for(int i = 0; i < stationsData.size(); i++) {
            if (stationsData.get(i).id == id) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            stationsData.set(index, station);
            stations.setValue((stationsData));
        }
        if (getSelectedStation().getValue() != null && id == getSelectedStation().getValue().id) {
            setSelectedStation(station);
        }
    }

    /**
     * A message has been sent from the NUC determine which station has values that are changing.
     * The values for the computer will be in CBUS format i.e. numbers. Therefore run a switch case
     * to determine what the status should be.
     */
    public void syncStationStatus(String id, String value, String ipAddress) {
        Station station = StationsFragment.mViewModel.getStationById(Integer.parseInt(id));

        //Exit the function if the tablet is in wall mode.
        if(Boolean.TRUE.equals(SettingsFragment.mViewModel.getHideStationControls().getValue())) {
            return;
        }

        //Disregard the message if from the same IP address
        if(NetworkService.getIPAddress().equals(ipAddress)) {
            return;
        }

        String status = null;
        switch (value) {
            case "0":
                break;
            case "1":
                break;
            case "2":
                status = "Turning On";
                break;
        }

        //Nothing to update or the station is already on.
        if(station.status.equals(status) || station.status.equals("On")) {
            return;
        }

        String finalStatus = status;
        MainActivity.runOnUI(() -> {
            station.cancelStatusCheck();
            station.powerStatusCheck();
            station.status = finalStatus;
            StationsFragment.mViewModel.updateStationById(Integer.parseInt(id), station);
        });
    }

    public LiveData<Station> selectStation(int id) {
        this.getSelectedStation();
        this.setSelectedStation(this.stations.getValue().stream().filter(station -> station.id == id).findFirst().get());
        return this.getSelectedStation();
    }

    public void setSelectedStation(Station station) {
        this.selectedStation.setValue(station);
    }

    public LiveData<Station> getSelectedStation() {
        if (selectedStation == null) {
            selectedStation = new MutableLiveData<>();
        }
        return selectedStation;
    }

    public void setStations(JSONArray stations) throws JSONException {
        List<Station> st = new ArrayList<>();
        for (int i = 0; i < stations.length(); i++) {
            JSONObject stationJson = stations.getJSONObject(i);
            Station station = new Station(
                    stationJson.getString("name"),
                    stationJson.getString("installedApplications"),
                    stationJson.getInt("id"),
                    stationJson.getString("status"),
                    stationJson.getInt("volume"),
                    stationJson.getString("room"),
                    stationJson.getString("ledRingId"),
                    stationJson.getString("macAddress"));
            if (!stationJson.getString("gameName").equals("")) {
                station.gameName = stationJson.getString("gameName");
            }
            if (!stationJson.getString("gameId").equals("null")) {
                station.gameId = stationJson.getString("gameId");
            }
            if (!stationJson.getString("gameType").equals("null")) {
                station.gameType = stationJson.getString("gameType");
            }
            st.add(station);
        }
        this.setStations(st);
    }

    public void setStations(List<Station> stations) {
        this.stations.setValue(stations);
    }

    public void loadStations() {
        NetworkService.sendMessage("NUC", "Stations", "List");
    }
}