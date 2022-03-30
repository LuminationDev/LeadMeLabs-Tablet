package com.lumination.leadmelabs.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A service responsible for the receiving and sending of messages.
 */
@TargetApi(29)
public class NetworkService extends Service {
    private static final String TAG = "NetworkService";
    private static final String CHANNEL_ID = "network_service";
    private static final String CHANNEL_NAME = "Network_Service";
    private static MainActivity main;

    private static String NUCAddress;
    private ServerSocket mServerSocket;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    /**
     * Class used for the client Binder.  We know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public NetworkService getService() {
            // Return this instance of NetworkService so clients can call public methods
            return NetworkService.this;
        }
    }

    /**
     * Set the IP address of the NUC so the android tablet can send messages to the NUC.
     * @param ipaddress A string representing IP address of the NUC.
     */
    public static void setNUCAddress(String ipaddress) {
        NUCAddress = ipaddress;
    }
    public static void setMain(MainActivity main) {
        NetworkService.main = main;
    }

    /**
     * Send a message to the NUC's server.
     */
    public static void sendMessage(String destination, String actionNamespace, String additionalData) {

        String message = "Android:" + destination + ":" + actionNamespace + ":" + additionalData; // add the source and destination at the front

        int port = 8080;

        Log.d(TAG, "Attempting to send: " + message);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddress = InetAddress.getByName(NUCAddress);
                    Socket soc = new Socket(serverAddress, port);

                    OutputStream toServer = soc.getOutputStream();
                    PrintWriter output = new PrintWriter(toServer);
                    output.println(message);
                    DataOutputStream out = new DataOutputStream(toServer);
                    out.writeBytes(message);

                    toServer.close();
                    output.close();
                    out.close();
                    soc.close();

                    Log.d(TAG, "Message sent closing socket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * Start a server on the device to receive messages from clients.
     */
    public void startServer() {
        int port = 3000;

        try {
            mServerSocket = new ServerSocket();
            mServerSocket.setReuseAddress(true);
            mServerSocket.bind(new InetSocketAddress(port));

            //noinspection InfiniteLoopStatement
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

    /**
     * Read what a client has sent to the server.
     * @param clientSocket A socket representing the latest connection.
     */
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

            //Get the IP address used to determine who has just connected.
            String ipAddress = clientSocket.getInetAddress().getHostAddress();

            //Message has been received close the socket
            clientSocket.close();

            String[] messageParts = message.split(":", 4);
            String source = messageParts[0];
            String destination = messageParts[1];
            String actionNamespace = messageParts[2];
            String additionalData = messageParts.length > 3 ? messageParts[3] : null;
            if (!destination.equals("Android")) {
                return;
            }

            if (actionNamespace.equals("Stations")) {
                if (additionalData.startsWith("List")) {
                    String jsonString = additionalData.split(":", 2)[1];
                    JSONArray json = new JSONArray(jsonString);
                    StationsViewModel stationsViewModel = new ViewModelProvider(main).get(StationsViewModel.class);
                    main.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                stationsViewModel.setStations(json);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            Log.d(TAG, "Message: " + message + " IpAddress:" + ipAddress);

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Unable to process client request");
            e.printStackTrace();
        }
    }

    /**
     * Shut down the server socket.
     */
    private void stopServer() {
        if(mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startServer();
            }
        });
        thread.start();
    }

    @Override
    public void onDestroy() {
        stopServer();
        endForeground();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    public void startForeground() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);

        final int notificationId = (int) System.currentTimeMillis();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("NetworkService is running in the foreground")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(notificationId, notification);
    }

    public void endForeground() {
        stopSelf();
        stopForeground(true);
    }
}
