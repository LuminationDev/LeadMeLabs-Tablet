package com.lumination.leadmelabs.ui.systemStatus;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.receivers.BatteryLevelReceiver;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.segment.analytics.Properties;

import java.util.Date;

public class SystemStatusFragment extends Fragment {
    private final Handler handler = new Handler();
    private Runnable runnable;
    private final String preferenceKey = "network_outage";

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

    //region Battery Status
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
    //endregion


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
                        cancelDelayedFunction();
                        MainActivity.getInstance().restartNetworkService();
                        ((TextView) view.findViewById(R.id.network_connection)).setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_network_connected, 0, 0);
                    });
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                startDelayedFunction();
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
            if (connected) {
                cancelDelayedFunction();
            } else {
                startDelayedFunction();
            }
            MainActivity.runOnUI(() -> {
                int drawable = connected ? R.drawable.ic_network_connected : R.drawable.ic_network_not_connected;
                ((TextView) view.findViewById(R.id.network_connection)).setCompoundDrawablesWithIntrinsicBounds(0, drawable, 0, 0);
            });
        }
    }

    /**
     * If the tablet loses internet connection start a timer, after 10 seconds display a lost internet
     * connection message.
     */
    public void startDelayedFunction() {
        runnable = () -> {
            if (MainActivity.getInstance() == null) return;

            //Create a saved data point to upload to segment once the application regains internet (share preferences perhaps)
            SharedPreferences sharedPreferences = MainActivity.getInstance().getApplication().getSharedPreferences(preferenceKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(preferenceKey, String.valueOf(new Date()));
            editor.apply();

            DialogManager.createBasicTrackedDialog(SegmentConstants.Network_Outage,
                    MainActivity.getInstance().getResources().getString(R.string.oh_no),
                    "The tablet has lost internet connection! Please check the WiFi as the tablet can no longer communicate with the Lab."
            );
        };

        // Post the runnable with a delay of 10 seconds
        handler.postDelayed(runnable, 10000); // 10000 milliseconds = 10 seconds
    }

    /**
     * Cancel the delay internet message runnable.
     */
    public void cancelDelayedFunction() {
        //Close the internet dialog if it is open
        DialogManager.closeOpenDialog(SegmentConstants.Network_Outage,
                MainActivity.getInstance().getResources().getString(R.string.oh_no));

        //Lodge any previous outages
        checkForOutages();

        // Remove the runnable from the handler if it's not null
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    /**
     * On connection to a network check the shared preferences to see if there were any outages
     * since the last connection.
     */
    public void checkForOutages() {
        if (MainActivity.getInstance() == null) return;

        SharedPreferences sharedPreferences = MainActivity.getInstance().getApplication().getSharedPreferences(preferenceKey, Context.MODE_PRIVATE);
        String outage = sharedPreferences.getString(preferenceKey, null);

        if (outage == null) return;

        //Remove the outage entry
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(preferenceKey);
        editor.apply();

        //Upload to segment
        Properties properties = new Properties();
        properties.put("classification", "Network");
        properties.put("timeStamp", outage);
        Segment.trackEvent(SegmentConstants.Network_Outage, properties);
    }
}
