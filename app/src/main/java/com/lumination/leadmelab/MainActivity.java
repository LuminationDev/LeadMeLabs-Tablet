package com.lumination.leadmelab;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.lumination.leadmelab.managers.DialogManager;
import com.lumination.leadmelab.managers.StartupManager;
import com.lumination.leadmelab.managers.StationManager;
import com.lumination.leadmelab.network.Station;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main";

    public static final int ANIM_HOME_INDEX = 0;
    public static final int ANIM_OPTIONS_INDEX = 1;
    public static final int ANIM_ROOM_INDEX = 2;
    public static final int ANIM_STATION_MANAGER_INDEX = 3;
    public static final int ANIM_STATION_INDEX = 4;
    public static final int ANIM_APPLICATION_INDEX = 5;

    public Context context;
    protected ViewAnimator labAnimator;
    public View mainScreen, optionsScreen, roomScreen, stationManagerScreen, stationScreen, applicationScreen;

    protected StartupManager mStartupManager;

    protected StationManager mStationManager;
    protected DialogManager mDialogManager;

    private TextView stationName, stationStatus, stationRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        inflateViews();
        setupAnimator();
        setupMain();
        instantiateClasses();
        runStartup();
        setupSelectStation();

        setContentView(labAnimator);
    }

    /**
     * Instantiate any classes that are necessary at the beginning of the application. Creating
     * the necessary layouts and inflated views.
     */
    private void instantiateClasses() {
        mStartupManager = new StartupManager(this);
        mStationManager = new StationManager(this);
        mDialogManager = new DialogManager(mStationManager, MainActivity.this);
    }

    private void runStartup() {
        mStationManager.startup();
    }

    /**
     * Setup the main two buttons to switch between room commands and station commands.
     */
    private void setupMain() {
        Button roomCommandsBtn = mainScreen.findViewById(R.id.room_commands_btn);
        roomCommandsBtn.setOnClickListener(view -> changeScreen(ANIM_ROOM_INDEX));

        Button stationCommandsBtn = mainScreen.findViewById(R.id.station_commands_btn);
        stationCommandsBtn.setOnClickListener(view -> changeScreen(ANIM_STATION_MANAGER_INDEX));
    }

    /**
     * Ready the views necessary to load each part of the application.
     */
    private void inflateViews() {
        mainScreen = View.inflate(context, R.layout.l__main_screen, null);
        optionsScreen = View.inflate(context, R.layout.l__options_screen, null);
        roomScreen = View.inflate(context, R.layout.l__room_screen, null);
        stationManagerScreen = View.inflate(context, R.layout.l__all_station_screen, null);
        stationScreen = View.inflate(context, R.layout.l__station_screen, null);
        applicationScreen = View.inflate(context, R.layout.l__steam_application_screen, null);
    }

    /**
     * Setup start switcher and main animator.
     */
    private void setupAnimator() {
        labAnimator = (ViewAnimator) View.inflate(context, R.layout.a__viewanimator, null);
        Log.d(TAG, "Got animator: " + labAnimator);

        labAnimator.addView(mainScreen);
        labAnimator.addView(optionsScreen);
        labAnimator.addView(roomScreen);
        labAnimator.addView(stationManagerScreen);
        labAnimator.addView(stationScreen);
        labAnimator.addView(applicationScreen);
    }

    /**
     * Get the station screen text views for changing later when a station has been selected.
     */
    public void setupSelectStation() {
        stationName = stationScreen.findViewById(R.id.station_details);
        stationStatus = stationScreen.findViewById(R.id.station_status);
        stationRunning = stationScreen.findViewById(R.id.station_running);
    }

    /**
     * Set the text views for the station screen.
     * @param mStation A Station instance the represents the newly selected station.
     */
    public void setStation(Station mStation) {
        stationName.setText(mStation.getName());
        stationStatus.setText(mStation.getStatus());
        stationRunning.setText(mStation.getRunning());
    }

    /**
     * Change the current view to the desired one using the labAnimator.
     * @param index An int representing the index of the desired view within the labAnimator.
     */
    public void changeScreen(int index) {
        labAnimator.setDisplayedChild(index);
    }

    /**
     * Access the dialog manager to open notifications or alerts.
     * @return The instance of the dialog manager
     */
    public DialogManager getDialogManager() {
        return this.mDialogManager;
    }
}
