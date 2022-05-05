package com.lumination.leadmelabs.models;

import java.util.ArrayList;
import java.util.HashSet;

public class Zone {
    public String name;
    public int id;
    public String automationValue;
    public String activeSceneName;
    public ArrayList<Scene> scenes;
    public int[] zoneTheatreIds;

    public Zone(String name, int id, String automationValue, ArrayList<Scene> scenes) {
        this.name = name;
        this.id = id;
        this.automationValue = automationValue;
        this.scenes = scenes;
        HashSet<Integer> theatreIds = new HashSet<>();
        for (Scene scene:scenes) {
            for (int theatreId:scene.theatreIds) {
                theatreIds.add(theatreId);
            }
        }

        zoneTheatreIds = theatreIds.stream().mapToInt(Number::intValue).toArray();
    }
}
