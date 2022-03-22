package com.lumination.leadmelab.managers;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lumination.leadmelab.network.Automation;
import com.lumination.leadmelab.utilities.Application;
import com.lumination.leadmelab.MainActivity;
import com.lumination.leadmelab.utilities.NetworkSniffer;
import com.lumination.leadmelab.R;
import com.lumination.leadmelab.network.Server;
import com.lumination.leadmelab.network.Station;
import com.lumination.leadmelab.adapters.StationAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StationManager {
    private static final String TAG = "Station Manager";

    //Messages for SteamCMD commands
    public static final String APPS = "Station:Steam:Applications:";
//    public static final String STOP = "Station:Steam:Stop:";
    public static final String INSTALL = "Station:Steam:Install:";
    public static final String UNINSTALL = "Station:Steam:Uninstall:";

    //Messages for commandline activations
    public static final String START_VR = "Station:CommandLine:StartVR";
    public static final String RESTART_VR = "Station:CommandLine:RestartVR";
    public static final String STOP_VR = "Station:CommandLine:EndVR";
    public static final String LAUNCH = "Station:CommandLine:Launch:";
    public static final String URL = "Station:CommandLine:URL:www.stackoverflow.com"; //TODO hardcoded for testing

    //TODO REMOVE DO NOT SEND COMMANDS OVER THE NETWORK - only for example and testing
    public static final String EXPLORER = "Station:CommandLine:TEST:explorer"; //TODO hardcoded for testing

    //Messages for automation activites
    public static final String LIGHT_ON = "Automation:Lighton";
    public static final String LIGHT_OFF = "Automation:Lightoff";

    protected Context context;
    private final GridView stationGrid;
    private final TextView waitingForStations;
    private final ProgressBar loading;

    private Server mServer;
    private final MainActivity main;
    private NetworkSniffer mNetworkSniffer;
    public ApplicationManager mApplicationManager;
    protected StationAdapter mStationAdapter;

    private final View stationManagerScreen;

    /**
     * Variable to hold the current computer clients that are known to the application. The key is
     * the IP Address of the client, this way any time a client connects it can instantly be
     * recognised.
     */
    public static HashMap<String, Station> Stations;

    public static String NUCIPAddress;

    /**
     * The currently selected Client
     */
    public static Station selected;

    private static final ThreadPoolExecutor backgroundExecutor = new ThreadPoolExecutor(
            2,
            3,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    public StationManager(MainActivity main) {
        this.main = main;
        this.context = main.context;
        this.stationManagerScreen = main.stationManagerScreen;
        this.stationGrid = stationManagerScreen.findViewById(R.id.stationListView);
        this.waitingForStations = stationManagerScreen.findViewById(R.id.no_stations_connected);
        this.loading = stationManagerScreen.findViewById(R.id.indeterminate_bar);

        Stations = new HashMap<>();
    }

    /**
     * Create the necessary classes and button actions
     */
    public void startup() {
        instantiateClasses();
        setupManagerButtons();

        startServer();
    }

    /**
     * Instantiate any classes that are necessary at the beginning of the application. Creating
     * the necessary layouts and inflated views.
     */
    private void instantiateClasses() {
        mServer = new Server(this);
        mNetworkSniffer = new NetworkSniffer(context);
        mApplicationManager = new ApplicationManager(main);
        mStationAdapter = new StationAdapter(main, this, new ArrayList<>());

        stationGrid.setAdapter(mStationAdapter);
    }

    /**
     * Find the buttons on the l__all_station_screen layout and assign the on click functions to each.
     */
    private void setupManagerButtons() {
        Button findStationsBtn = stationManagerScreen.findViewById(R.id.core_finder);
        findStationsBtn.setOnClickListener(view -> startNetworkSniffer());

        Button verifyStationsBtn = stationManagerScreen.findViewById(R.id.core_verify_station);
        verifyStationsBtn.setOnClickListener(view -> StationManager.executeStationCommand("Communication:Hello Python"));

        Button addStationBtn = stationManagerScreen.findViewById(R.id.core_add_station);
        addStationBtn.setOnClickListener(view -> main.getDialogManager().showNewClientDialog());

        Button addNUCBtn = stationManagerScreen.findViewById(R.id.core_nuc_station);
        addNUCBtn.setOnClickListener(view -> main.getDialogManager().showNUCDialog());

        ImageView backBtn = stationManagerScreen.findViewById(R.id.leadme_icon);
        backBtn.setOnClickListener(view -> main.changeScreen(MainActivity.ANIM_HOME_INDEX));
    }

    /**
     * Set a new command message for a client and submit it to the background executor for running.
     * @param command A String representing the a message to be received by the Client.
     */
    public static void executeStationCommand(String command) {
        Log.d(TAG, "Active threads: " + backgroundExecutor.getActiveCount());

        Station current = Stations.get(selected.getNumber());

        if (current != null) {
            Log.d(TAG, "Client: " + current.toString() + " Command: " + command);

            current.setCommand(command);
            backgroundExecutor.submit(current);
        }
    }

    /**
     * Start a new automation runnable and send through a command
     * @param command A String representing the a message to be received by the NUC.
     */
    public static void executeAutomationCommand(String command) {
        Log.d(TAG, "Active threads: " + backgroundExecutor.getActiveCount());

        Automation current = new Automation();

        Log.d(TAG, "Client: " + current.toString() + " Command: " + command);

        current.setCommand(command);
        backgroundExecutor.submit(current);
    }

    /**
     * Start the socket server on the android device, this is the contact point for the client
     * sockets on the computers.
     */
    private void startServer() { backgroundExecutor.submit(mServer); }

    /**
     * Ping each of the devices on the same network mask to see if they are reachable or not.
     */
    private void startNetworkSniffer() {
        mNetworkSniffer.startSniffing();
        showLoading(true);
    }

    /**
     * Get the currently selected Client.
     * @return A string representing the IP Address of the current client.
     */
    public static Station getSelected() {
        return selected;
    }

    /**
     * Remove an application from the stations list, occurs on uninstall
     * @param app A ....
     */
    public static void removeApp(Application app) {
        Stations.get(getSelected().getNumber()).removeApplication(app);
    }

    /**
     * Populate the application list and handle any additional functions that are necessary.
     */
    public void populateApplications() {
        mApplicationManager.populateApplicationList();
    }

    /**
     * Create a new Client from the supplied
     */
    public void createNewClient(String name, String hostIP) {
        Station computer = new Station(name, hostIP);
        Stations.put(hostIP, computer);

        mStationAdapter.addStation(computer);
    }

    /**
     * Display an indeterminate progress bar while waiting to search for stations.
     * @param visible A boolean representing if the progress bar should be shown.
     */
    public void showLoading(boolean visible) {
        main.runOnUiThread(() -> loading.setVisibility(visible ? View.VISIBLE : View.GONE));
    }
}
