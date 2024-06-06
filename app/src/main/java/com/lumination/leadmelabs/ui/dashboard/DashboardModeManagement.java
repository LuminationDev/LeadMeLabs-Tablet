package com.lumination.leadmelabs.ui.dashboard;

import android.util.Log;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.appliance.controllers.SceneController;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.PeriodicChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A class dedicated to managing the tracking of the mode that is being set.
 */
public class DashboardModeManagement {
    private static PeriodicChecker periodicChecker;

    private static final Map<String, Timer> modeTimers = new HashMap<>();
    private static final Map<String, TimerTask> modeTasks = new HashMap<>();
    private static final Map<String, Timer> modeBackupTimers = new HashMap<>();
    private static final Map<String, TimerTask> modeBackupTasks = new HashMap<>();

    /**
     * A mode has been triggered, disable the other dashboard buttons while the mode is being set.
     * @param active A string of the mode that is being set.
     */
    public void changeModeButtonAvailability(String sceneName, String room, String active) {
        //Check if scene timers is enable in settings (defaults to 'On')
        if (Boolean.FALSE.equals(SettingsFragment.mViewModel.getSceneTimer().getValue())) {
            return;
        }

        DashboardFragment.mViewModel.setActivatingMode(active);
        DashboardFragment.mViewModel.setChangingMode(true);

        long backupDelay;
        switch (active) {
            //Mode buttons disabled until all computers turn on - backup reset is 3 minutes (same as power status check)
            case Constants.VR_MODE:
            case Constants.SHOWCASE_MODE:
                backupDelay = 3 * 60 * 1000; //3 minutes
                waitForStations("Scene", sceneName, room, "On", active);
                break;

            //Mode buttons disabled until all computers turn on, initial delay of 30 seconds as the Stations turn off - backup reset is 4 minutes
            case Constants.RESTART_MODE:
                backupDelay = 4 * 60 * 1000; //4 minutes

                //Wait 30 seconds before starting the check, ensuring the Stations are already 'Off'
                MainActivity.UIHandler.postDelayed(() -> waitForStations("Room", "", "", "On", active), 30000);
                SceneController.handeActivatingScene(null, true);
                break;

            //Mode buttons disabled for 1 minute after all the computer's statuses go to off - backup reset is 2 minutes
            case Constants.SHUTDOWN_MODE:
                backupDelay = 2 * 60 * 1000; //2 minutes
                waitForStations("Room", "", room,"Off", active);
                SceneController.handeActivatingScene(null, true);
                break;

            //Mode buttons disabled for 1 minute after all the computer's statuses go to off - backup reset is 3 minutes
            case Constants.CLASSROOM_MODE:
                backupDelay = 3 * 60 * 1000; //3 minutes
                waitForStations("Scene", sceneName,room,"Off", active);
                break;

            case Constants.BASIC_ON_MODE:
                backupDelay = 3 * 60 * 1000; //3 minutes
                waitForStations("Backup", sceneName, room,"On", active);
                break;

            case Constants.BASIC_OFF_MODE:
                backupDelay = 3 * 60 * 1000; //3 minutes
                waitForStations("Backup", sceneName, room,"Off", active);
                break;

            //No Station actions, wait for 5 seconds before unlocking scenes
            case Constants.BASIC_MODE:
                backupDelay = 5 * 1000;
                break;

            default:
                return;
        }

        //Create a backup timer that cancels all tasks in case something has gone wrong
        createModeResetTimer(backupDelay, active, room, true);
    }

