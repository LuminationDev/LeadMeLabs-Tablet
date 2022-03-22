package com.lumination.leadmelab.managers;

import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lumination.leadmelab.utilities.Application;
import com.lumination.leadmelab.MainActivity;
import com.lumination.leadmelab.R;
import com.lumination.leadmelab.adapters.ApplicationAdapter;

import java.util.ArrayList;
import java.util.Objects;

public class ApplicationManager {
    private final String TAG = "ApplicationManager";
    private final MainActivity main;

    public ApplicationAdapter mApplicationAdapter;

    private final View stationScreen, applicationScreen;
    private GridView applicationGrid, manageGrid;
    private TextView waitingForApps, manageWaitingForApps;
    private final ProgressBar loading;

    /**
     * The currently selected application.
     */
    public static Application selected;

    public ApplicationManager(MainActivity main) {
        this.main = main;
        this.stationScreen = main.stationScreen;
        this.applicationScreen = main.applicationScreen;
        this.loading = main.stationScreen.findViewById(R.id.indeterminate_bar);

        instantiateClasses();
        setupVariables();
    }

    /**
     * Instantiate any classes that are necessary at the beginning of the application. Creating
     * the necessary layouts and inflated views.
     */
    private void instantiateClasses() {
        mApplicationAdapter = new ApplicationAdapter(main, this, new ArrayList<>());
    }

    /**
     * Set up any elements that are need to be accessed.
     */
    private void setupVariables() {
        //Grid view on the station manager screen
        waitingForApps = stationScreen.findViewById(R.id.no_applications_installed);
        applicationGrid = stationScreen.findViewById(R.id.application_gridView);
        applicationGrid.setAdapter(mApplicationAdapter);

        //Grid view on the application manager screen
        manageWaitingForApps = applicationScreen.findViewById(R.id.no_applications_installed);
        manageGrid = applicationScreen.findViewById(R.id.application_gridView);
        manageGrid.setAdapter(mApplicationAdapter);
    }

    /**
     * Create a number of imageViews that are going to be attached to a Client's application
     * screen, these view represent the steam applications that are currently installed on the
     * client's computer.
     */
    public void populateApplicationList() {
        Log.d(TAG, "Populating application list");
        mApplicationAdapter.clearView();

        //Create the new images from the clients installed list
        ArrayList<Application> apps = Objects.requireNonNull(StationManager.Stations.get(StationManager.getSelected().getNumber())).getApplications();

        changeVisibility(apps);

        for (Application app: apps) {
            mApplicationAdapter.addApplication(app);
        }
    }

    //Change this later to binding
    private void changeVisibility(ArrayList<Application> apps) {
        main.runOnUiThread(() -> {
            waitingForApps.setVisibility(apps.size() > 0 ? View.GONE : View.VISIBLE);
            manageWaitingForApps.setVisibility(apps.size() > 0 ? View.GONE : View.VISIBLE);
            applicationGrid.setVisibility(apps.size() > 0 ? View.VISIBLE : View.GONE);
            manageGrid.setVisibility(apps.size() > 0 ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * Display an indeterminate progress bar while waiting to search for stations.
     * @param visible A boolean representing if the progress bar should be shown.
     */
    public void showLoading(boolean visible) {
        main.runOnUiThread(() -> loading.setVisibility(visible ? View.VISIBLE : View.GONE));
    }
}
