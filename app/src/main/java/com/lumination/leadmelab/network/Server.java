package com.lumination.leadmelab.network;

import android.util.Log;

import com.lumination.leadmelab.managers.StationManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Creates a server socket on the device which waits and listens for any connections for the
 * computer servers, any connection that comes through will leave a message and can be handled
 * appropriately from there.
 */
public class Server implements Runnable {
    private final String TAG = "Server";
    private final StationManager manager;

    public Server(StationManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            ServerSocket mServerSocket = new ServerSocket();
            mServerSocket.setReuseAddress(true);
            int port = 3000;
            mServerSocket.bind(new InetSocketAddress(port));

            while (true) {
                Log.d(TAG, "ServerSocket Created, awaiting connection");
                Socket clientSocket;

                try {
                    //blocks the thread until client is accepted
                    clientSocket = mServerSocket.accept();
                } catch (IOException e) {
                    throw new RuntimeException("Error creating client", e);
                }

                Log.d(TAG, "run: client connected: " + clientSocket.toString());

                receiveMessage(clientSocket);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating ServerSocket: ", e);
            e.printStackTrace();
        }
    }

    private void receiveMessage(Socket clientSocket) {
        try {
            // get the input stream from the connected socket
            InputStream inputStream = clientSocket.getInputStream();
            // read from the stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] content = new byte[ 2048 ];
            int bytesRead;

            while( ( bytesRead = inputStream.read( content ) ) != -1 ) {
                baos.write( content, 0, bytesRead );
            }

            String message = baos.toString();

            //Message has been received close the socket
            clientSocket.close();

            //Get the IP address used to determine who has just connected.
            String ipAddress = clientSocket.getInetAddress().getHostAddress();

            determineAction(message, ipAddress);

        } catch (IOException e) {
            Log.e(TAG, "Unable to process client request");
            e.printStackTrace();
        }
    }

    /**
     * Determine what action to take after receiving a message from a client computer.
     */
    private void determineAction(String message, String ID) {
        Log.d(TAG, message);

        //Hard coded for now
        ID = "1";

        String[] split = message.split(":");
        String action = split[1];
        String information = "none";

        if(split.length > 2) {
            information = split[2];
        }

        switch(action) {
            case "LumiMachine":
                //Can be used later to automatically add any computers?
                Log.d(TAG, "New connection: " + ID);
                manager.createNewClient("Computer", ID);
                break;

            case "Communication":
                Log.d(TAG, "Message from: " + ID);
                Log.d(TAG, "Communication event: " + information);
                break;

            case "Action":
                Log.d(TAG, "Message from: " + ID);
                Log.d(TAG, "Action event: " + information);
                break;

            case "Applications":
                Log.d(TAG, "Message from: " + ID);
                Log.d(TAG, "Incoming application list: " + information);
                String[] appIDs = information.split("/");

                //If there are no IDs stop
                if(appIDs[0].equals("none")) {
                    break;
                }

                //Turn the information string into an array list.
                ArrayList<String> apps = new ArrayList<>(Arrays.asList(appIDs));

                // Add all the applications to the client
                Objects.requireNonNull(StationManager.Stations.get(ID)).setApplications(apps);
                manager.populateApplications();
                break;

            case "Launch":
                Log.d(TAG, "Message from: " + ID);
                Log.d(TAG, "Launched Application: " + information);
                //Update the station manager
                break;

            case "Install":
                Log.d(TAG, "Message from: " + ID);
                Log.d(TAG, "Installed Application: " + information);
                //Update the application adapter
                break;

            case "Uninstall":
                Log.d(TAG, "Message from: " + ID);
                Log.d(TAG, "Uninstalled Application: " + information);
                //Update the application adapter
                break;

            default:
                Log.d(TAG, "Message from: " + ID);
                Log.d(TAG, message);
                Log.e(TAG, "Unexpected message, discarding");
        }
    }
}
