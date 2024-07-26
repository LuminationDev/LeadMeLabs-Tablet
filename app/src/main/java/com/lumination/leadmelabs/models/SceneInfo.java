package com.lumination.leadmelabs.models;

public class SceneInfo {
    private final String room;
    private final String id;

    public SceneInfo(String room, String id) {
        this.room = room;
        this.id = id;
    }

    public String getRoom() {
        return room;
    }

    public String getId() {
        return id;
    }
}
