package com.lumination.leadmelabs.models.stations;

import android.content.Context;
import android.os.CountDownTimer;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.Arrays;
import java.util.List;

public class StatusHandler {
    //region Statuses
    public static final String ON = "On";
    public static final String TURNING_ON = "Turning On";
    public static final String RESTARTING = "Restarting";
    public static final String IDLE = "Idle";
    public static final String OFF = "Off";

    private static final List<String> OFF_STATUSES = Arrays.asList(StatusHandler.OFF, StatusHandler.TURNING_ON, StatusHandler.RESTARTING);
    private static final List<String> ON_STATUSES = Arrays.asList(StatusHandler.ON, StatusHandler.IDLE);
    //endregion

    private CountDownTimer shutdownTimer;
    private String status;

    protected String getStatus() {
        return this.status;
    }

    protected void setStatus(String status) {
        this.status = status;
    }

    public int getStatusTextColor(Context context) {
        int colorResId;
        switch (status) {
            case StatusHandler.IDLE:
                colorResId = R.color.grey_dark;
                break;

            case StatusHandler.ON:
            case StatusHandler.TURNING_ON:
            case StatusHandler.RESTARTING:
                colorResId = R.color.blue;
                break;

            case StatusHandler.OFF:
            default:
                colorResId = R.color.text;
                break;
        }

        return ContextCompat.getColor(context, colorResId);
    }

    public int getIdleModeColour(Context context) {
        int colorResId;
        switch (status) {
            case StatusHandler.IDLE:
                colorResId = R.color.purple_500;
                break;

            case StatusHandler.ON:
                colorResId = R.color.blue_darkest;
                break;

            case StatusHandler.TURNING_ON:
            case StatusHandler.RESTARTING:
            case StatusHandler.OFF:
            default:
                colorResId = R.color.grey_card;
                break;
        }

        return ContextCompat.getColor(context, colorResId);
    }

    /**
     * Is the Station Off.
     * @return A boolean if the Station cannot receive commands.
     */
    public boolean isStationOff() {
        return status == null || status.equals(OFF);
    }

    /**
     * Is the Station Off, Turning on or Restarting.
     * @return A boolean if the Station cannot receive commands.
     */
    public boolean isStationOffOrChanging() {
        return status != null && OFF_STATUSES.contains(status);
    }

    /**
     * Is the Station On.
     * @return A boolean if the Station can receive commands.
     */
    public boolean isStationOn() {
        return status != null && status.equals(ON);
    }

    /**
     * Is the Station On or Idle.
     * @return A boolean if the Station can receive commands.
     */
    public boolean isStationOnOrIdle() {
        return status != null && ON_STATUSES.contains(status);
    }

    /**
     * Is the Station in Idle mode.
     * @return A boolean if the Station is currently in Idle mode.
     */
    public boolean isStationIdle() {
        return status != null && status.equals(IDLE);
    }

    //region Power Control
    /**
     * Start a countdown to check the station status, if the station has not contacted the NUC
     * within the time limit (3mins) then something has gone wrong and alert the user.
     */
    public void powerStatusCheck(int stationId, long delay) {
        //Cancel any previous power checks before starting a new one
        cancelStatusCheck();

        shutdownTimer = new CountDownTimer(delay, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(stationId);

                if(!SettingsFragment.checkLockedRooms(station.getRoom())) {
                    return;
                }
                DialogManager.createBasicDialog("Station error", station.getName() + " has not powered on correctly. Try starting again, and if this does not work please contact your IT department for help");
                MainActivity.runOnUI(() -> {
                    station.setStatus(StatusHandler.OFF);
                    NetworkService.sendMessage("NUC", "UpdateStation", station.getId() + ":SetValue:status:" + StatusHandler.OFF);
                    ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(station.getId(), station);
                });
            }
        }.start();
    }

    /**
     * The station has turned on so cancel the automatic station check.
     */
    public void cancelStatusCheck() {
        if(shutdownTimer != null) {
            shutdownTimer.cancel();
        }
    }
    //endregion
}
