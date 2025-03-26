package com.lumination.leadmelabs.models.applications.details;

import java.util.ArrayList;


/**
 * Holds information relevant to an application. An application may have global actions, levels and
 * actions within those levels.
 */
public class Details {
    public String name;

    /**
     * Actions that can be applied to any scene, regardless of the current state of the application.
     * This could be an alternate shutdown, reset, restart etc..
     */
    private ArrayList<Actions> GlobalActions = new ArrayList<>();

    /**
     * Levels consist of the name and what is trigger to activate that level. Each level may contain
     * different actions that can be taken.
     */
    private ArrayList<Levels> Levels = new ArrayList<>();

    public Details(String name) {
        this.name = name;
    }

    public void setGlobalActions(ArrayList<Actions> actions) {
        this.GlobalActions = actions;
    }

    public void addGlobalAction(Actions action) {
        this.GlobalActions.add(action);
    }

    public ArrayList<Actions> getGlobalActions() {
        return GlobalActions;
    }

    public void setLevels(ArrayList<Levels> levels) {
        this.Levels = levels;
    }

    public void addLevels(Levels levels) {
        this.Levels.add(levels);
    }

    public ArrayList<Levels> getLevels() {
        return Levels;
    }
}
