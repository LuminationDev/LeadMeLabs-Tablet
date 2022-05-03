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

import com.lumination.leadmelabs.BuildConfig;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.UIUpdateManager;

import androidx.core.app.NotificationCompat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A service responsible for the receiving and sending of messages.
 */
@TargetApi(29)
public class NetworkService extends Service {
    private static final String TAG = "NetworkService";
    private static final String CHANNEL_ID = "network_service";
    private static final String CHANNEL_NAME = "Network_Service";

    private static String NUCAddress;
    private ServerSocket mServerSocket;

    private final ThreadPoolExecutor serverThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    private static final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

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

    public static String getNUCAddress() { return NUCAddress; }

    /**
     * Send a message to the NUC's server.
     */
    public static void sendMessage(String destination, String actionNamespace, String additionalData) {

        String message = "Android:" + destination + ":" + actionNamespace + ":" + additionalData; // add the source and destination at the front

        message = EncryptionHelper.encrypt(message, BuildConfig.APP_KEY);
        int port = 8080;

        Log.d(TAG, "Attempting to send: " + message);

        String finalMessage = message;
        backgroundExecutor.submit(() -> {
            try {
                InetAddress serverAddress = InetAddress.getByName(NUCAddress);
                Socket soc = new Socket(serverAddress, port);

                OutputStream toServer = soc.getOutputStream();
                PrintWriter output = new PrintWriter(toServer);
                output.println(finalMessage);
                DataOutputStream out = new DataOutputStream(toServer);
                out.writeBytes(finalMessage);

                toServer.close();
                output.close();
                out.close();
                soc.close();

                Log.d(TAG, "Message sent closing socket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Start a server on the device to receive messages from clients.
     */
    public void startServer() {
        int port = 3000;

        serverThreadPool.submit(() -> {
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

                    backgroundExecutor.submit(() -> receiveMessage(clientSocket));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating ServerSocket: ", e);
                e.printStackTrace();
            }
        });
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
            message = EncryptionHelper.decrypt(message, BuildConfig.APP_KEY);

            //Get the IP address used to determine who has just connected.
            String ipAddress = clientSocket.getInetAddress().getHostAddress();

            //Message has been received close the socket
            clientSocket.close();

            //Pass the message of to the UI updater
            UIUpdateManager.determineUpdate(message);

            Log.d(TAG, "Message: " + message + " IpAddress:" + ipAddress);

        } catch (IOException e) {
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
            } finally {
                serverThreadPool.shutdown();
            }
        }
    }

    /**
     * Send a broadcast to all devices on the local network looking for a response by the NUC.
     * @param broadcastMessage A string message to be sent.
     */
    public static void broadcast(String broadcastMessage) {
        broadcastMessage = EncryptionHelper.encrypt(broadcastMessage, BuildConfig.APP_KEY);

        String finalBroadcastMessage = broadcastMessage;
        backgroundExecutor.submit(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] buffer = finalBroadcastMessage.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName("255.255.255.255"), 11000);

                socket.send(packet);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
        startServer();
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
