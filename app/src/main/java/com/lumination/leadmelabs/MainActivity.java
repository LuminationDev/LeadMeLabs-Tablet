package com.lumination.leadmelabs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

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
import com.lumination.leadmelabs.ui.pages.DashboardFragment;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

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

        if (savedInstanceState == null) {
            fragmentManager = getSupportFragmentManager();

            //Loading the home screen
            fragmentManager.beginTransaction()
                    .replace(R.id.main, DashboardFragment.class, null)
                    .commitNow();
        }

        hideStatusBar();
        startNetworkService();
        loadNuc();

        //Example of RXJava
        Flowable.just("Hello world").subscribe(System.out::println);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNetworkService();
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
