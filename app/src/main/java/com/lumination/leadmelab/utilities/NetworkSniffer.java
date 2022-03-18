package com.lumination.leadmelab.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Sniff out all devices on the network, looking for a particular host name.
 */
public class NetworkSniffer {
    private final String TAG = "NetworkSniffer";
    private final WeakReference<Context> mContextRef;
    protected final static ThreadPoolExecutor snifferExecutor = new ThreadPoolExecutor(
            2,
            3,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    //Holds potential IP address that have been deemed reachable by the sniffer threads
    public static ArrayList<String> PotentialIpAddresses = new ArrayList<>();

    public NetworkSniffer(Context context) {
        mContextRef = new WeakReference<>(context);
    }

    public void startSniffing() {
        Context context = mContextRef.get();

        if (context != null) {

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            String ipAddress = null;

            try {
                ipAddress = InetAddress.getByAddress(
                        ByteBuffer
                                .allocate(Integer.BYTES)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .putInt(wifiManager.getConnectionInfo().getIpAddress())
                                .array()
                ).getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            if (ipAddress == null) {
                return;
            }

            Log.d(TAG, "activeNetwork: " + activeNetwork);
            Log.d(TAG, "ipString: " + ipAddress);

            String prefix = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
            Log.d(TAG, "prefix: " + prefix);


            //Set up the different threads
            //Normal Tablet will be Dual core, max efficiency on a Quad core though
            Sniffer thread1 = new Sniffer("1", prefix, 0, 63);
            Sniffer thread2 = new Sniffer("2", prefix, 64, 127);
            Sniffer thread3 = new Sniffer("3", prefix, 128, 191);
            Sniffer thread4 = new Sniffer("4", prefix, 192, 255);
            snifferExecutor.submit(thread1);
            snifferExecutor.submit(thread2);
            snifferExecutor.submit(thread3);
            snifferExecutor.submit(thread4);
        }
    }

    /**
     * Cycle through the potential IP address and ping each one for a connection.
     */
    public static void checkConnections() {
        for (String ipAddress: PotentialIpAddresses) {
            snifferExecutor.submit(() -> sendPing(ipAddress));
        }
    }

    private static void sendPing(String ipAddress) {
        String TAG = "Ping";
        Log.d(TAG, "Attempting to connect");
        Log.d(TAG, "IP Address: " + ipAddress);
        Log.d(TAG, "Port: " + 8080);

        try {
            InetAddress serverAddress = InetAddress.getByName(ipAddress);
            Socket soc = new Socket(serverAddress, 8080);
            soc.setSoTimeout(300);

            OutputStream toServer = soc.getOutputStream();
            PrintWriter output = new PrintWriter(toServer);
            output.println("Connection:");
            DataOutputStream out = new DataOutputStream(toServer);
            out.writeBytes("Connection:");

            toServer.close();
            output.close();
            out.close();
            soc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * A thread to search through a particular range of IP addresses. Using multiple threads will
 * allow for much faster searching and larger timeout allowances.
 */
class Sniffer implements Runnable {
    private String TAG = "SnifferThread:";
    private final int start;
    private final int end;
    private final String prefix;

    public Sniffer(String ID, String prefix, int start, int end) {
        this.TAG += ID;
        this.prefix = prefix;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        try {
            for (int i = start; i < end; i++) {
                String testIp = prefix + i;

                InetAddress address = InetAddress.getByName(testIp);
                boolean reachable = address.isReachable(100);
                String hostName = address.getCanonicalHostName();

                if (reachable) {
                    Log.d(TAG, "Host: " + hostName + "(" + testIp + ") is reachable!");
                    NetworkSniffer.PotentialIpAddresses.add(testIp);
                } else {
                    Log.d(TAG, "Host: " + hostName + "(" + testIp + ") is not reachable!");
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "Well that's not good.", t);
        } finally {
            Log.d(TAG, "Sniffing finished");
            Log.d(TAG, "Active threads: " + NetworkSniffer.snifferExecutor.getActiveCount());

            if(NetworkSniffer.snifferExecutor.getActiveCount() == 1) {
                NetworkSniffer.checkConnections();
            }
        }
    }
}
