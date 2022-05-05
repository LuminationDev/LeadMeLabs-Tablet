package com.lumination.leadmelabs.models;

import androidx.lifecycle.MutableLiveData;

public class Scene {
    public String name;
    public int number;
    public String value;
    public MutableLiveData<Boolean> isActive;
    public int[] theatreIds;

    public Scene(String name, int number, String value, int[] theatreIds) {
        this.name = name;
        this.number = number;
        this.value = value;
        this.theatreIds = theatreIds;

        isActive = new MutableLiveData<>(false);
    }
}
