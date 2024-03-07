package com.lumination.leadmelabs.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.managers.UIUpdateManager;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.library.application.ApplicationLibraryFragment;

import androidx.core.app.NotificationCompat;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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
        NetworkService.sendMessage("NUC", "CanAcknowledge", "");
        NetworkService.sendMessage("NUC", "MessageType", "Android:Unicode");
        NetworkService.sendMessage("NUC", "MessageType", "Android:Json");
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
     * Send a message to the NUCs server.
     */
    public static void sendMessage(String destination, String actionNamespace, String additionalData) {

        String message = "Android:" + destination + ":" + actionNamespace + ":" + additionalData; // add the source and destination at the front

        Log.d(TAG, "Going to send: " + message);
        if (getEncryptionKey().length() == 0) {
            DialogManager.createMissingEncryptionDialog("Unable to communicate with stations", "Encryption key not set. Please contact your IT department for help");
            return;
        }

        if (MainActivity.isNucUtf8) {
            message = EncryptionHelper.encrypt(message, getEncryptionKey());
        } else {
            message = EncryptionHelper.encryptUnicode(message, getEncryptionKey());
        }

        int port = 55556;

        Log.d(TAG, "Attempting to send: " + message);

        String finalMessage = message;
        backgroundExecutor.submit(() -> {
            try {
                InetAddress serverAddress = InetAddress.getByName(NUCAddress);
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddress, port), 4000);

                OutputStream outputStream = socket.getOutputStream();

                // Construct the header
                String headerMessageType = "text";
                byte[] headerMessageTypeBytes;
                if (MainActivity.isNucUtf8) {
                    headerMessageTypeBytes = headerMessageType.getBytes();
                } else {
                    headerMessageTypeBytes = headerMessageType.getBytes(StandardCharsets.UTF_16LE);
                }

                byte[] headerMessageTypeLengthBytes = ByteBuffer.allocate(4).putInt(headerMessageTypeBytes.length).array();

                // Transform the message to a byte array.
                byte[] messageBytes = finalMessage.getBytes();

                // Send the header message type length
                outputStream.write(headerMessageTypeLengthBytes, 0, headerMessageTypeLengthBytes.length);
                // Send the header message type
                outputStream.write(headerMessageTypeBytes, 0, headerMessageTypeBytes.length);

                // Send the message
                outputStream.write(messageBytes, 0, messageBytes.length);

                // Close the output stream and socket
                outputStream.close();
                socket.close();

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

                while (true) {
                    Log.d(TAG, "ServerSocket Created, awaiting connection");
                    Socket clientSocket;

                    try {
                        //blocks the thread until client is accepted
                        clientSocket = mServerSocket.accept();
                    } catch (SocketException e) {
                        Log.e(TAG, e.toString());
                        return;
                    } catch (Exception e) {
                        Sentry.captureException(e);
                        return;
                    }

                    Log.d(TAG, "run: client connected: " + clientSocket.toString());

                    backgroundExecutor.submit(() -> determineMessageType(clientSocket));
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
        String ipAddress = null;

        try {
            WifiManager wm = (WifiManager) MainActivity.getInstance().getSystemService(Context.WIFI_SERVICE);
            ipAddress = InetAddress.getByAddress(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(wm.getConnectionInfo().getIpAddress())
                            .array()
            ).getHostAddress();
        } catch (UnknownHostException e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }

        return ipAddress;
    }

    /**
     * Determine what is being sent to the Android tablet by reading the header from the
     * input stream.
     * @param clientSocket A socket representing the latest connection.
     */
    private void determineMessageType(Socket clientSocket) {
        try {
            DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());

            // Read the header length
            byte[] headerLengthBuffer = new byte[4];
            inputStream.readFully(headerLengthBuffer);
            int headerLength = ByteBuffer.wrap(headerLengthBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt();

            // Read the header
            byte[] headerBuffer = new byte[headerLength];
            inputStream.readFully(headerBuffer);

            //TODO This can be removed in subsequent updates
            MainActivity.isNucUtf8 = DetermineConnectionType(headerBuffer);

            String headerMessageType;
            if (MainActivity.isNucUtf8) {
                headerMessageType = new String(headerBuffer, StandardCharsets.UTF_8);
            } else {
                headerMessageType = new String(headerBuffer, StandardCharsets.UTF_16LE);
            }

            switch (headerMessageType) {
                case "text":
                    receiveMessage(clientSocket, inputStream);
                    break;
                case "image":
                    receiveImage(clientSocket, inputStream);
                    break;
                default:
                    Log.e(TAG, "Unknown connection attempt: " + headerMessageType);
                    break;
            }

        }  catch (IOException e) {
            Log.e(TAG, "Unable to process client request");
            e.printStackTrace();
        }
    }

    /**
     * Determine the incoming connection type, if the header message can be decoded in Unicode and
     * is one of the approved headers then the NUC is Unicode enabled.
     * @param headerBuffer A byte array containing the header message.
     * @return A boolean of if the header is in UTF-8 or Unicode.
     */
    private boolean DetermineConnectionType(byte[] headerBuffer) {
        String message = new String(headerBuffer, StandardCharsets.UTF_16LE);
        return !message.equals("text") && !message.equals("image");
    }

    /**
     * Read what a client has sent to the server.
     * @param clientSocket A socket representing the latest connection.
     */
    private void receiveMessage(Socket clientSocket, DataInputStream inputStream) throws IOException {
        // Read the rest of the stream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] content = new byte[ 2048 ];
        int bytesRead;

        while( ( bytesRead = inputStream.read( content ) ) != -1 ) {
            byteArrayOutputStream.write( content, 0, bytesRead );
        }

        String message = byteArrayOutputStream.toString();
        String unencryptedMessage = message;

        if (getEncryptionKey().length() == 0) {
            DialogManager.createMissingEncryptionDialog("Unable to communicate with stations", "Encryption key not set. Please contact your IT department for help");
            return;
        }

        if (MainActivity.isNucUtf8) {
            message = EncryptionHelper.decrypt(message, getEncryptionKey());
        } else {
            message = EncryptionHelper.decryptUnicode(message, getEncryptionKey());
        }

        if (!message.equals("NUC:Android:Ping")) {
            NetworkService.sendMessage("NUC", "ACK", unencryptedMessage.substring(0, Math.min(30, unencryptedMessage.length())));
        }

        //Get the IP address used to determine who has just connected.
        String ipAddress = clientSocket.getInetAddress().getHostAddress();

        //Message has been received close the socket
        clientSocket.close();

        //Pass the message of to the UI updater
        UIUpdateManager.determineUpdate(message);

        Log.d(TAG, "Message: " + message + " IpAddress:" + ipAddress);
    }

    /**
     * Convert a binary message a client has sent to the server into an image.
     * @param clientSocket A socket representing the latest connection.
     */
    private void receiveImage(Socket clientSocket, DataInputStream inputStream) throws IOException {
        // Read the file name length
        byte[] fileNameLengthBuffer = new byte[4];
        inputStream.readFully(fileNameLengthBuffer);
        int headerLength = ByteBuffer.wrap(fileNameLengthBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt();

        // Read the file name
        byte[] fileNameBuffer = new byte[headerLength];
        inputStream.readFully(fileNameBuffer);
        String fileName;
        if (MainActivity.isNucUtf8) {
            fileName = new String(fileNameBuffer, StandardCharsets.UTF_8);
        } else {
            fileName = new String(fileNameBuffer, StandardCharsets.UTF_16LE);
        }

        // Read the rest of the payload
        ByteArrayOutputStream payloadStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[ 1024 ];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            // Write the data to the payload stream
            payloadStream.write(buffer, 0, bytesRead);
        }
        byte[] imageData = payloadStream.toByteArray();

        // Create a Bitmap object from the byte array
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        // Get the app's internal directory
        File directory = getApplicationContext().getFilesDir();

        // Create a file object for the image
        File file = new File(directory, fileName);

        // Create an output stream for the file
        FileOutputStream outputStream = new FileOutputStream(file);

        // Compress the bitmap and write it to the output stream
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        //Remove from the requested images list (fileName is in the format name_header.jpg
        String[] name = fileName.split("_");
        ImageManager.requestedImages.remove(name[0]);

        try {
            //Notify the ApplicationAdapter that the data has changed
            if (ApplicationLibraryFragment.installedApplicationAdapter != null) {
                MainActivity.runOnUI(() -> ApplicationLibraryFragment.installedApplicationAdapter.notifyDataSetChanged());
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Thumbnail refresh exception: " + e);
        }

        //Message has been received close the byte array stream and the socket
        outputStream.close();
        clientSocket.close();
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
