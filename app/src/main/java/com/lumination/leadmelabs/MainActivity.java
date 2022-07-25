package com.lumination.leadmelabs;

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

import com.google.firebase.analytics.FirebaseAnalytics;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.services.jobServices.LicenseJobService;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.services.jobServices.RefreshJobService;
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
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.ui.stations.SteamSelectionFragment;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";

    public static MainActivity instance;
    public static MainActivity getInstance() { return instance; }

    public static Handler UIHandler;
    public static FragmentManager fragmentManager;
    public static MutableLiveData<Integer> fragmentCount;
    public static int hasNotReceivedPing = 0;

    public static Handler handler;

    static { UIHandler = new Handler(Looper.getMainLooper()); }

    /**
     * Allows runOnUIThread calls from anywhere in the program.
     */
    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
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

        FirebaseManager.validateLicenseKey();
        scheduleJobs();
        FirebaseManager.setupFirebaseManager(FirebaseAnalytics.getInstance(this));

        if (savedInstanceState == null) {
            setupFragmentManager();
        }

        startNucPingMonitor();

        DialogManager.createBasicDialog(
                "Oh no!",
                "Station 1's headset has disconnected. Please check the battery is charged."
        );
    }

    /**
     * Start any long running jobs.
     */
    private void scheduleJobs() {
        LicenseJobService.schedule(this);
        RefreshJobService.schedule(this);
    }

    public static void startNucPingMonitor() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
            public void run() {
                hasNotReceivedPing += 1;
                if (hasNotReceivedPing > 3) {
                    Log.e("MainActivity", "NUC Lost");
                    DialogManager.buildReconnectDialog();
                } else {
                    handler.postDelayed(this, 500000);
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

    //TODO APPLIANCES DO NOT UPDATE IF A USER HAS NOT CLICKED ON ROOM CONTROLS TO START WITH
    /**
     * Populate all static ViewModels so that the information is available as soon as the
     * fragment is loaded.
     */
    private void preloadViewModels() {
        RoomFragment.mViewModel = ViewModelProviders.of(this).get(RoomViewModel.class);
        SettingsFragment.mViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        StationsFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        StationSelectionPageFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        StationSingleFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        SteamSelectionFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        StationsFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        LogoFragment.mViewModel = ViewModelProviders.of(this).get(LogoViewModel.class);
        ApplianceFragment.mViewModel = ViewModelProviders.of(this).get(ApplianceViewModel.class);
        SessionControlsFragment.mViewModel = ViewModelProviders.of(this).get(SessionControlsViewModel.class);
        SubMenuFragment.mViewModel = ViewModelProviders.of(this).get(SubMenuViewModel.class);
        SideMenuFragment.mViewModel = ViewModelProviders.of(this).get(SideMenuViewModel.class);
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


    /**
     * On awake check the Cbus to see if any values are not the correct ones.
     */
    @Override
    protected void onResume() {
        super.onResume();
        ApplianceFragment.mViewModel.getActiveAppliances();
    }
}
