package com.lumination.leadmelabs.ui.dashboard;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.PeriodicChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A class dedicated to managing the tracking of the mode that is being set.
 */
public class DashboardModeManagement {
    private static Timer modeTimer;
    private static TimerTask modeTask;
    private static Timer modeBackupTimer;
    private static TimerTask modeBackupTask;
    private static PeriodicChecker periodicChecker;

    /**
     * A mode has been triggered, disable the other dashboard buttons while the mode is being set.
     * @param active A string of the mode that is being set.
     */
    public void changeModeButtonAvailability(String sceneName, String active) {
        DashboardFragment.mViewModel.setActivatingMode(active);
        DashboardFragment.mViewModel.setChangingMode(true);

        long backupDelay;
        switch (active) {
            //Mode buttons disabled until all computers turn on - backup reset is 3 minutes (same as power status check)
            case Constants.VR_MODE:
            case Constants.PRESENTATION_MODE:
                backupDelay = 3 * 60 * 1000; //3 minutes
                waitForStations("Scene", sceneName, "On");
                break;

            //Mode buttons disabled until all computers turn on, initial delay of 30 seconds as the Stations turn off - backup reset is 4 minutes
            case Constants.RESTART_MODE:
                backupDelay = 4 * 60 * 1000; //4 minutes

                //Wait 30 seconds before starting the check, ensuring the Stations are already 'Off'
                MainActivity.UIHandler.postDelayed(() -> waitForStations("Room", "","On"), 30000);
                break;

            //Mode buttons disabled for 1 minute after all the computer's statuses go to off - backup reset is 2 minutes
            case Constants.SHUTDOWN_MODE:
                backupDelay = 2 * 60 * 1000; //2 minutes
                waitForStations("Room", "", "Off");
                break;

            //Mode buttons disabled for 1 minute after all the computer's statuses go to off - backup reset is 3 minutes
            case Constants.CLASSROOM_MODE:
                backupDelay = 3 * 60 * 1000; //3 minutes
                waitForStations("Scene", sceneName,"Off");
                break;

            default:
                return;
        }

        //Create a backup timer that cancels all tasks in case something has gone wrong
        createModeResetTimer(backupDelay, true);
    }

    /**
     * Wait for all Stations within the specified context to reach their target status.
     * @param context The context within which stations are to be collected (Room or Scene).
     * @param sceneName A string of the scene that is being triggered as to collect associated
     *                  stations (left empty for room collection).
     * @param targetStatus A string of the status the Station should end on (On or Off).
     */
    private void waitForStations(String context, String sceneName, String targetStatus) {
        periodicChecker = new PeriodicChecker();
        AtomicBoolean isFirst = new AtomicBoolean(true);

        // Stops the periodic check if the callback is true
        ArrayList<?> stations = collectStations(context, sceneName, targetStatus);

        periodicChecker.setCallback(() -> {
            //If this passed on the first instance then the scene is already set, only have a few seconds cool down in this instance
            if (checkStationStatuses(stations, targetStatus)) {
                if (isFirst.get()) {
                    createModeResetTimer(5 * 1000, false);
                }
                else if (targetStatus.equals("Off")) {
                    //Wait an extra 45 seconds to make sure the Stations are off
                    createModeResetTimer(45 * 1000, false);
                }
                else {
                    //The mode is set, cancel the timers
                    cancelModeTimers();
                }
                return true;
            }

            isFirst.set(false);
            return false;
        });

        // Start the periodic check
        periodicChecker.start();
    }

