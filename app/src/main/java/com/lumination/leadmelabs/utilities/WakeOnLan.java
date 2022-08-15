package com.lumination.leadmelabs.utilities;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class WakeOnLan {
    /**
     * Turn on all computers in the currently selected room with the Wake On Lan function that
     * extends from the NUC.
     */
    public static void WakeAll() {
        int[] stationIds = StationsFragment.getInstance().getRoomStations().stream().mapToInt(station -> station.id).toArray();
        String stationIdsString = String.join(", ", Arrays.stream(stationIds).mapToObj(String::valueOf).toArray(String[]::new));
        NetworkService.sendMessage("NUC," + stationIdsString,
                "WOL",
                "Startup" + ":"
                        + "computer" + ":"
                        + NetworkService.getIPAddress());

        //Change all stations to turning on status
        for (int stationId: stationIds ) {
            ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).syncStationStatus(String.valueOf(stationId), "2", "selfUpdate");
        }
    }

    /**
     * Send a Wake On Lan command from the Android Tablet aimed at the NUC so it can be remotely
     * turned on.
     */
    public static void WakeNUCOnLan() {
        String mac = "7c-10-c9-40-18-98"; //TODO Load from saved Memory in the future

        Log.d("JavaWakeOnLan", "method started");
        if (mac == null) {
            Log.d("Mac error at wakeup", "mac = null");
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                String broadcastIP = getBroadcastAddress();
                System.out.println(broadcastIP);

                byte[] macBytes = getMacBytes(mac);
                byte[] bytes = new byte[6 + 16 * macBytes.length];
                for (int i = 0; i < 6; i++) {
                    bytes[i] = (byte) 0xff;
                }
                for (int i = 6; i < bytes.length; i += macBytes.length) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
                }

                InetAddress address = InetAddress.getByName(broadcastIP);
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 9);
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.send(packet);
                }

                System.out.println("Wake-on-LAN packet sent.");
            }
            catch (Exception e) {
                System.out.println("Failed to send Wake-on-LAN packet: " + e);
                System.exit(1);
            }
        });

        thread.start();
    }

    /**
     * Get the current broadcast address for the Wifi Network.
     * @return A string representing the broadcast IP address.
     * @throws IOException Throws if the address cannot be accessed.
     */
    private static String getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) MainActivity.getInstance().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads).getHostAddress();
    }

    /**
     * Turn a supplied mac address into a byte array.
     * @param macAddress A string of HEX a decimal values separated by either ':' or '-'.
     * @return A byte array.
     * @throws IllegalArgumentException Throws if an invalid MAC address is supplied.
     */
    private static byte[] getMacBytes(String macAddress) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macAddress.split("([:|-])");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }
}
