package com.lumination.leadmelabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.services.jobServices.RefreshJobService;
import com.lumination.leadmelabs.services.jobServices.UpdateJobService;
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
import com.lumination.leadmelabs.utilities.Constants;

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

    public static AppUpdateManager appUpdateManager;
    private InstallStateUpdatedListener installStateUpdatedListener;

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
        updateSetup();

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
        startLockTask();
    }

    private void updateSetup() {
        appUpdateManager = AppUpdateManagerFactory.create(this);
        installStateUpdatedListener = state -> {
            switch(state.installStatus()) {
                case InstallStatus.PENDING:
                    Toast.makeText(getApplicationContext(), "Download pending..", Toast.LENGTH_SHORT).show();
                    break;

                case InstallStatus.DOWNLOADING:
                    Toast.makeText(getApplicationContext(), "Downloading..", Toast.LENGTH_SHORT).show();
                    break;

                case InstallStatus.DOWNLOADED:
                    popupSnackBarForCompleteUpdate();
                    break;

                case InstallStatus.INSTALLED:
                    removeInstallStateUpdateListener();
                    break;

                case InstallStatus.FAILED:
                    Toast.makeText(getApplicationContext(), "Error: Download failed", Toast.LENGTH_LONG).show();
                    break;

                case InstallStatus.CANCELED:
                    Toast.makeText(getApplicationContext(), "Error: Download cancelled", Toast.LENGTH_LONG).show();
                    break;

                case InstallStatus.INSTALLING:
                case InstallStatus.UNKNOWN:
                case InstallStatus.REQUIRES_UI_INTENT:
                default:
                    Log.d("Update", "Other state: " + state.installStatus());
                    break;
            }
        };
        appUpdateManager.registerListener(installStateUpdatedListener);
    }

    private void removeInstallStateUpdateListener() {
        if (appUpdateManager != null) {
            appUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }

    /**
     * Start any long running jobs.
     */
    private void scheduleJobs() {
//        LicenseJobService.schedule(this);
        RefreshJobService.schedule(this);
//        UpdateJobService.schedule(this);
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
                    handler.postDelayed(this, 5000);
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
        if (ViewModelProviders.of(this).get(SettingsViewModel.class).getHideStationControls().getValue()) {
            fragmentManager.beginTransaction()
                    .replace(R.id.main, ControlPageFragment.class, null)
                    .addToBackStack("menu:controls")
                    .commit();
            fragmentManager.beginTransaction()
                    .replace(R.id.sub_menu, SubMenuFragment.class, null, "sub")
                    .commitNow();

            args.putString("menuSize", "mini");
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.main, DashboardPageFragment.class, null)
                    .addToBackStack("menu:dashboard")
                    .commit();
        }

        //Load the side menu as a separate transaction as this is not kept on the back stack.
        fragmentManager.beginTransaction()
                .replace(R.id.side_menu, SideMenuFragment.class, args)
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

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
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
        if (!isServiceRunning(NetworkService.class)) {
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
     * Present the user with an option to restart the application
     */
    public static void popupSnackBarForCompleteUpdate() {
        Snackbar.make(MainActivity.getInstance().findViewById(android.R.id.content).getRootView(), "Update is ready to install!", Snackbar.LENGTH_INDEFINITE)
                .setAction("RESTART", view -> {
                    MainActivity.UIHandler.post(() -> MainActivity.getInstance().stopLockTask());
                    if (appUpdateManager != null) {
                        appUpdateManager.completeUpdate();
                    }
                })
                .setActionTextColor(ResourcesCompat.getColor(MainActivity.getInstance().getResources(), R.color.orange, null))
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.UPDATE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this,"Update success!", Toast.LENGTH_LONG).show();
                // If the update succeeds, relaunch the application
            }

            if  (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Update cancelled, auto retry in 3 hours.", Toast.LENGTH_LONG).show();
                UIHandler.postDelayed(this::startLockTask, 5000);

            }

            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Update failed, auto retry in 3 hours.", Toast.LENGTH_LONG).show();
                // If the update is cancelled or fails,
                // you can request to start the update again.
                UIHandler.postDelayed(this::startLockTask, 5000);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNetworkService();
    }

    /**
     * On awake check the Cbus to see if any values are not the correct ones and if a download has
     * been initiated.
     */
    @Override
    protected void onResume() {
        super.onResume();
        ApplianceFragment.mViewModel.getActiveAppliances();

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if(am.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_PINNED) {
            startLockTask();
        }

        if(appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(MainActivity.getInstance().getApplicationContext());
        }

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(
            appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability()
                        == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // If an in-app update is already running, resume the update.
                    UpdateJobService.runUpdate(appUpdateInfo);
                }
            }
        );
    }
}
