package com.lumination.leadmelab.network;

import android.util.Log;

import com.lumination.leadmelab.utilities.Application;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Create a client socket to connect to a computer running a server. Aim is to send a message to
 * the server and then close, nothing more is expected or should be done.
 */
public class Station implements Runnable {
    private final String TAG = "Station";

    private final String name;
    private final String mHostIP;
    private String status = "Online"; //Hard coded for now will run a check in the future
    private String running = "Nothing"; //Hard coded for now will check if steam games running in the future
    private final int port = 8080; //Hard coded for now but switch to server set in future
    private ArrayList<Application> applications;

    private String mCommand;

    public Station(String name, String mHostIP) {
        this.name = name;
        this.mHostIP = mHostIP;
        this.applications = new ArrayList<>();
    }

    /**
     * Set the command message that will be sent to the Clients server for processing.
     * @param command A String representing a set of instructions.
     */
    public void setCommand(String command) {
        this.mCommand = command;
    }

    /**
     * From a list of application IDs create a new application instance for each and add them to
     * the stations application array.
     * @param appIDs An Array list of strings representing the AppIDs installed on a client computer
     *             supplied by Steam.
     */
    public void setApplications(ArrayList<String> appIDs) {
        applications = new ArrayList<>();

        for(String appID : appIDs) {

            String[] appSplit = appID.split("\\|");
            Application newApp = new Application(appSplit[1], appSplit[0]);

            if(!applications.contains(newApp)) {
                applications.add(newApp);
            }
        }
    }

    /**
     * When an application has been uninstalled from a computer remove the application's ID from
     * the array list.
     * @param app An Application instance representing the ID of the recently removed application.
     */
    public void removeApplication(Application app) {
        if (!applications.contains(app)) {
            applications.remove(app);
        }
    }

    /**
     * Get the list of application IDs that are installed on this clients computer
     * @return An Arraylist of Strings that represent installed applications.
     */
    public ArrayList<Application> getApplications() {
        return this.applications;
    }

    /**
     * Access the name of this station.
     * @return A String representing the name of the station.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Access the IP address of this station.
     * @return A String representing the IP Address of the station.
     */
    public String getIP() {
        return this.mHostIP;
    }

    /**
     * Retrieve the current status of the station.
     * @return A String representing what stage a station is in, offline, online or error.
     */
    public String getStatus() { return this.status; }

    /**
     * Retrieve any information about if a station is currently running a steam application.
     * @return A String representing the name and ID of a steam application that may be running.
     */
    public String getRunning() { return this.running; }

    @Override
    public void run() {
        Log.d(TAG, "Attempting to connect");
        Log.d(TAG, "IP Address: " + mHostIP);
        Log.d(TAG, "Port: " + port);

        try {
            InetAddress serverAddress = InetAddress.getByName(mHostIP);
            Socket soc = new Socket(serverAddress, port);

            OutputStream toServer = soc.getOutputStream();
            PrintWriter output = new PrintWriter(toServer);
            output.println(mCommand);
            DataOutputStream out = new DataOutputStream(toServer);
            out.writeBytes(mCommand);

            toServer.close();
            output.close();
            out.close();
            soc.close();
            Log.d(TAG, "Message sent closing scoket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
