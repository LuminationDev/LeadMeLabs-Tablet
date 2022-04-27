package com.lumination.leadmelabs.models;

public class SteamApplication implements SteamApplicationInterface {
    public String name;
    public int id;

    public SteamApplication(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getImageUrl() {
        return "https://cdn.cloudflare.steamstatic.com/steam/apps/" + this.id + "/header.jpg";
    }

    @Override
    public boolean equals(SteamApplication steamApplication) {
        return steamApplication.name.equals(this.name);
    }
}
