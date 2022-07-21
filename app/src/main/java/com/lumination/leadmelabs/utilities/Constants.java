package com.lumination.leadmelabs.utilities;

public class Constants {
    //Appliance Constants
    public static String SCENE = "scenes";
    public static String LED = "LED rings";
    public static String BLIND = "blinds";
    public static String COMPUTER = "computers";


    //Status Constants
    public static String ACTIVE = "active";
    public static String INACTIVE = "inactive";
    public static String STOPPED = "stopped";


    //Value Constants
    public static String APPLIANCE_OFF_VALUE = "0";
    public static String BLIND_STOPPED_VALUE = "5";
    public static String LED_ON_VALUE = "153";
    public static String APPLIANCE_ON_VALUE = "255";


    //Time Constants
    public static final long MINIMAL_INTERVAL = 15 * 60 * 1000L; // 15 Minutes
    public static final long THREE_HOUR_INTERVAL = 3* 60 * 60 * 1000L; // 3 Hours
    public static final long ONE_DAY_INTERVAL = 24 * 60 * 60 * 1000L; // 1 Day
    public static final long ONE_WEEK_INTERVAL = 7 * 24 * 60 * 60 * 1000L; // 1 Week
}
