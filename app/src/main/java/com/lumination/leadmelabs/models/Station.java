package com.lumination.leadmelabs.models;

import java.util.ArrayList;

public class Station {
    public String name;
    public int id;
    public String status = null;
    public ArrayList<SteamApplication> steamApplications = new ArrayList<>();

    public Station(String name, String steamapps, int id) {
        this.name = name;
        String[] apps = steamapps.split("/");
        for (String app: apps) {
            String[] appData = app.split("\\|");
            steamApplications.add(new SteamApplication(appData[1].replace("\"", ""), Integer.parseInt(appData[0])));
        }
        this.id = id;
    }
}
