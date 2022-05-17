package com.lumination.leadmelabs.models;

import java.util.ArrayList;
import java.util.Collections;

public class Station {
    public String name;
    public int id;
    public String status;
    public String gameName;
    public String theatreText;
    public int volume;
    public ArrayList<SteamApplication> steamApplications = new ArrayList<>();
    public int theatreId;
    public boolean selected = false;

    public Station(String name, String steamApplications, int id, String status, int volume, int theatreId) {
        this.name = name;
        if (steamApplications != null && steamApplications.length() > 0 && !steamApplications.equals("null")) {
            this.setSteamApplicationsFromJsonString(steamApplications);
        }
        this.id = id;
        this.status = status;
        this.volume = volume;
        this.theatreId = theatreId;
    }

    public void setSteamApplicationsFromJsonString(String steamApplicationsJson)
    {
        steamApplications = new ArrayList<>();
        String[] apps = steamApplicationsJson.split("/");
        for (String app: apps) {
            String[] appData = app.split("\\|");
            this.steamApplications.add(new SteamApplication(appData[1].replace("\"", ""), Integer.parseInt(appData[0])));
        }
        Collections.sort(this.steamApplications, (steamApplication, steamApplication2) -> steamApplication.name.compareToIgnoreCase(steamApplication2.name));
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
}
