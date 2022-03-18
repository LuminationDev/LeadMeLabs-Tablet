package com.lumination.leadmelab.utilities;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class BroadcastReceiver implements Runnable {
    private final String TAG = "BroadcastReceiver";
    private final int port = 3000;

    public BroadcastReceiver() {

    }

    @Override
    public void run() {
        Log.d(TAG, "Starting receiver");
        try {
            // Create a socket to listen on the port.
            DatagramSocket dsocket = new DatagramSocket(port);
            dsocket.setBroadcast(true);

            // Create a buffer to read datagrams into. If a
            // packet is larger than this buffer, the
            // excess will simply be discarded!
            byte[] buffer = new byte[1024];

            // Create a packet to receive data into the buffer
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Now loop forever, waiting to receive packets and printing them.
            while (true) {
                // Wait to receive a datagram
                dsocket.receive(packet);

                // Convert the contents to a string, and display them
                String msg = new String(buffer, 0, packet.getLength());
                Log.d(TAG, packet.getAddress().getHostName() + ": " + msg);

                // Reset the length of the packet before reusing it.
                packet.setLength(buffer.length);

                String res = msg;

                Log.d(TAG, res);
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
