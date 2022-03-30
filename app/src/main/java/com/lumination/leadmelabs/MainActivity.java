package com.lumination.leadmelabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import io.reactivex.rxjava3.core.*;

import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.menu.SideMenuFragment;
import com.lumination.leadmelabs.ui.nuc.NucFragment;
import com.lumination.leadmelabs.ui.scenes.ScenesFragment;
import com.lumination.leadmelabs.ui.scenes.ScenesViewModel;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;

import org.json.JSONArray;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    public static Handler UIHandler;
    private FragmentManager fragmentManager;

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

        if (savedInstanceState == null) {
            fragmentManager = getSupportFragmentManager();
            loadFragments();
        }

        hideStatusBar();
        startNetworkService();

        //Example of RXJava
        Flowable.just("Hello world").subscribe(System.out::println);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNetworkService();
    }

    /**
     * Load in the initial fragments for the main view.
     */
    private void loadFragments() {
        fragmentManager.beginTransaction()
                .replace(R.id.side_menu, SideMenuFragment.newInstance())
                .commitNow();

        fragmentManager.beginTransaction()
                .replace(R.id.stations, StationsFragment.newInstance())
                .commitNow();

        fragmentManager.beginTransaction()
                .replace(R.id.nuc, NucFragment.newInstance())
                .commitNow();

        fragmentManager.beginTransaction()
                .replace(R.id.scenes, ScenesFragment.newInstance())
                .commitNow();

        fragmentManager.beginTransaction()
                .replace(R.id.logo, LogoFragment.newInstance())
                .commitNow();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(network_intent);
        } else {
            startService(network_intent);
        }
    }

    /**
     * Stop the network service.
     */
    private void stopNetworkService() {
        Intent stop_network_intent = new Intent(getApplicationContext(), NetworkService.class);
        stopService(stop_network_intent);
    }

    //Simplify the functions below to a generic one
    public static void updateStations(String jsonString) throws JSONException {
        JSONArray json = new JSONArray(jsonString);

        MainActivity.runOnUI(() -> {
            try {
                StationsFragment.mViewModel.setStations(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateScenes(String jsonString) throws JSONException {
        JSONArray json = new JSONArray(jsonString);

        MainActivity.runOnUI(() -> {
            try {
                ScenesFragment.mViewModel.setScenes(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }
}