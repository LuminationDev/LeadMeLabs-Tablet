package com.lumination.leadmelabs.utilities;

import android.os.CountDownTimer;
import android.widget.Toast;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.HashMap;
import java.util.List;
/**
 * Class used to identify different areas of the lab. This can be built out to identify different
 * stations, rooms, lighting areas, etc..
 */
public class Identifier {
    //Detect if the function is already running, do not want to double up.
    private static boolean identifying = false;
    public static int initialDelay = 2500;

    /**
     * Cycle through the currently viewable stations, triggering the identify stations overlay and the
     * associated LED rings.
     */
    public static void identifyStations(List<Station> stations) {
        final int[] index = {stations.size() - 1};
        if (identifying) {
            return;
        }
        identifying = true;
        CountDownTimer timer = new CountDownTimer(4000L * (stations.size()), 4000) {
            @Override
            public void onTick(long l) {
                int listIndex = (stations.size() - 1) - index[0];
                if (listIndex > stations.size() - 1 || listIndex < 0) {
                    return;
                }
                Station station = stations.get((stations.size() - 1) - index[0]);
                NetworkService.sendMessage("NUC", "IdentifyStation", station.id + "");
                Toast.makeText(MainActivity.getInstance().getApplicationContext(), "Successfully located " + station.name, Toast.LENGTH_SHORT).show();
                index[0]--;

                HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                    put("station_id", String.valueOf(station.id));
                }};
                FirebaseManager.logAnalyticEvent("identify_station", analyticsAttributes);
            }

            @Override
            public void onFinish() {
                identifying = false;
            }
        }.start();
    }
}
