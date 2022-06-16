package com.lumination.leadmelabs.utilities;

import android.widget.Toast;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class used to identify different areas of the lab. This can be built out to identify different
 * stations, rooms, lighting areas, etc..
 */
public class Identifier {
    private static ScheduledExecutorService scheduledExecutor;
    private static int count = 0;
    private static final int stagger = 1500;
    //Detect if the function is already running, do not want to double up.
    private static boolean identifying = false;
    public static int initialDelay = 2500;

    /**
     * Cycle through the currently viewable stations, triggering the identify stations overlay and the
     * associated LED rings.
     */
    public static void identifyStations(List<Station> stations) {
        scheduledExecutor = new ScheduledThreadPoolExecutor(1);
        count = 0;

        if(identifying) {
            return;
        } else {
            identifying = true;
        }

        if(stations == null || stations.size() == 0) {
            Toast.makeText(MainActivity.getInstance(), "No stations located", Toast.LENGTH_SHORT).show();
            identifying = false;
            return;
        }

        scheduledExecutor.scheduleAtFixedRate(task(stations), initialDelay, stagger, TimeUnit.MILLISECONDS);
    }

    /**
     * A standard runnable to hold the current communication. Using a runnable allows the
     * processingPool to execute one at a time.
     * @param stations A list of Station objects to iterate through.
     * @return A runnable object relating to the passed in Station.
     */
    private static Runnable task(List<Station> stations) {
        return () -> {
            //Trigger the overlay
            NetworkService.sendMessage("Station," + stations.get(count).id, "CommandLine", "IdentifyStation");
            //Trigger the LED ring to flash - run the script on the CBUS with the provided context (the associated LED ring id - CBUS group id not actual id)
            NetworkService.sendMessage("NUC", "Automation", "Script:0:127:1:" + stations.get(count).associated.automationId);

            count++;

            //If this is the last station reset
            if (count == stations.size()) {
                identifying = false;
                scheduledExecutor.shutdown();
                MainActivity.runOnUI(() ->
                    Toast.makeText(MainActivity.getInstance().getApplicationContext(), "Stations located successfully", Toast.LENGTH_SHORT).show()
                );
            }
        };
    }
}
