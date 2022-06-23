package com.lumination.leadmelabs.models;

import android.os.CountDownTimer;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.ArrayList;

public class Station implements Cloneable {
    public String name;
    public int id;
    public String status;
    public String room;
    public String gameName = null;
    public String gameId;
    public String theatreText;
    public int volume;
    public ArrayList<SteamApplication> steamApplications = new ArrayList<>();
    public int theatreId;
    public boolean selected = false;
    public Appliance associated = null;
    public int automationGroup;
    public int automationId;
    private CountDownTimer timer;

    @Override
    public Station clone() {
        Station clonedStation = null;
        try {
            clonedStation = (Station) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return clonedStation;
    }

    public Station(String name, String steamApplications, int id, String status, int volume, int theatreId, String room) {
        this.name = name;
        if (steamApplications != null && steamApplications.length() > 0 && !steamApplications.equals("Off")) {
            this.setSteamApplicationsFromJsonString(steamApplications);
        }
        this.id = id;
        this.status = status;
        this.volume = volume;
        this.theatreId = theatreId;
        this.room = room;
    }

    public void setSteamApplicationsFromJsonString(String steamApplicationsJson)
    {
        steamApplications = new ArrayList<>();
        String[] apps = steamApplicationsJson.split("/");
        for (String app: apps) {
            String[] appData = app.split("\\|");
            if (appData.length > 1) {
                this.steamApplications.add(new SteamApplication(appData[1].replace("\"", ""), Integer.parseInt(appData[0])));
            }
        }
        this.steamApplications.sort((steamApplication, steamApplication2) -> steamApplication.name.compareToIgnoreCase(steamApplication2.name));
    }

    public boolean hasSteamApplicationInstalled(int steamApplicationId)
    {
        for (SteamApplication steamApplication:steamApplications) {
            if (steamApplication.id == steamApplicationId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Start a countdown to check the station status, if the station has not contacted the NUC
     * within the time limit (3mins) then something has gone wrong and alert the user.
     */
    public void powerStatusCheck() {
        timer = new CountDownTimer(3* 1000 * 60, 2000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                DialogManager.createBasicDialog("STATION ERROR", "Station " + id + " has not powered on correctly");
                MainActivity.runOnUI(() -> {
                    Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);
                    station.status = "Off";
                    ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(id, station);
                });
            }
        }.start();
    }

    /**
     * The station has turned on so cancel the automatic station check.
     */
    public void cancelStatusCheck() {
        if(timer != null) {
            timer.cancel();
        }
    }
}
