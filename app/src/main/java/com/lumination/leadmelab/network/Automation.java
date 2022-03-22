package com.lumination.leadmelab.network;

import android.util.Log;

import com.lumination.leadmelab.managers.StationManager;
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
public class Automation implements Runnable {
    private final String TAG = "Automation";
    private final int port = 8080; //Hard coded for now but switch to server set in future

    private String mCommand;

    public Automation()
    {
    }

    /**
     * Set the command message that will be sent to the Clients server for processing.
     * @param command A String representing a set of instructions.
     */
    public void setCommand(String command) {
        this.mCommand = command;
    }

    @Override
    public void run() {
        Log.d(TAG, "Attempting to connect");
        Log.d(TAG, "Command: " + mCommand);
        Log.d(TAG, "Port: " + port);

        try {
            InetAddress serverAddress = InetAddress.getByName(StationManager.NUCIPAddress);
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
