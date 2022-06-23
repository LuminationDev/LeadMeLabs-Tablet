package com.lumination.leadmelabs.utilities;

import com.lumination.leadmelabs.models.Station;

import java.util.ArrayList;
import java.util.List;
public class Helpers {
    public static ArrayList<Station> cloneStationList(List<Station> stationList) {
        ArrayList<Station> clone = new ArrayList<Station>(stationList.size());
        for (Station station:stationList) {
            clone.add(station.clone());
        }
        return clone;
    }
}
