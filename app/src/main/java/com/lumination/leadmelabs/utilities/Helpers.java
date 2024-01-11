package com.lumination.leadmelabs.utilities;

import android.util.Log;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.stations.VirtualStation;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
public class Helpers {
    public static ArrayList<VirtualStation> cloneStationList(List<VirtualStation> stationList) {
        ArrayList<VirtualStation> clone = new ArrayList<VirtualStation>(stationList.size());
        for (VirtualStation station:stationList) {
            clone.add((VirtualStation) station.clone());
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

    public static boolean urlIsAvailable(String url){
        try {
            URL urlObject = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
            urlConnection.connect();
            urlConnection.disconnect();
            return true;
        } catch (IOException e) {
            Log.e("QaChecks", "Exception", e);
        }
        return false;
    }
}
