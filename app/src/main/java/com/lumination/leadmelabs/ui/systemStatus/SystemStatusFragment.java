package com.lumination.leadmelabs.ui.systemStatus;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
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

    private BatteryLevelReceiver batteryLevelReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_system_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        batteryLevelReceiver = new BatteryLevelReceiver();
        batteryLevelReceiver.setBatteryTextView(view.findViewById(R.id.battery_level));
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        getContext().registerReceiver(batteryLevelReceiver, filter);


        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        ((MainActivity) getActivity()).restartNetworkService();
                        ((TextView) view.findViewById(R.id.network_connection)).setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_network_connected, 0, 0);
                    });
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        ((TextView) view.findViewById(R.id.network_connection)).setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_network_not_connected, 0, 0);
                    });
                }
            }
        };

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getContext().getSystemService(ConnectivityManager.class);
        connectivityManager.requestNetwork(networkRequest, networkCallback);
        NetworkInfo nInfo = connectivityManager.getActiveNetworkInfo();
        boolean connected = nInfo != null && nInfo.isConnected();
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                ((TextView) view.findViewById(R.id.network_connection)).setCompoundDrawablesWithIntrinsicBounds(0, connected ? R.drawable.ic_network_connected : R.drawable.ic_network_not_connected, 0, 0);
            });
        }
    }
}