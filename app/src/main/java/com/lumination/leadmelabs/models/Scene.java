package com.lumination.leadmelabs.models;

import androidx.lifecycle.MutableLiveData;

public class Scene {
    public String name;
    public int number;
    public int value;
    public MutableLiveData<Integer> icon;
    public MutableLiveData<Boolean> isActive;

    public Scene(String name, int number, int value) {
        this.name = name;
        this.number = number;
        this.value = value;

        icon = new MutableLiveData<>();
        isActive = new MutableLiveData<>(false);
    }
}
