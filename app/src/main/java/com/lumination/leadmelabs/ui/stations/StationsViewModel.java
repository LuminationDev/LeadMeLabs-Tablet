package com.lumination.leadmelabs.ui.stations;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StationsViewModel extends ViewModel {
    private MutableLiveData<List<Station>> stations;
    private MutableLiveData<Station> selectedStation;

    public LiveData<List<Station>> getStations() {
        if (stations == null) {
            stations = new MutableLiveData<>();
            loadStations();
        }
        return stations;
    }

    public Station getStationById(int id) {
        List<Station> stationsData = stations.getValue();
        stationsData.removeIf(station -> station.id != id);
        return stationsData.get(0);
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

    }

    public LiveData<Station> selectStation(int index) {
        this.selectedStation.setValue(this.stations.getValue().get(index));
        return this.getSelectedStation();
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
                    stationJson.getString("status"));
            st.add(station);
        }
        this.setStations(st);
    }

    public void setStations(List<Station> stations) {
        this.stations.setValue(stations);
    }

    private void loadStations() {
        NetworkService.sendMessage("NUC", "Stations", "List");
    }
}