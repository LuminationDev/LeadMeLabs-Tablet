package com.lumination.leadmelab.managers;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.lumination.leadmelab.MainActivity;
import com.lumination.leadmelab.R;

/**
 * Class to manager all the startup assignments for buttons, views, and text inputs that are
 * secondary to the main screen.
 */
public class StartupManager {
    private final String TAG = "Startup";

    private final MainActivity main;

    private final View stationScreen, applicationScreen, roomScreen;

    public StartupManager(MainActivity main) {
        this.main = main;
        this.stationScreen = main.stationScreen;
        this.applicationScreen = main.applicationScreen;
        this.roomScreen = main.roomScreen;

        setupActionButtons();
        setupVRSessionButtons();
        setupApplicationButtons();

        setupRoomButtons();
    }

    /**
     * Find the buttons on the l__station_screen layout and assign the on click functions to each.
     */
    private void setupActionButtons() {
        Button launchBtn = stationScreen.findViewById(R.id.launch_core_btn);
        launchBtn.setOnClickListener(view -> {
            if (checkSelection()) {
                StationManager.executeStationCommand(StationManager.LAUNCH + ApplicationManager.selected.getID());
            }
        });

        Button manageBtn = stationScreen.findViewById(R.id.manage_core_btn);
        manageBtn.setOnClickListener(view -> {
            main.changeScreen(MainActivity.ANIM_APPLICATION_INDEX);
        });

//        Button quitBtn = stationScreen.findViewById(R.id.quit_core_btn);
//        quitBtn.setOnClickListener(view -> {
//            if (checkSelection()) {
//                StationManager.executeCommand(StationManager.STOP + ApplicationManager.selected.getID());
//            }
//        });

        Button urlBtn = stationScreen.findViewById(R.id.url_core_btn);
        urlBtn.setOnClickListener(view -> {
            StationManager.executeStationCommand(StationManager.URL);
        });

        Button expBtn = stationScreen.findViewById(R.id.explorer_core_btn);
        expBtn.setOnClickListener(view -> {
            StationManager.executeStationCommand(StationManager.EXPLORER);
        });

        ImageView backBtn = stationScreen.findViewById(R.id.leadme_icon);
        backBtn.setOnClickListener(view -> main.changeScreen(MainActivity.ANIM_STATION_MANAGER_INDEX));
    }

    private void setupVRSessionButtons() {
        LinearLayout startVRBtn = stationScreen.findViewById(R.id.start_vr_btn);
        startVRBtn.setOnClickListener(view -> {
            StationManager.executeStationCommand(StationManager.START_VR);
        });

        LinearLayout restartVRBtn = stationScreen.findViewById(R.id.restart_vr_btn);
        restartVRBtn.setOnClickListener(view -> {
            StationManager.executeStationCommand(StationManager.RESTART_VR);
        });

        LinearLayout endVRBtn = stationScreen.findViewById(R.id.end_vr_btn);
        endVRBtn.setOnClickListener(view -> {
            StationManager.executeStationCommand(StationManager.STOP_VR);
        });
    }

    /**
     * Find the buttons on the l__steam_application_screen layout and assign the on click functions to each.
     */
    private void setupApplicationButtons() {
        Button installBtn = applicationScreen.findViewById(R.id.install_core_btn);
        installBtn.setOnClickListener(view -> {
            if (checkSelection()) {
                StationManager.executeStationCommand(StationManager.INSTALL + ApplicationManager.selected.getID());
            }
        });

        Button uninstallBtn = applicationScreen.findViewById(R.id.uninstall_core_btn);
        uninstallBtn.setOnClickListener(view -> {
            if (checkSelection()) {
                // Remove from the adapter view?
                StationManager.executeStationCommand(StationManager.UNINSTALL + ApplicationManager.selected.getID());
            }
        });

        Button verifyAppBtn = applicationScreen.findViewById(R.id.verify_core_btn);
        verifyAppBtn.setOnClickListener(view -> {
            Log.d(TAG, "Verify the apps local files");
        });

        ImageView backBtn = applicationScreen.findViewById(R.id.leadme_icon);
        backBtn.setOnClickListener(view -> main.changeScreen(MainActivity.ANIM_STATION_INDEX));
    }

    boolean isOn = false;
    /**
     * Testing how to send the actions to the room
     */
    private void setupRoomButtons() {
        Button lightBtn = roomScreen.findViewById(R.id.light_switch_btn);
        lightBtn.setOnClickListener(view -> {
            if(isOn) {
                StationManager.executeAutomationCommand(StationManager.LIGHT_ON);
                isOn = false;
                lightBtn.setBackgroundTintList(ContextCompat.getColorStateList(main.context, R.color.leadme_purple));
            } else {
                StationManager.executeAutomationCommand(StationManager.LIGHT_OFF);
                isOn = true;
                lightBtn.setBackgroundTintList(ContextCompat.getColorStateList(main.context, R.color.leadme_white));
            }
        });

        ImageView backBtn = roomScreen.findViewById(R.id.leadme_icon);
        backBtn.setOnClickListener(view -> main.changeScreen(MainActivity.ANIM_HOME_INDEX));
    }

    private boolean checkSelection() {
        return ApplicationManager.selected != null;
    }
}
