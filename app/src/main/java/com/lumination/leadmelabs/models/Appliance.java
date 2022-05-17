package com.lumination.leadmelabs.models;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Turn into generic abstract class in the future for all the different appliances
 */
public class Appliance {
    public String name;
    public String type;
    public ArrayList<String> description;
    public int id;
    public int value;
    public MutableLiveData<Integer> icon;
    public MutableLiveData<Boolean> isActive;

    public Appliance(String type, String name, int id) {
        this.type = type;
        this.name = name;
        this.id = id;

        setDescription(type);

        icon = new MutableLiveData<>(null);
        isActive = new MutableLiveData<>(false);
    }

    /**
     * Set the custom on and off descriptions for the appliance type.
     */
    private void setDescription(String type) {
        switch(type) {
            case "blinds":
                this.description = new ArrayList<>(Arrays.asList("OPEN", "CLOSE"));
                break;

            case "sources":
                this.description = new ArrayList<>(Arrays.asList("HDMI 1", "HDMI 2"));
                break;

            default:
                this.description = new ArrayList<>(Arrays.asList("ON", "OFF"));
                break;
        }
    }
}
