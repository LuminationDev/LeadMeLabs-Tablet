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
    public static final String VR_ROOM = "VR Room";


    //Appliance Constants
    public static final String SCENE = "scenes";
    public static final String LED = "LED rings";
    public static final String LED_WALLS = "LED walls";
    public static final String SPLICERS = "splicers";
    public static final String BLIND = "blinds";
    public static final String COMPUTER = "computers";
    public static final String SOURCE = "sources";


    //Status Constants
    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";
    public static final String STOPPED = "stopped";
    public static final String SET = "set";
    public static final String LOADING = "loading";
    public static final String DISABLED = "disabled";


    //Value Constants
    public static final String APPLIANCE_OFF_VALUE = "0";
    public static final String BLIND_STOPPED_VALUE = "5";
    public static final String LED_ON_VALUE = "153";
    public static final String SPLICER_MIRROR = "1";
    public static final String SPLICER_STRETCH = "2";
    public static final String APPLIANCE_ON_VALUE = "255";


    //Time Constants
    public static final long MINIMAL_INTERVAL = 15 * 60 * 1000L; // 15 Minutes
    public static final long THREE_HOUR_INTERVAL = 3* 60 * 60 * 1000L; // 3 Hours
    public static final long ONE_DAY_INTERVAL = 24 * 60 * 60 * 1000L; // 1 Day
    public static final long ONE_WEEK_INTERVAL = 7 * 24 * 60 * 60 * 1000L; // 1 Week


    //Scene Subtypes
    //BLIND VALUES
    public static final String BLIND_SCENE_SUBTYPE = "Blind";
    public static final String BLIND_SCENE_CLOSE = "0";
    public static final String BLIND_SCENE_STOPPED = "1";
    public static final String BLIND_SCENE_OPEN = "2";


    //HDMI VALUES
    public static final String SOURCE_HDMI_1 = "255";
    public static final String SOURCE_HDMI_2 = "0";
    public static final String SOURCE_HDMI_3 = "127";


    //Request Codes
    public static final int UPDATE_REQUEST_CODE = 99;


    //Message constants
    public static final String Invalid = "invalid";
    public static final String ShareCode = "shareCode";
    public static final String VideoPlayer = "videoPlayer";
    public static final String VideoPlayerVr = "videoPlayerVr";
    public static final String OpenBrush = "openBrush";


    //File Category constants
    public static final String OPEN_BRUSH_FILE = "OpenBrush";

    //Embedded Application Constants
    public static final String VIDEO_PLAYER_NAME = "Video Player";
    public static final String VIDEO_TYPE_NORMAL = "Normal";
    public static final String VIDEO_TYPE_VR = "Vr";
    public static final String VIDEO_TYPE_BACKDROP = "Backdrop";

    //Headset types
    public static final String  HEADSET_TYPE_VIVE_PRO_1 = "VivePro1";
    public static final String  HEADSET_TYPE_VIVE_PRO_2 = "VivePro2";
    public static final String  HEADSET_TYPE_VIVE_FOCUS_3 = "ViveFocus3";
    public static final String  HEADSET_TYPE_VIVE_BUSINESS_STREAMING = "ViveBusinessStreaming";

    //Dashboard Modes
    public static final String VR_MODE = "vr_mode";
    public static final String SHOWCASE_MODE = "showcase_mode";
    public static final String RESTART_MODE = "restart_mode";
    public static final String SHUTDOWN_MODE = "shutdown_mode";
    public static final String CLASSROOM_MODE = "classroom_mode";
    public static final String BASIC_MODE = "basic_mode"; // No station actions involved
    public static final String BASIC_ON_MODE = "basic_on_mode"; // Some stations turning on
    public static final String BASIC_OFF_MODE = "basic_off_mode"; // Some stations turning off

    //Network Status
    public static final String OFFLINE = "Offline";
    public static final String NO_INTERNET = "No internet";
    public static final String ONLINE = "Online";
}
