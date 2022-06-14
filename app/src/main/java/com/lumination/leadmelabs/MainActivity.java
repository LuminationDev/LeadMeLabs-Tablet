package com.lumination.leadmelabs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.logo.LogoViewModel;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.room.RoomViewModel;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sessionControls.SessionControlsFragment;
import com.lumination.leadmelabs.ui.sessionControls.SessionControlsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuViewModel;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuFragment;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuViewModel;
import com.lumination.leadmelabs.ui.stations.StationSelectionFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.ui.stations.SteamSelectionFragment;
import com.lumination.leadmelabs.ui.zones.ZonesFragment;
import com.lumination.leadmelabs.ui.zones.ZonesViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";

    public static MainActivity instance;
    public static MainActivity getInstance() { return instance; }

    public static Handler UIHandler;
    public static FragmentManager fragmentManager;
    public static MutableLiveData<Integer> fragmentCount;

    public static androidx.appcompat.app.AlertDialog gameLaunchDialog;
    public static List<Integer> gameLaunchStationIds;

    public static int hasNotReceivedPing = 0;

    public static Handler handler;

    public static androidx.appcompat.app.AlertDialog reconnectDialog;

    static { UIHandler = new Handler(Looper.getMainLooper()); }

    /**
     * Allows runOnUIThread calls from anywhere in the program.
     */
    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

    public static void createBasicDialog(String titleText, String contentText) {
        MainActivity.runOnUI(() -> {
            View basicDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_template, null);
            Button confirmButton = basicDialogView.findViewById(R.id.confirm_button);
            Button cancelButton = basicDialogView.findViewById(R.id.cancel_button);
            ProgressBar loadingBar = basicDialogView.findViewById(R.id.loading_bar);
            TextView title = basicDialogView.findViewById(R.id.title);
            TextView contentView = basicDialogView.findViewById(R.id.content_text);
            title.setText(titleText);
            contentView.setText(contentText);
            AlertDialog launchFailedDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.getInstance()).setView(basicDialogView).create();
            confirmButton.setOnClickListener(w -> launchFailedDialog.dismiss());
            cancelButton.setVisibility(View.GONE);
            loadingBar.setVisibility(View.GONE);
            confirmButton.setText("Dismiss");
            launchFailedDialog.show();
            launchFailedDialog.getWindow().setLayout(1200, 320);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        instance = this;

        hideStatusBar();
        startNetworkService();
        loadNuc();

        preloadViewModels();
        preloadData();

        if (savedInstanceState == null) {
            setupFragmentManager();
        }

        startNucPingMonitor();
    }

    public static void startNucPingMonitor() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
            public void run() {
                hasNotReceivedPing += 1;
                if (hasNotReceivedPing > 2) {
                    Log.e("MainActivity", "NUC Lost");

                    View reconnectDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_reconnect, null);
                    Button reconnectButton = reconnectDialogView.findViewById(R.id.reconnect_button);
                    Button closeReconnectDialogButton = reconnectDialogView.findViewById(R.id.close_reconnect_dialog);
                    reconnectDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.getInstance()).setView(reconnectDialogView).create();
                    reconnectDialog.setCancelable(false);
                    reconnectDialog.setCanceledOnTouchOutside(false);
                    reconnectDialogView.findViewById(R.id.reconnect_failed).setVisibility(View.GONE);
                    reconnectButton.setOnClickListener(w -> {
                        reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.VISIBLE);
                        reconnectDialogView.findViewById(R.id.reconnect_failed).setVisibility(View.GONE);
                        NetworkService.broadcast("Android:Reconnect");
                        new java.util.Timer().schedule( // turn animations back on after the scenes have updated
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        MainActivity.runOnUI(new Runnable() {
                                            @Override
                                            public void run() {
                                                reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
                                                reconnectDialogView.findViewById(R.id.reconnect_failed).setVisibility(View.VISIBLE);
                                            }
                                        });
                                    }
                                },
                                10000
                        );
                    });
                    closeReconnectDialogButton.setOnClickListener(w -> {
                        reconnectDialogView.findViewById(R.id.reconnect_failed).setVisibility(View.GONE);
                        reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
                        reconnectDialog.dismiss();
                    });
                    reconnectDialog.show();
                    reconnectDialog.getWindow().setLayout(1200, 450);
                } else {
                    handler.postDelayed(this, 5000);
                }
            }
        }, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNetworkService();
    }

    /**
     * Instantiate the fragment manager for the activity and the livedata counter used for the
     * back button data binding. Finish by loading the initial fragment.
     */
    private void setupFragmentManager() {
        fragmentManager = getSupportFragmentManager();

        fragmentCount = new MutableLiveData<>(0);
        fragmentManager.addOnBackStackChangedListener(() ->
                fragmentCount.setValue(fragmentManager.getBackStackEntryCount())
        );

        //Load the side menu as a separate transaction as this is not kept on the back stack.
        fragmentManager.beginTransaction()
                .replace(R.id.side_menu, SideMenuFragment.class, null)
                .commitNow();

        //Loading the home screen
        if (ViewModelProviders.of(this).get(SettingsViewModel.class).getHideStationControls().getValue()) {
            fragmentManager.beginTransaction()
                    .replace(R.id.main, ControlPageFragment.class, null)
                    .addToBackStack("menu:controls")
                    .commit();
            fragmentManager.beginTransaction()
                    .replace(R.id.sub_menu, SubMenuFragment.class, null, "sub")
                    .commitNow();
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.main, DashboardPageFragment.class, null)
                    .addToBackStack("menu:dashboard")
                    .commit();
        }
    }

    /**
     * Populate all static ViewModels so that the information is available as soon as the
     * fragment is loaded.
     */
    private void preloadViewModels() {
        RoomFragment.mViewModel = ViewModelProviders.of(this).get(RoomViewModel.class);
        SettingsFragment.mViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        StationsFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        StationSelectionFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        StationSingleFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        SteamSelectionFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        StationsFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        LogoFragment.mViewModel = ViewModelProviders.of(this).get(LogoViewModel.class);
        ApplianceFragment.mViewModel = ViewModelProviders.of(this).get(ApplianceViewModel.class);
        SessionControlsFragment.mViewModel = ViewModelProviders.of(this).get(SessionControlsViewModel.class);
        SubMenuFragment.mViewModel = ViewModelProviders.of(this).get(SubMenuViewModel.class);
        SideMenuFragment.mViewModel = ViewModelProviders.of(this).get(SideMenuViewModel.class);
        //ZonesFragment.mViewModel = ViewModelProviders.of(this).get(ZonesViewModel.class);
    }

    /**
     * Preload the data necessary for the ViewModel operations.
     */
    private void preloadData() {
        SettingsFragment.mViewModel.getNuc();
        StationsFragment.mViewModel.getStations();
        ApplianceFragment.mViewModel.getAppliances();
        ApplianceFragment.mViewModel.getActiveAppliances();
        SessionControlsFragment.mViewModel.getInfo();
        SubMenuFragment.mViewModel.getSelectedPage();
        SideMenuFragment.mViewModel.getSelectedIcon();
        SettingsFragment.mViewModel.getHideStationControls();
        //ZonesFragment.mViewModel.getZones();
    }

    /**
     * Hide the navigation buttons giving the application a fullscreen look
     */
    private void hideStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    /**
     * Start the network service.
     */
    private void startNetworkService() {
        Log.d(TAG, "startService: ");

        Intent network_intent = new Intent(getApplicationContext(), NetworkService.class);
        startForegroundService(network_intent);
    }

    /**
     * Stop the network service.
     */
    private void stopNetworkService() {
        Intent stop_network_intent = new Intent(getApplicationContext(), NetworkService.class);
        stopService(stop_network_intent);
    }

    /**
     * Set the NUC address for the network service. Loading in the main activity so the initial
     * network messages can be sent straight away for station/scene list and initial scene value.
     */
    private void loadNuc() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("nuc_address", Context.MODE_PRIVATE);
        String address = sharedPreferences.getString("nuc_address", "");

        if(!address.equals("")) {
            NetworkService.setNUCAddress(address);
        }
    }

    /**
     * Change the background of the selected view while a user is touching it.
     * @param view A view which has an OnTouchListener added.
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void feedback(View view) {
        view.setOnTouchListener((v, event) -> {
            switch(event.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setBackgroundResource(0);
                    break;

                case MotionEvent.ACTION_DOWN:
                    v.setBackground(ResourcesCompat.getDrawable(
                            MainActivity.getInstance().getResources(),
                            R.drawable.icon_touch_event,
                            null)
                    );
                    break;
            }

           return false;
       });
    }

    public static void awaitStationGameLaunch(int[] stationIds, String gameName)
    {
        View gameLaunchDialogView = View.inflate(instance, R.layout.dialog_template, null);
        Button confirmButton = gameLaunchDialogView.findViewById(R.id.confirm_button);
        Button cancelButton = gameLaunchDialogView.findViewById(R.id.cancel_button);
        TextView title = gameLaunchDialogView.findViewById(R.id.title);
        TextView contentText = gameLaunchDialogView.findViewById(R.id.content_text);
        title.setText("Launching Game");
        contentText.setText("Launching " + gameName + " on " + String.join(", ", StationsFragment.mViewModel.getStationNames(stationIds)));
        gameLaunchDialog = new androidx.appcompat.app.AlertDialog.Builder(instance).setView(gameLaunchDialogView).create();
        gameLaunchStationIds =  new ArrayList<Integer>(stationIds.length);
        for (int i : stationIds)
        {
            gameLaunchStationIds.add(i);
        }
        confirmButton.setOnClickListener(w -> gameLaunchDialog.dismiss());
        cancelButton.setVisibility(View.GONE);
        confirmButton.setText("Dismiss");
        gameLaunchDialog.show();
        gameLaunchDialog.getWindow().setLayout(1200, 380);
    }

    public static void gameLaunchedOnStation(int stationId) {
        if (gameLaunchStationIds != null) {
            gameLaunchStationIds.removeIf(id -> id == stationId);
            if (gameLaunchStationIds.size() == 0) {
                if (gameLaunchDialog != null) {
                    gameLaunchDialog.dismiss();
                }
            }
        }
    }
}
