package com.lumination.leadmelabs.models.stations;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.interfaces.IApplicationLoadedCallback;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.stations.handlers.StateHandler;
import com.lumination.leadmelabs.models.stations.handlers.StatusHandler;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.ui.stations.controllers.ApplicationController;
import com.lumination.leadmelabs.ui.stations.controllers.AudioController;
import com.lumination.leadmelabs.ui.stations.controllers.VideoController;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.IconManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Station implements Cloneable {
    public String name;
    public int id;
    public String room;
    public String macAddress;
    private final boolean isHiddenStation; //If the Station should be shown and sent messages normally
    public ArrayList<Integer> nestedStations;

    public boolean selected = false;

    public Boolean requiresSteamGuard = false;

    //Handle a Station's status
    //Describes the computer status (Off, On, Turning On, Restarting, Idle)
    public StatusHandler statusHandler = new StatusHandler();

    //Handle a Station's state
    //Describes the state of the LeadMe software
    public StateHandler stateHandler = new StateHandler();

    //Handle video management
    public VideoController videoController;

    //Handle audio management
    public AudioController audioController;

    //Handle application management
    public ApplicationController applicationController;

    //Track animation of icons
    IconManager iconManager = new IconManager();

    //Track the animation of the dots for Awaiting headset connection
    private Timer timer;
    int dotsCount = 0;

    public Station(String name, Object applications, int id, String status, String state, String room, String macAddress, boolean isHiddenStation) {
        this.name = name;
        this.id = id;
        this.setStatus(status);
        this.setState(state);
        this.room = room;
        this.macAddress = macAddress;
        this.isHiddenStation = isHiddenStation;

        //Setup the controllers
        this.videoController = new VideoController(id);
        this.audioController = new AudioController();
        this.applicationController = new ApplicationController(applications);
    }

    /**
     * Get the Id of the Station.
     * @return An int representing the Id of the current Station.
     */
    public int getId() {
        return id;
    }

    /**
     * Get the name of the Station
     * @return A String of the Station's name
     */
    public String getName() {
        return name;
    }

    public void setName(String newName)
    {
        name = newName;
    }

    public String getRoom() {
        return room;
    }

    //region Status Shortcuts
    public String getStatus() {
        return statusHandler.getStatus();
    }
    public void setStatus(String status) {
        this.statusHandler.setStatus(status);
    }
    public boolean isOff() { return this.statusHandler.isStationOff(); }
    public boolean isOn() { return this.statusHandler.isStationOn(); }
    //endregion

    //region State Shortcuts
    public String getState() {
        return stateHandler.getState();
    }
    public void setState(String state) {
        this.stateHandler.setState(state);
    }
    //endregion

    /**
     * If the Station is hidden, it does not receive commands or is shown like normal Stations. They
     * are controlled either through the SingleBoundFragment or if you turn on ShowHiddenStations in
     * the Settings page.
     * @return A boolean of if the Station should be shown and sent commands.
     */
    public boolean getIsHidden() {
        boolean hidden = Boolean.TRUE.equals(ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getShowHiddenStations().getValue());
        return this.isHiddenStation && !hidden;
    }

    @NonNull
    @Override
    public Station clone() {
        Station clonedStation = null;
        try {
            clonedStation = (Station) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        assert clonedStation != null;
        return clonedStation;
    }

    //region Data Binding
    /**
     * Data binding to update the Station content flexbox background.
     */
    @BindingAdapter("stationState")
    public static void setStationStateBackground(FlexboxLayout flexbox, Station selectedStation) {
        if (selectedStation == null) return;

        boolean isStatusOn = selectedStation.statusHandler.isStationOnOrIdle();
        boolean hasState = selectedStation.stateHandler.hasState();
        boolean hasGame = selectedStation.applicationController.hasGame();

        //Station is On and has either a State or a Game running
        if(isStatusOn && (hasState || hasGame)) {
            flexbox.setBackgroundResource(R.drawable.card_station_ripple_white);
        } else {
            flexbox.setBackgroundResource(R.drawable.card_station_ripple_empty);
        }
    }

    /**
     * Data binding to update the Station content text (Status or Game name)
     */
    @BindingAdapter("stationState")
    public static void setStationStateTextAndVisibility(TextView textView, Station selectedStation) {
        if (selectedStation == null) return;

        //Set the visibility value
        boolean isStatusOn = selectedStation.statusHandler.isStationOnOrIdle();
        boolean hasState = selectedStation.stateHandler.hasState();
        boolean hasGame = selectedStation.applicationController.hasGame();
        int visibility = isStatusOn && (hasState || hasGame) ? View.VISIBLE : View.INVISIBLE;
        textView.setVisibility(visibility);

        //Stop the dot animation if it is anything besides Awaiting headset connection
        if(selectedStation.getState() == null || !selectedStation.stateHandler.isAwaitingHeadset()) {
            selectedStation.stopAnimateDots();
        }

        //Set the text value ('Not set' - backwards compatibility, default state when it is not sent across)
        if(selectedStation.stateHandler.isAvailable() || !hasGame) {
            //Show the state if the state is anything but Ready to go
            textView.setText(selectedStation.getState());

            //Start the dot animation if awaiting connection and animator is not already running
            if(selectedStation.stateHandler.isAwaitingHeadset()) {
                selectedStation.startAnimateDots(textView);
            }
        } else {
            selectedStation.stopAnimateDots();
            textView.setText(selectedStation.applicationController.getExperienceName());
        }
    }

    /// <summary>
    /// Starts an animation that updates a TextView with a sequence of dots, creating a loading effect.
    /// The animation displays the text "Awaiting headset connection" followed by 1, 2, 3 dots in a loop,
    /// with each dot appearing at one-second intervals. The sequence restarts after reaching three dots.
    /// </summary>
    /// <param name="textView">The TextView to animate.</param>
    private void startAnimateDots(final TextView textView) {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                textView.post(() -> {
                    StringBuilder animatedText = new StringBuilder("Awaiting headset connection");
                    for (int i = 0; i < dotsCount; i++) {
                        animatedText.append('.');
                    }

                    //Collect the current station state
                    VrStation station = (VrStation) ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);

                    //Make sure the state is the same before updating the dots
                    if(station.stateHandler.isAwaitingHeadset()) {
                        textView.setText(animatedText.toString());
                        dotsCount = (dotsCount + 1) % 4; // Change the number of dots as needed
                    } else {
                        this.cancel();
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopAnimateDots() {
        if (timer != null) {
            timer.cancel();
        }
    }
    //endregion

    //region Application Message
    private static final int CHECK_INTERVAL = 1000; // Interval to check the variable in milliseconds
    private static final int TIMEOUT_DURATION = 60000; // Timeout duration in milliseconds
    private int elapsedTime = 0;

    /**
     * Checks if a video player is active in the station and loads the current video.
     * If the video player is not active, it opens the video player application and loads the video.
     * @param currentVideo The current video to be loaded.
     * @return A boolean of if the video was active.
     */
    public boolean checkForVideoPlayer(Video currentVideo) {
        // Add an on click listener to the image if the video player is active
        Application current = applicationController.findCurrentApplication();
        if (!(current instanceof EmbeddedApplication)) { // Video player is not open
            openApplicationAndSendMessage(Constants.VideoPlayerName, () -> videoController.loadTrigger(currentVideo.getName()));
            return false;
        }
        else {
            String subtype = current.subtype.optString("category", "");
            if (subtype.equals(Constants.VideoPlayer)) {
                videoController.loadTrigger(currentVideo.getName());
                return true;
            } else { //Video player is not open
                openApplicationAndSendMessage(Constants.VideoPlayerName, () -> videoController.loadTrigger(currentVideo.getName()));
                return false;
            }
        }
    }

    /**
     * Opens an application and sends a command once the program has started.
     *
     * @param applicationName The application that is going to be launched.
     * @param callback The callback to be executed after opening the application.
     */
    public void openApplicationAndSendMessage(String applicationName, IApplicationLoadedCallback callback) {
        //Collect the application Id from the application list
        Application application = applicationController.findApplicationByName(applicationName);

        //Send the command to open the video player
        JSONObject message = new JSONObject();
        try {
            message.put("Action", "Launch");
            message.put("ExperienceId", application.getId());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        NetworkService.sendMessage("Station," + getId(), "Experience", message.toString());

        //Set a watching to check the stations gameName
        startPeriodicChecks(applicationName, callback);
    }

    /**
     * Initiates periodic checks for a certain condition.
     * This method schedules periodic checks to determine if a certain condition is met.
     *
     * @param applicationName The application that is going to be launched.
     * @param callback The callback to be executed after opening the application.
     */
    private void startPeriodicChecks(String applicationName, IApplicationLoadedCallback callback) {
        final Runnable checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (applicationName.equals(applicationController.getExperienceName())) {
                    //When it is set to the correct application, send the additional message
                    callback.onApplicationLoaded();
                    elapsedTime = 0; //reset
                    return;
                }
                // Check if timeout occurred
                if (elapsedTime >= TIMEOUT_DURATION) {
                    elapsedTime = 0; //reset

                    MainActivity.runOnUI(() ->
                            DialogManager.createBasicDialog(
                                    "Experience launch failed",
                                    "Launch of " + applicationName + " failed on " + name
                            )
                    );
                    return;
                }
                // Schedule the next check after the interval
                MainActivity.handler.postDelayed(this, CHECK_INTERVAL);
                elapsedTime += CHECK_INTERVAL;
            }
        };

        // Start the first check immediately
        MainActivity.handler.post(checkRunnable);
    }
    //endregion

    //region Nested Stations
    /**
     * Collect the first nested station in the Nested Station array. The Snowy Hydro project only
     * ever has one nested station, however future iterations of the lab may have more.
     * @return A Station object or null.
     */
    public Station getFirstNestedStationOrNull() {
        if (this.nestedStations == null || this.nestedStations.isEmpty())  {
            return null;
        }

        int stationId = this.nestedStations.get(0);
        return ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(stationId);
    }
    //endregion
}
