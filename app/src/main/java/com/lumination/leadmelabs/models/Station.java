package com.lumination.leadmelabs.models;

import java.util.ArrayList;

public class Station {
    public String name;
    public int id;
    public String status;
    public String gameName;
    public int volume;
    public ArrayList<SteamApplication> steamApplications = new ArrayList<>();

    public Station(String name, String steamApplications, int id, String status, int volume) {
        this.name = name;
        if (steamApplications != null && steamApplications.length() > 0 && !steamApplications.equals("null")) {
            this.setSteamApplicationsFromJsonString(steamApplications);
        }
        this.id = id;
        this.status = status;
        this.volume = volume;
    }

    public void setSteamApplicationsFromJsonString(String steamApplicationsJson)
    {
        steamApplications = new ArrayList<>();
        String[] apps = steamApplicationsJson.split("/");
        for (String app: apps) {
            String[] appData = app.split("\\|");
            this.steamApplications.add(new SteamApplication(appData[1].replace("\"", ""), Integer.parseInt(appData[0])));
        }
    }
}
