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
                    stationJson.getString("steamapps"),
                    stationJson.getInt("id"));
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