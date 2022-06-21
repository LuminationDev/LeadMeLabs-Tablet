package com.lumination.leadmelabs.utilities;

import android.os.CountDownTimer;
import android.widget.Toast;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.List;
/**
 * Class used to identify different areas of the lab. This can be built out to identify different
 * stations, rooms, lighting areas, etc..
 */
public class Identifier {
    //Detect if the function is already running, do not want to double up.
    private static boolean identifying = false;

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
        CountDownTimer timer = new CountDownTimer(2000L * (stations.size()), 2000) {
            @Override
            public void onTick(long l) {
                Station station = stations.get((stations.size() - 1) - index[0]);
                NetworkService.sendMessage("Station," + station.id, "CommandLine", "IdentifyStation");
                if(station.associated != null) {
                    NetworkService.sendMessage("NUC", "Automation", "Script:0:127:1:" + station.associated.automationId);
                }
                index[0]--;
            }

            @Override
            public void onFinish() {
                MainActivity.runOnUI(() ->
                        Toast.makeText(MainActivity.getInstance().getApplicationContext(), "Stations located successfully", Toast.LENGTH_SHORT).show()
                );
                identifying = false;
            }
        }.start();
    }
}
