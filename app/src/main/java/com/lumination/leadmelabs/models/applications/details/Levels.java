package com.lumination.leadmelabs.models.applications.details;

import java.util.ArrayList;

public class Levels {
    public String name;
    public String trigger;
    private final ArrayList<Actions> actions = new ArrayList<>();

    public Levels(String name, String trigger) {
        this.name = name;
        this.trigger = trigger;
    }

    public void addAction(Actions action) {
        this.actions.add(action);
    }

    public ArrayList<Actions> getActions() {
        return this.actions;
    }
}
