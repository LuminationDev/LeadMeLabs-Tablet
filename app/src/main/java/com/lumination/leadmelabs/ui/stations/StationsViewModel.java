package com.lumination.leadmelabs.ui.stations;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;
import com.lumination.leadmelabs.services.NetworkService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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

    public int getSelectedSteamApplicationId() {
        return selectedSteamApplicationId.getValue();
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

    public ArrayList<Station> getSelectedStations() {
        ArrayList<Station> selectedStations = (ArrayList<Station>) stations.getValue();
        selectedStations = (ArrayList<Station>) selectedStations.clone();
        selectedStations.removeIf(station -> !station.selected);
        return selectedStations;
    }

    public Station getStationById(int id) {
        ArrayList<Station> stationsData = (ArrayList<Station>) stations.getValue();
        for (Station station:stationsData) {
            if (station.id == id) {
                return station;
            }
        }
        return null;
    }

    public List<SteamApplication> getAllSteamApplications () {
        HashSet<SteamApplication> hashSet = new HashSet<>();
        for (Station station: stations.getValue()) {
            hashSet.addAll(station.steamApplications);
        }
        return new ArrayList<>(hashSet);
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
        if (id == getSelectedStation().getValue().id) {
            setSelectedStation(station);
        }
    }

    public LiveData<Station> selectStation(int index) {
        this.getSelectedStation();
        this.setSelectedStation(this.stations.getValue().get(index));
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
                    stationJson.getInt("theatreId"));
            if (stationJson.getString("gameName") != "null") {
                station.gameName = stationJson.getString("gameName");
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

    private void loadStations() {
        NetworkService.sendMessage("NUC", "Stations", "List");
    }
}