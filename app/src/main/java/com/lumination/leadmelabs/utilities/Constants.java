package com.lumination.leadmelabs.utilities;

public class Constants {
    //Action namespace Constants
    public static final String MESSAGE_TYPE = "MessageType";
    public static final String LAB_LOCATION = "LabLocation";
    public static final String STATIONS = "Stations";
    public static final String APPLIANCES = "Appliances";
    public static final String STATION = "Station";
    public static final String STATION_STATE = "CurrentState";
    public static final String AUTOMATION = "Automation";
    public static final String SEGMENT = "Segment";
    public static final String QA = "QA";


    //Room Constants
    public static String VR_ROOM = "VR Room";


    //Appliance Constants
    public static String SCENE = "scenes";
    public static String LED = "LED rings";
    public static String LED_WALLS = "LED walls";
    public static String SPLICERS = "splicers";
    public static String BLIND = "blinds";
    public static String COMPUTER = "computers";
    public static String SOURCE = "sources";


    //Status Constants
    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";
    public static final String STOPPED = "stopped";


    //Value Constants
    public static String APPLIANCE_OFF_VALUE = "0";
    public static String BLIND_STOPPED_VALUE = "5";
    public static String LED_ON_VALUE = "153";
    public static String SPLICER_MIRROR = "1";
    public static String SPLICER_STRETCH = "2";
    public static String APPLIANCE_ON_VALUE = "255";


    //Time Constants
    public static final long MINIMAL_INTERVAL = 15 * 60 * 1000L; // 15 Minutes
    public static final long THREE_HOUR_INTERVAL = 3* 60 * 60 * 1000L; // 3 Hours
    public static final long ONE_DAY_INTERVAL = 24 * 60 * 60 * 1000L; // 1 Day
    public static final long ONE_WEEK_INTERVAL = 7 * 24 * 60 * 60 * 1000L; // 1 Week


    //Scene Subtypes
    //BLIND VALUES
    public static String BLIND_SCENE_SUBTYPE = "Blind";
    public static String BLIND_SCENE_CLOSE = "0";
    public static String BLIND_SCENE_STOPPED = "1";
    public static String BLIND_SCENE_OPEN = "2";


    //HDMI VALUES
    public static String SOURCE_HDMI_1 = "255";
    public static String SOURCE_HDMI_2 = "0";
    public static String SOURCE_HDMI_3 = "127";


    //Request Codes
    public static int UPDATE_REQUEST_CODE = 99;


    //Message constants
    public static String Invalid = "invalid";
    public static String ShareCode = "shareCode";
    public static String VideoPlayer = "videoPlayer";


    //Embedded Application Constants
    public static String VIDEO_PLAYER_NAME = "Video Player";
    public static String VIDEO_TYPE_NORMAL = "Normal";
    public static String VIDEO_TYPE_VR = "Vr";
    public static String VIDEO_TYPE_BACKDROP = "Backdrop";

    //Headset types
    public static String  HEADSET_TYPE_VIVE_PRO_1 = "VivePro1";
    public static String  HEADSET_TYPE_VIVE_PRO_2 = "VivePro2";
    public static String  HEADSET_TYPE_VIVE_FOCUS_3 = "ViveFocus3";
    public static String  HEADSET_TYPE_VIVE_BUSINESS_STREAMING = "ViveBusinessStreaming";

    //Network Status
    public static final String OFFLINE = "Offline";
    public static final String NO_INTERNET = "No internet";
    public static final String ONLINE = "Online";
}
