package com.lumination.leadmelabs.models;

/**
 * A class that holds the individual details for each NovaStar scene.
 */
public class Option {
    public String id;
    public String name;
    private String parentId;

    public Option(String id, String name, String parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getParentId() { return this.parentId; }
}
