package com.lumination.leadmelabs.models;

import androidx.lifecycle.MutableLiveData;

/**
 * Turn into generic abstract class in the future for all the different appliances
 */
public class Appliance {
    public String name;
    public String type;
    public int id;
    public int value;
    public MutableLiveData<Integer> icon;
    public MutableLiveData<Boolean> isActive;

    public Appliance(String type, String name, int id) {
        this.type = type;
        this.name = name;
        this.id = id;

        icon = new MutableLiveData<>(null);
        isActive = new MutableLiveData<>(false);
    }
}
