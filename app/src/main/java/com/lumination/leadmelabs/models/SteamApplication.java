package com.lumination.leadmelabs.models;

public class SteamApplication {
    public String name;
    public int id;

    public SteamApplication(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getImageUrl() {
        return "https://cdn.cloudflare.steamstatic.com/steam/apps/" + this.id + "/header.jpg";
    }
}
