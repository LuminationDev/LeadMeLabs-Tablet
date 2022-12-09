package com.lumination.leadmelabs.ui.stations;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class StationsViewModel extends ViewModel {
    private MutableLiveData<List<Station>> stations;
    private MutableLiveData<Station> selectedStation;
    private MutableLiveData<Integer> selectedSteamApplicationId = new MutableLiveData<>();

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

    public int getSelectedSteamApplicationId() {
        return selectedSteamApplicationId.getValue();
    }

    public String getSelectedSteamApplicationName(int steamApplicationId) {
        List<SteamApplication> steamApps = getAllSteamApplications();
        steamApps.removeIf(steamApplication -> steamApplication.id != steamApplicationId);
        return steamApps.get(0).name;
    }

    public void selectSelectedSteamApplication(int id) {
        selectedSteamApplicationId.setValue(id);
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

    public List<SteamApplication> getAllSteamApplications () {
        HashSet<SteamApplication> hashSet = new HashSet<>();
        if(stations.getValue() == null) {
            return new ArrayList<>();
        }
        for (Station station: stations.getValue()) {
            //Check if station does not have any loaded games?
            hashSet.addAll(station.steamApplications);
        }
        ArrayList<SteamApplication> list = new ArrayList<>(hashSet);
        list.sort((steamApplication, steamApplication2) -> steamApplication.name.compareToIgnoreCase(steamApplication2.name));
        return list;
    }

    public List<SteamApplication> getStationSteamApplications (int stationId) {
        ArrayList<SteamApplication> list = new ArrayList<>();
        if(stations.getValue() == null) {
            return list;
        }
        for (Station station: stations.getValue()) {
            if (station.id == stationId) {
                list = new ArrayList<>(station.steamApplications);
                list.sort((steamApplication, steamApplication2) -> steamApplication.name.compareToIgnoreCase(steamApplication2.name));
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
        if(SettingsFragment.mViewModel.getHideStationControls().getValue()) {
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
                    stationJson.getString("steamApplications"),
                    stationJson.getInt("id"),
                    stationJson.getString("status"),
                    stationJson.getInt("volume"),
                    stationJson.getInt("theatreId"),
                    stationJson.getString("room"),
                    stationJson.getString("ledRingId"),
                    stationJson.getString("macAddress"));
            if (!stationJson.getString("gameName").equals("")) {
                station.gameName = stationJson.getString("gameName");
            }
            if (stationJson.getString("gameId") != "null") {
                station.gameId = stationJson.getString("gameId");
            }
            st.add(station);
        }
        this.setStations(st);
    }

    public void setActiveTheatres(int[] theatreIds, int[] zoneTheatreIds) {
        ArrayList<Station> stationArrayList = (ArrayList<Station>) getStations().getValue();
        stationArrayList = (ArrayList<Station>) stationArrayList.clone();
        for (Station s:stationArrayList) {
            boolean containsTheatre = false;
            for (int id:theatreIds) {
                if (s.theatreId == id) {
                    containsTheatre = true;
                    s.theatreText = "Theatre " + s.theatreId;
                }
            }
            if (!containsTheatre && s.theatreText != null) {
                for (int id:zoneTheatreIds) {
                    if (s.theatreId == id) {
                        s.theatreText = null;
                    }
                }
            }
        }
        setStations(stationArrayList);
    }

    public void setStations(List<Station> stations) {
        this.stations.setValue(stations);
    }

    public void loadStations() {
        NetworkService.sendMessage("NUC", "Stations", "List");
    }
}