    /**
     * Collect all the Stations based on the given context.
     * @param context The context within which stations are to be collected (Room or Scene).
     * @param sceneName (Optional) The name of the scene to query, applicable only if context is "Scene".
     * @param targetStatus A string of the status the Station should end on (On or Off).
     * @return A list of Station objects or JSONObjects based on the context.
     */
    private ArrayList<?> collectStations(String context, String sceneName, String targetStatus) {
        switch (context) {
            case "Room":
                ArrayList<Object> stations = new ArrayList<>();
                List<Station> roomStations = StationsFragment.mViewModel.getStations().getValue();
                if (roomStations != null) {
                    String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();
                    if (roomType == null) {
                        roomType = "All";
                    }

                    for (Station station : roomStations) {
                        // Do not show hidden stations
                        if (station.getIsHidden()) {
                            continue;
                        }

                        if (roomType.equals("All")) {
                            if (SettingsFragment.checkLockedRooms(station.room)) {
                                stations.add(station);
                            }
                        } else if (station.room.equals(roomType)) {
                            stations.add(station);
                        }
                    }
                }
                return stations;

            case "Scene":
                ArrayList<JSONObject> stationObjects = new ArrayList<>();
                ApplianceViewModel applianceViewModel = ApplianceFragment.mViewModel;
                List<Appliance> appliances = applianceViewModel.getAppliances().getValue();

                if (appliances != null) {
                    List<Appliance> matchingAppliances = appliances.stream()
                            .filter(appliance -> appliance.name.toLowerCase().contains(sceneName)
                                    && "scenes".equals(appliance.type)
                                    && SettingsFragment.checkLockedRooms(appliance.room))
                            .collect(Collectors.toList());

                    if (matchingAppliances.isEmpty()) {
                        matchingAppliances = DashboardFragment.getInstance().searchForBackupSceneTrigger(appliances, sceneName);
                    }

                    if (!matchingAppliances.isEmpty()) {
                        stationObjects = DashboardFragment.getInstance().collectStationsWithActions(matchingAppliances, targetStatus);
                    }
                }
                return stationObjects;

            default:
                // Handle unsupported context
                return new ArrayList<>();
        }
    }

    /**
     * Check if all the Stations in the supplied list have a status matching the supplied target
     * status.
     * @param stationArray A list of Stations or JSONObject objects to check the statuses of.
     * @param targetStatus A string of the status the Station should end on (On or Off).
     * @return A boolean, true if all Stations have the target status, otherwise false.
     */
    private boolean checkStationStatuses(ArrayList<?> stationArray, String targetStatus) {
        //Track the stations statuses for each loop and see if they all match the target status
        boolean allCorrect = true;

        //Bail early if there are not Stations to check
        if (stationArray.isEmpty()) return true;

        //This is the loop to check
        for (Object stationObject : stationArray) {
            Station station;
            if (stationObject instanceof Station) {
                station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(((Station) stationObject).getId());
            } else if (stationObject instanceof JSONObject) {
                try {
                    int id = Integer.parseInt(((JSONObject) stationObject).getString("id"));
                    station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);
                } catch (JSONException e) {
                    continue; // Skip this station and proceed with the next one
                }
            } else {
                // Handle unsupported station type
                continue; // Skip this station and proceed with the next one
            }

            if (!station.statusHandler.getStatus().equals(targetStatus)) {
                allCorrect = false;
            }
        }

        return allCorrect;
    }

    /**
     * Create a timer for mode reset after a specific check has returned true.
     * I.e. Classroom mode has been triggered, all Stations have now gone to 'Off', start a timer so
     * wake on lan cannot be triggered too early.
     * @param delayInMillis A long representing the delay before triggering the mode reset.
     * @param isBackupTimer Boolean indicating whether this is a backup timer or not.
     */
    private static void createModeResetTimer(long delayInMillis, boolean isBackupTimer) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                cancelModeTimers();
            }
        };

        if (isBackupTimer) {
            modeBackupTimer = timer;
            modeBackupTask = task;
        } else {
            modeTimer = timer;
            modeTask = task;
        }

        timer.schedule(task, delayInMillis);
    }

    /**
     * Cancel the modeTimer, modeBackupTimer and periodicChecker before resetting the mode in
     * the viewModel.
     */
    public static void cancelModeTimers() {
        if (modeTimer != null) {
            modeTask.cancel();
            modeTimer.cancel();
            modeTimer = null;
        }

        if (modeBackupTimer != null) {
            modeBackupTask.cancel();
            modeBackupTimer.cancel();
            modeBackupTimer = null;
        }

        if (periodicChecker != null) {
            periodicChecker.stop();
            periodicChecker = null;
        }

        MainActivity.runOnUI(() -> DashboardFragment.mViewModel.resetMode());
    }
}
