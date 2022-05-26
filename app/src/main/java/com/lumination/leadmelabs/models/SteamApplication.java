package com.lumination.leadmelabs.models;

import java.util.Objects;

public class SteamApplication {
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
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SteamApplication other = (SteamApplication) obj;
        return Objects.equals(id, other.id);
    }
}
