package com.lumination.leadmelabs.models;

public class VrOption {
    public String id;
    public String name;
    public String trigger;

    public VrOption(String id, String name, String trigger) {
        this.id = id;
        this.name = name;
        this.trigger = trigger;
    }

    public String getId() { return this.id; }
    public String getName() {
        return this.name;
    }
    public String getTrigger() {
        return this.trigger;
    }
}
