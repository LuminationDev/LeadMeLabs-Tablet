package com.lumination.leadmelabs.models.stations;

import android.os.CountDownTimer;
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
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.ui.stations.controllers.ApplicationController;
import com.lumination.leadmelabs.ui.stations.controllers.AudioController;
import com.lumination.leadmelabs.ui.stations.controllers.VideoController;
import com.lumination.leadmelabs.utilities.IconManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class Station implements Cloneable {
    public String name;
    public int id;
    public String status; //Describes the computer status (Off, On, Turning On)
    public String state; //Describes the state of the LeadMe software
    public String room;
    public boolean selected = false;
    private CountDownTimer shutdownTimer;
    public String macAddress;

    public Boolean requiresSteamGuard = false;

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

    public Station(String name, Object applications, int id, String status, String state, String room, String macAddress) {
        this.name = name;
        this.id = id;
        this.status = status;
        this.state = state;
        this.room = room;
        this.macAddress = macAddress;

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

    public void setName(String newName)
    {
        name = newName;
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

        return clonedStation;
    }

    //region Power Control
    /**
     * Start a countdown to check the station status, if the station has not contacted the NUC
     * within the time limit (3mins) then something has gone wrong and alert the user.
     */
    public void powerStatusCheck(long delay) {
        //Cancel any previous power checks before starting a new one
        cancelStatusCheck();

        shutdownTimer = new CountDownTimer(delay, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                if(!SettingsFragment.checkLockedRooms(room)) {
                    return;
                }
                DialogManager.createBasicDialog("Station error", name + " has not powered on correctly. Try starting again, and if this does not work please contact your IT department for help");
                MainActivity.runOnUI(() -> {
                    Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);
                    station.status = "Off";
                    NetworkService.sendMessage("NUC", "UpdateStation", id + ":SetValue:status:Off");
                    ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(id, station);
                });
            }
        }.start();
    }

    /**
     * The station has turned on so cancel the automatic station check.
     */
    public void cancelStatusCheck() {
        if(shutdownTimer != null) {
            shutdownTimer.cancel();
        }
    }
    //endregion

    //region Data Binding
    /**
     * Data binding to update the Station content flexbox background.
     */
    @BindingAdapter("stationState")
    public static void setStationStateBackground(FlexboxLayout flexbox, Station selectedStation) {
        if (selectedStation == null) return;

        boolean isStatusOn = selectedStation.status != null && (!selectedStation.status.equals("Off") && !selectedStation.status.equals("Turning On") && !selectedStation.status.equals("Restarting"));
        boolean hasState = selectedStation.state != null && selectedStation.state.length() != 0;
        boolean hasGame = selectedStation.applicationController.getGameName() != null && selectedStation.applicationController.getGameName().length() != 0 && !selectedStation.applicationController.getGameName().equals("null");

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
        boolean isStatusOn = selectedStation.status != null && (!selectedStation.status.equals("Off") && !selectedStation.status.equals("Turning On") && !selectedStation.status.equals("Restarting"));
        boolean hasState = selectedStation.state != null && selectedStation.state.length() != 0;
        boolean hasGame = selectedStation.applicationController.getGameName() != null && selectedStation.applicationController.getGameName().length() != 0 && !selectedStation.applicationController.getGameName().equals("null");
        int visibility = isStatusOn && (hasState || hasGame) ? View.VISIBLE : View.INVISIBLE;
        textView.setVisibility(visibility);

        //Stop the dot animation if it is anything besides Awaiting headset connection
        if(selectedStation.state == null || !selectedStation.state.equals("Awaiting headset connection...")) {
            selectedStation.stopAnimateDots();
        }

        //Set the text value ('Not set' - backwards compatibility, default state when it is not sent across)
        if(selectedStation.state != null && (!selectedStation.state.equals("Ready to go") || !hasGame) && !selectedStation.state.equals("Not set")) {
            //Show the state if the state is anything but Ready to go
            textView.setText(selectedStation.state);

            //Start the dot animation if awaiting connection and animator is not already running
            if(selectedStation.state.equals("Awaiting headset connection...")) {
                selectedStation.startAnimateDots(textView);
            }
        } else {
            selectedStation.stopAnimateDots();
            textView.setText(selectedStation.applicationController.getGameName());
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
                    if(station.state.equals("Awaiting headset connection...")) {
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

    //region ApplicationMessage
    private static final int CHECK_INTERVAL = 1000; // Interval to check the variable in milliseconds
    private static final int TIMEOUT_DURATION = 60000; // Timeout duration in milliseconds
    private int elapsedTime = 0;

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
        NetworkService.sendMessage("Station," + MainActivity.getStationId(), "Experience", message.toString());

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
                if (applicationName.equals(applicationController.getGameName())) {
                    //When it is set to the correct application, send the additional message
                    callback.onApplicationLoaded();
                    return;
                }
                // Check if timeout occurred
                if (elapsedTime >= TIMEOUT_DURATION) {
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
}
