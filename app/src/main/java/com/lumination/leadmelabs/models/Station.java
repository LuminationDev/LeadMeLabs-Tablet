package com.lumination.leadmelabs.models;

import android.os.CountDownTimer;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.util.ArrayList;
import java.util.Objects;

public class Station implements Cloneable {
    public String name;
    public int id;
    public String status;
    public String room;
    public String gameName = null;
    public String gameId;
    public String gameType;
    public int volume;
    public ArrayList<Application> applications = new ArrayList<>();
    public boolean selected = false;
    private CountDownTimer timer;
    public String macAddress;
    public String ledRingId;
    public Boolean requiresSteamGuard = false;

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

    public Station(String name, String applications, int id, String status, int volume, String room, String ledRingId, String macAddress) {
        this.name = name;
        if (applications != null && applications.length() > 0 && !applications.equals("Off")) {
            this.setApplicationsFromJsonString(applications);
        }
        this.id = id;
        this.status = status;
        this.volume = volume;
        this.room = room;
        this.macAddress = macAddress;
        this.ledRingId = ledRingId;
    }

    public void setName(String newName)
    {
        name = newName;
    }

    public void setApplicationsFromJsonString(String applicationsJson)
    {
        ArrayList<Application> newApplications = new ArrayList<>();
        String[] apps = applicationsJson.split("/");
        for (String app: apps) {
            String[] appData = app.split("\\|");
            if (appData.length > 1) {
                switch (appData[0]) {
                    case "Custom":
                        newApplications.add(new CustomApplication(appData[0], appData[2].replace("\"", ""), appData[1]));
                        break;
                    case "Steam":
                        newApplications.add(new SteamApplication(appData[0], appData[2].replace("\"", ""), appData[1]));
                        break;
                    case "Vive":
                        newApplications.add(new ViveApplication(appData[0], appData[2].replace("\"", ""), appData[1]));
                        break;
                }
            }
        }
        if (newApplications.size() > 0) {
            newApplications.sort((application, application2) -> application.name.compareToIgnoreCase(application2.name));
            this.applications = newApplications;

            //Check for missing thumbnails
            ImageManager.CheckLocalCache(applicationsJson);
        }
    }

    /**
     * Detect if a particular station has an application installed on it
     * @param applicationId A long that represents the ID of an experience.
     * @return A boolean if the application is installed.
     */
    public boolean hasApplicationInstalled(String applicationId)
    {
        for (Application application:this.applications) {
            if (Objects.equals(application.id, applicationId)) {
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
        //Cancel any previous power checks before starting a new one
        cancelStatusCheck();

        timer = new CountDownTimer(3 * 1000 * 60, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                if(!SettingsFragment.checkLockedRooms(room)) {
                    return;
                }

                DialogManager.createBasicDialog("Station error", name + " has not powered on correctly. Try starting again, and if this does not work please contact your IT department for help");
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
