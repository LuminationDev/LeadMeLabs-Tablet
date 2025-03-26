package com.lumination.leadmelabs.models.applications.details;

import org.json.JSONObject;

public class Actions {
    public String name;
    public String trigger;
    public JSONObject extra;

    public Actions(String name, String trigger) {
        this.name = name;
        this.trigger = trigger;
    }
}
