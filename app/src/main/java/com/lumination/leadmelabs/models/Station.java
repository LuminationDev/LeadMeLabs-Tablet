package com.lumination.leadmelabs.models;

import java.util.ArrayList;

public class Station {
    public String name;
    public int id;
    public String status;
    public ArrayList<SteamApplication> steamApplications = new ArrayList<>();

    public Station(String name, String steamApplications, int id, String status) {
        this.name = name;
        if (steamApplications != null && steamApplications.length() > 0 && !steamApplications.equals("null")) {
            this.setSteamApplicationsFromJsonString(steamApplications);
        }
        this.id = id;
        this.status = status;
    }

    public void setSteamApplicationsFromJsonString(String steamApplicationsJson)
    {
        steamApplications = new ArrayList<>();
        String[] apps = steamApplicationsJson.split("/");
        for (String app: apps) {
            String[] appData = app.split("\\|");
            steamApplications.add(new SteamApplication(appData[1].replace("\"", ""), Integer.parseInt(appData[0])));
        }
    }
}
