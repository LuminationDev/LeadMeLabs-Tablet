package com.lumination.leadmelabs.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.UIUpdateManager;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import io.sentry.Sentry;

/**
 * A service responsible for the receiving and sending of messages.
 */
@TargetApi(29)
public class NetworkService extends Service {
    private static final String TAG = "NetworkService";
    private static final String CHANNEL_ID = "network_service";
    private static final String CHANNEL_NAME = "Network_Service";

    public static String IPAddress;
    private static String NUCAddress;
    private static ServerSocket mServerSocket;

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
        NetworkService.sendMessage("NUC", "Connect", "Connect");
    }

    /**
     * Reset the NUC address to the current value, this re-calls any start up messages and resets
     * the connection in case the NUC has restarted. Including those to get the current Station
     * & Appliance lists.
     */
    public static void refreshNUCAddress() {
        ApplianceViewModel.init = false;
        SettingsFragment.mViewModel.setNucAddress(NetworkService.getNUCAddress());
    }

    public static String getNUCAddress() { return NUCAddress; }
    
    public static String getIPAddress() {
        if(IPAddress == null) {
            IPAddress = collectIpAddress();
        }

        return IPAddress;
    }

    private static String getEncryptionKey() {
        SharedPreferences sharedPreferences = MainActivity.getInstance().getSharedPreferences("encryption_key", Context.MODE_PRIVATE);
        return sharedPreferences.getString("encryption_key", "");
    }

    /**
     * Send a message to the NUC's server.
     */
    public static void sendMessage(String destination, String actionNamespace, String additionalData) {

        String message = "Android:" + destination + ":" + actionNamespace + ":" + additionalData; // add the source and destination at the front

        Log.d(TAG, "Going to send: " + message);
        if (getEncryptionKey().length() == 0) {
            DialogManager.createBasicDialog("Unable to communicate with stations", "Encryption key not set. Please contact your IT department for help");
            return;
        }
        message = EncryptionHelper.encrypt(message, getEncryptionKey());
        int port = 55556;

        Log.d(TAG, "Attempting to send: " + message);

        String finalMessage = message;
        backgroundExecutor.submit(() -> {
            try {
                InetAddress serverAddress = InetAddress.getByName(NUCAddress);
                Socket soc = new Socket();
                soc.connect(new InetSocketAddress(serverAddress, port), 4000);

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
        IPAddress = collectIpAddress();

        int port = 55555;

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
                        Sentry.captureException(e);
                        return;
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
     * Collect the local IP address, regardless if it is IPv4 or IPv6.
     */
    public static String collectIpAddress() {
        WifiManager wm = (WifiManager) MainActivity.getInstance().getSystemService(Context.WIFI_SERVICE);

        String ipAddress = null;

        try {
            ipAddress = InetAddress.getByAddress(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(wm.getConnectionInfo().getIpAddress())
                            .array()
            ).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return ipAddress;
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
            if (getEncryptionKey().length() == 0) {
                DialogManager.createBasicDialog("Unable to communicate with stations", "Encryption key not set. Please contact your IT department for help");
                return;
            }
            message = EncryptionHelper.decrypt(message, getEncryptionKey());

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
        broadcastMessage = EncryptionHelper.encrypt(broadcastMessage, getEncryptionKey());

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
                .setSmallIcon(R.drawable.ic_launcher)
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