    /**
     * Wait for all Stations within the specified context to reach their target status.
     * @param context The context within which stations are to be collected (Room or Scene).
     * @param sceneName A string of the scene that is being triggered as to collect associated
     *                  stations (left empty for room collection).
     * @param room A string of the room linked to the scene.
     * @param targetStatus A string of the status the Station should end on (On or Off).
     */
    private void waitForStations(String context, String sceneName, String room, String targetStatus, String mode) {
        periodicChecker = new PeriodicChecker();
        AtomicBoolean isFirst = new AtomicBoolean(true);

        // Stops the periodic check if the callback is true
        ArrayList<?> stations = collectStations(context, sceneName, room, targetStatus);

        periodicChecker.setCallback(() -> {
            //If this passed on the first instance then the scene is already set, only have a few seconds cool down in this instance
            if (checkStationStatuses(stations, targetStatus)) {
                if (isFirst.get()) {
                    createModeResetTimer(5 * 1000, mode, room, false);
                }
                else if (targetStatus.equals("Off")) {
                    //Wait an extra 45 seconds to make sure the Stations are off
                    createModeResetTimer(45 * 1000, mode, room, false);
                }
                else {
                    //The mode is set, cancel the timers
                    cancelModeTimers(mode, room);
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
     * @param room (Optional) The room a scene is linked to.
     * @param targetStatus A string of the status the Station should end on (On or Off).
     * @return A list of Station objects or JSONObjects based on the context.
     */
    public static ArrayList<?> collectStations(String context, String sceneName, String room, String targetStatus) {
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

            case "Backup":
            case "Scene":
                ArrayList<JSONObject> stationObjects = new ArrayList<>();
                ApplianceViewModel applianceViewModel = ApplianceFragment.mViewModel;
                List<Appliance> appliances = applianceViewModel.getAppliances().getValue();

                if (appliances != null) {
                    List<Appliance> matchingAppliances = appliances.stream()
                            .filter(appliance -> appliance.name.toLowerCase().contains(sceneName)
                                    && "scenes".equals(appliance.type)
                                    && appliance.room.equals(room)
                                    && SettingsFragment.checkLockedRooms(appliance.room))
                            .collect(Collectors.toList());

                    if (matchingAppliances.isEmpty()) {
                        matchingAppliances = DashboardFragment.getInstance().searchForBackupSceneTrigger(appliances, sceneName, targetStatus);
                        SceneController.handleBackupScene(matchingAppliances.get(0));
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

            if (station != null) {
                if (!station.statusHandler.getStatus().equals(targetStatus)) {
                    allCorrect = false;
                }
            }
        }

        return allCorrect;
    }

    /**
     * Create a timer for mode reset after a specific check has returned true.
     * I.e. Classroom mode has been triggered, all Stations have now gone to 'Off', start a timer so
     * wake on lan cannot be triggered too early.
     * @param delayInMillis A long representing the delay before triggering the mode reset.
     * @param mode A string of the mode that was initially called.
     * @param room A string of the room that the timer is associated with.
     * @param isBackupTimer Boolean indicating whether this is a backup timer or not.
     */
    private static void createModeResetTimer(long delayInMillis, String mode, String room, boolean isBackupTimer) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                cancelModeTimers(mode, room);
            }
        };

        if (isBackupTimer) {
            modeBackupTimers.put(room, timer);
            modeBackupTasks.put(room, task);
        } else {
            modeTimers.put(room, timer);
            modeTasks.put(room, task);
        }

        timer.schedule(task, delayInMillis);
    }

    /**
     * Cancel the modeTimer, modeBackupTimer and periodicChecker before resetting the mode in
     * the viewModel.
     * @param mode A string of the mode that was initially called.
     * @param room A string of the room that the timer is associated with.
     */
    public static void cancelModeTimers(String mode, String room) {
        if (modeTimers.get(room) != null) {
            Timer timer = modeTimers.get(room);
            if (timer != null) {
                timer.cancel();
            }
            modeTimers.remove(room);

            TimerTask task = modeTasks.get(room);
            if (task != null) {
                task.cancel();
            }
            modeTasks.remove(room);
        }

        if (modeBackupTimers.get(room) != null) {
            Timer timer = modeBackupTimers.get(room);
            if (timer != null) {
                timer.cancel();
            }
            modeBackupTimers.remove(room);

            TimerTask task = modeBackupTasks.get(room);
            if (task != null) {
                task.cancel();
            }
            modeBackupTasks.remove(room);
        }

        if (modeTimers.isEmpty() && modeBackupTimers.isEmpty() && periodicChecker != null) {
            periodicChecker.stop();
            periodicChecker = null;
        }

        //Reset the dashboard buttons and the Scene buttons (Room control page)
        MainActivity.runOnUI(() -> {
            SceneController.resetAfterSceneActivation(mode);
            DashboardFragment.mViewModel.resetMode(mode);
        });
    }

    /**
     * The underlying appliance list has been re-written cancel any timers linked to setting scenes
     * otherwise they will hang when completing.
     */
    public static void forceCancelAllTimers() {
        Log.e("FORCE CANCEL", "Appliance list is re-written, force cancel any timers");

        //Clear all the timers and timer tasks
        cancelAll(modeTimers);
        cancelAll(modeTasks);
        cancelAll(modeBackupTimers);
        cancelAll(modeBackupTasks);

        //Reset all the modes
        List<String> modes = DashboardFragment.mViewModel.getActivatingMode().getValue();
        if (modes == null) return;

        for (String mode : modes) {
            MainActivity.runOnUI(() -> {
                //SceneController.resetAfterSceneActivation(mode);
                SceneController.reset();
                DashboardFragment.mViewModel.resetMode(mode);
            });
        }
    }

    /**
     * Cancels all Timer or TimerTask objects in the provided map.
     *
     * <p>This method iterates through the values of the given map and cancels
     * each value if it is an instance of Timer or TimerTask. It uses the
     * appropriate cancel method based on the type of the object.</p>
     *
     * @param <T> the type of values in the map
     * @param map the map containing Timer or TimerTask objects to be cancelled
     */
    private static <T> void cancelAll(Map<String, T> map) {
        for (T value : map.values()) {
            if (value instanceof Timer) {
                ((Timer) value).cancel();
            } else if (value instanceof TimerTask) {
                ((TimerTask) value).cancel();
            }
        }
    }
}
