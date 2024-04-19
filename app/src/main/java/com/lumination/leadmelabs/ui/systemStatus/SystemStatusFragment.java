package com.lumination.leadmelabs.ui.systemStatus;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.receivers.BatteryLevelReceiver;

public class SystemStatusFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_system_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupBatteryMonitor(view);
        setupNetworkMonitor(view);
    }

    /**
     * Sets up a battery monitor to display battery level updates in a TextView.
     *
     * @param view The view containing the TextView for displaying battery level.
     */
    private void setupBatteryMonitor(View view) {
        Context context = getContext();
        if (context == null) return;

        BatteryLevelReceiver batteryLevelReceiver = new BatteryLevelReceiver();
        batteryLevelReceiver.setBatteryTextView(view.findViewById(R.id.battery_level), view.findViewById(R.id.charge_icon));
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        context.registerReceiver(batteryLevelReceiver, filter);
    }

    /**
     * Sets up a network monitor to track network availability and update UI accordingly.
     *
     * @param view The view containing the TextView for displaying network connection status.
     */
    private void setupNetworkMonitor(View view) {
        Context context = getContext();
        if (context == null) return;

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (MainActivity.getInstance() != null) {
                    MainActivity.runOnUI(() -> {
                        MainActivity.getInstance().restartNetworkService();
                        ((TextView) view.findViewById(R.id.network_connection)).setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_network_connected, 0, 0);
                    });
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                /*
                setTimeout 10 seconds
                if still lost
                I have lost internet connection, please try restarting the tablet

                if we regain connection (above), automatically dismiss the popup
                 */

                if (MainActivity.getInstance() != null) {
                    MainActivity.runOnUI(() -> {
                        ((TextView) view.findViewById(R.id.network_connection)).setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_network_not_connected, 0, 0);
                    });
                }
            }
        };

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.requestNetwork(networkRequest, networkCallback);

        NetworkCapabilities networkCapabilities = null;
        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                break;
            }
        }

        boolean connected = networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        if (MainActivity.getInstance() != null) {
            MainActivity.runOnUI(() -> {
                int drawable = connected ? R.drawable.ic_network_connected : R.drawable.ic_network_not_connected;
                ((TextView) view.findViewById(R.id.network_connection)).setCompoundDrawablesWithIntrinsicBounds(0, drawable, 0, 0);
            });
        }
    }
}
