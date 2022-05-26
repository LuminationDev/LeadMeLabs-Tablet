package com.lumination.leadmelabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import io.reactivex.rxjava3.core.*;

import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.logo.LogoViewModel;
import com.lumination.leadmelabs.ui.nuc.NucFragment;
import com.lumination.leadmelabs.ui.nuc.NucViewModel;
import com.lumination.leadmelabs.ui.pages.QuickStartPageFragment;
import com.lumination.leadmelabs.ui.sessionControls.SessionControlsFragment;
import com.lumination.leadmelabs.ui.sessionControls.SessionControlsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuViewModel;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuFragment;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuViewModel;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.ui.stations.SteamSelectionFragment;
import com.lumination.leadmelabs.ui.zones.ZonesFragment;
import com.lumination.leadmelabs.ui.zones.ZonesViewModel;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";

    public static Handler UIHandler;
    public static FragmentManager fragmentManager;

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


        hideStatusBar();
        startNetworkService();
        loadNuc();

        preloadViewModels();
        preloadData();

        if (savedInstanceState == null) {
            fragmentManager = getSupportFragmentManager();

            //Loading the home screen
            fragmentManager.beginTransaction()
                    .replace(R.id.main, QuickStartPageFragment.class, null)
                    .replace(R.id.side_menu, SideMenuFragment.class, null)
                    .commitNow();
        }

        //Example of RXJava
        //Flowable.just("Hello world").subscribe(System.out::println);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNetworkService();
    }

    /**
     * Populate all static ViewModels so that the information is available as soon as the
     * fragment is loaded.
     */
    private void preloadViewModels() {
        NucFragment.mViewModel = ViewModelProviders.of(this).get(NucViewModel.class);
        StationsFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        StationSingleFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        SteamSelectionFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        StationsFragment.mViewModel = ViewModelProviders.of(this).get(StationsViewModel.class);
        LogoFragment.mViewModel = ViewModelProviders.of(this).get(LogoViewModel.class);
        ApplianceFragment.mViewModel = ViewModelProviders.of(this).get(ApplianceViewModel.class);
        SessionControlsFragment.mViewModel = ViewModelProviders.of(this).get(SessionControlsViewModel.class);
        SubMenuFragment.mViewModel = ViewModelProviders.of(this).get(SubMenuViewModel.class);
        SideMenuFragment.mViewModel = ViewModelProviders.of(this).get(SideMenuViewModel.class);
        ZonesFragment.mViewModel = ViewModelProviders.of(this).get(ZonesViewModel.class);
    }

    /**
     * Preload the data necessary for the ViewModel operations.
     */
    private void preloadData() {
        NucFragment.mViewModel.getNuc();
        StationsFragment.mViewModel.getStations();
        ApplianceFragment.mViewModel.getAppliances();
        ApplianceFragment.mViewModel.getActiveAppliances();
        SessionControlsFragment.mViewModel.getInfo();
        SubMenuFragment.mViewModel.getSelectedPage();
        SideMenuFragment.mViewModel.getSelectedIcon();
        ZonesFragment.mViewModel.getZones();
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
}
