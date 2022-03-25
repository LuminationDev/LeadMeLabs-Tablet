package com.lumination.leadmelabs.models;

import java.util.ArrayList;

public class Station {
    public String name;
    public int number;
    public String status;
    public String activeProcess;
    public ArrayList<Application> applications;

    public Station(String name, int number) {
        this.name = name;
        this.number = number;
    }
}
