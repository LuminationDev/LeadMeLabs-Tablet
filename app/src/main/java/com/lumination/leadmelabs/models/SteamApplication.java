package com.lumination.leadmelabs.models;

public class SteamApplication implements SteamApplicationInterface {
    public String name;
    public int id;

    public SteamApplication(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public static String getImageUrl(int id) {
        return "https://cdn.cloudflare.steamstatic.com/steam/apps/" + id + "/header.jpg";
    }

    public static String getImageUrl(String id) {
        return getImageUrl(Integer.parseInt(id));
    }

    @Override
    public boolean equals(SteamApplication steamApplication) {
        return steamApplication.name.equals(this.name);
    }
}
