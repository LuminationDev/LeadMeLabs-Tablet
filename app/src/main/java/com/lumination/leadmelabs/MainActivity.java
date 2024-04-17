package com.lumination.leadmelabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.services.jobServices.UpdateJobService;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.library.LibraryViewModel;
import com.lumination.leadmelabs.ui.library.application.ApplicationLibraryFragment;
import com.lumination.leadmelabs.ui.library.video.VideoLibraryFragment;
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
import com.lumination.leadmelabs.ui.systemStatus.SystemStatusFragment;
import com.lumination.leadmelabs.unique.snowHydro.StationSingleNestedFragment;
import com.lumination.leadmelabs.unique.snowHydro.modal.ModalDialogFragment;
import com.lumination.leadmelabs.utilities.Helpers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";

    public static MainActivity instance;
    public static MainActivity getInstance() { return instance; }
    public static boolean isActivityRunning;
    public static Handler UIHandler;
    public static FragmentManager fragmentManager;
    public static MutableLiveData<Integer> fragmentCount;
    public static int hasNotReceivedPing = 0;
    public static boolean attemptedRefresh = false;
    public static boolean reconnectionIgnored = false;
    public static boolean isAppInForeground = false;

    public static boolean isNucUtf8 = true;
    public static boolean isNucJsonEnabled = false;

    static ScheduledExecutorService scheduler;

    public static Handler handler;
    static { UIHandler = new Handler(Looper.getMainLooper()); }

    public static AppUpdateManager appUpdateManager;

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
        isAppInForeground = true;
        isActivityRunning = true;
        instance = this;
        appUpdateManager = AppUpdateManagerFactory.create(MainActivity.getInstance().getApplicationContext());

        hideStatusBar();

        startNetworkService();
        loadNuc();

        FirebaseManager.setupFirebaseManager(FirebaseAnalytics.getInstance(this));

        preloadViewModels();
        preloadData();
        Segment.initialise(); //This needs to be after preloading the view models

        FirebaseManager.validateLicenseKey();
        scheduleJobs();

        if (savedInstanceState == null) {
            setupFragmentManager();
        }

        startNucPingMonitor();
        startLockTask();

        FirebaseManager.reportTrafficFlags();
    }

    /**
     * Start any long running jobs.
     */
    private void scheduleJobs() {
//        LicenseJobService.schedule(this);
        UpdateJobService.schedule(this);
    }

    public static void startNucPingMonitor() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
            public void run() {
                //The prompt has been ignored, try again in 10 minutes
                if(reconnectionIgnored) {
                    Log.e("MainActivity", "Reconnection Ignored");
                    handler.postDelayed(this, 10 * 60000);
                    reconnectionIgnored = false;
                    return;
                }

                hasNotReceivedPing += 1;
                if (hasNotReceivedPing > 3 && attemptedRefresh) {
                    Log.e("MainActivity", "NUC Lost");
                    if(DialogManager.reconnectDialog == null || !DialogManager.reconnectDialog.isShowing()) {
                        DialogManager.buildReconnectDialog();
                    }

                    //Wait 10 minutes and then try to reconnect every 10 minutes - this will stop
                    //the popup occurring over night when the NUC restarts. The scheduler will be
                    //shutdown upon a successful reconnection.
                    if(scheduler != null) {
                        if(!scheduler.isShutdown()) {
                            scheduler.shutdownNow();
                        }
                    }

                    scheduler = Executors.newSingleThreadScheduledExecutor();
                    scheduler.scheduleWithFixedDelay(() -> {
                        if(NetworkService.getNUCAddress() != null) {
                            NetworkService.refreshNUCAddress();
                        }
                    }, 10, 10, TimeUnit.MINUTES);
                } else if (hasNotReceivedPing > 3) {
                     /*
                    see if we can run a refresh before we show the dialog, in case the nuc has
                    shutdown, means we don't have to show a disruptive dialog
                     */
                    if(NetworkService.getNUCAddress() != null) {
                        NetworkService.refreshNUCAddress();
                    }
                    attemptedRefresh = true;
                    handler.postDelayed(this, 5000);
                    if(scheduler == null) return;
                    if(scheduler.isShutdown()) return;
                    scheduler.shutdownNow();
                } else {
                    handler.postDelayed(this, 5000);
                    if(scheduler == null) return;
                    if(scheduler.isShutdown()) return;
                    scheduler.shutdownNow();
                }
            }
        }, 5000);
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

        Bundle args = new Bundle();

        //Loading the home screen
        if (Boolean.TRUE.equals(ViewModelProviders.of(this).get(SettingsViewModel.class).getHideStationControls().getValue())) {
            args.putString("menuSize", "mini");
            fragmentManager.beginTransaction()
                    .replace(R.id.main, ControlPageFragment.class, null)
                    .addToBackStack("menu:controls")
                    .commit();
            fragmentManager.beginTransaction()
                    .replace(R.id.sub_menu, SubMenuFragment.class, null, "sub")
                    .commitNow();
            Segment.trackScreen("menu:controls");
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.main, DashboardPageFragment.class, null)
                    .addToBackStack("menu:dashboard")
                    .commit();
            Segment.trackScreen("menu:dashboard");
        }

        //Load the side menu as a separate transaction as this is not kept on the back stack.
        fragmentManager.beginTransaction()
                .replace(R.id.side_menu, SideMenuFragment.class, args)
                .replace(R.id.system_status, SystemStatusFragment.class, null)
                .commitNow();
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
        StationSingleNestedFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        ModalDialogFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        LibrarySelectionFragment.mViewModel = ViewModelProviders.of(this).get(LibraryViewModel.class);
        ApplicationLibraryFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        VideoLibraryFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
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
        SettingsFragment.mViewModel.getNucAddress();
        SettingsFragment.mViewModel.getLabLocation();
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
     * Checks if the NetworkService is currently running.
     *
     * @return true if the NetworkService is running, false otherwise.
     */
    private boolean isNetworkServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
                if (processInfo.processName.equals(NetworkService.class.getName())) {
                    return true;
                }
            }
        }
        return false;
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
     * Start the network service.
     */
    public void restartNetworkService() {
        if (!isNetworkServiceRunning() || !isAppInForeground) {
            return;
        }
        Log.d(TAG, "restartNetworkService: ");

        stopNetworkService();

        Intent network_intent = new Intent(getApplicationContext(), NetworkService.class);
        startForegroundService(network_intent);
        NetworkService.refreshNUCAddress();
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
     * Collect the current version number of the project.
     * @return A string of the version number in the format X.XX.
     */
    public String getSoftwareVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityRunning = false;
        stopNetworkService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isAppInForeground = false;
    }

    /**
     * On awake check the Cbus to see if any values are not the correct ones and if a download has
     * been initiated.
     */
    @Override
    protected void onResume() {
        super.onResume();
        isAppInForeground = true;

        // Prompt the user to pin the application
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if(am.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_PINNED) {
            startLockTask();
        }

        // Check if the nuc address is set, if not do not attempt to contact the NUC
        if (NetworkService.getNUCAddress() == null || Helpers.isNullOrEmpty(NetworkService.getNUCAddress())) return;

        // If the tablet has lost connection, attempt to re-connect, otherwise collect the active appliances
        if (hasNotReceivedPing > 3) {
            NetworkService.refreshNUCAddress();
        } else {
            ApplianceFragment.mViewModel.getActiveAppliances();
        }
        SettingsFragment.mViewModel.resetSupportModeIfRequired();
    }
}
