package com.lumination.leadmelabs.utilities;

import com.lumination.leadmelabs.MainActivity;
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

    /**
     * Convert a density pixel value to regular pixels based on the tablets screen density.
     * Dynamically setting dimensions require pixel units instead of density pixels.
     * @param dp A int representing the density pixels to be converted.
     * @return A int representing the relative pixel size for an individual device.
     */
    public static int convertDpToPx(int dp){
        float scale = MainActivity.getInstance().getResources().getDisplayMetrics().density;
        return (int) ((dp * scale) + 0.5f);
    }
}
