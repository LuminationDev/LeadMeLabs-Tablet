package com.lumination.leadmelabs.ui.pages;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.application.ApplicationSelectionFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Identifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardPageFragment extends Fragment {
    public static FragmentManager childManager;
    private static boolean cancelledShutdown = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page_dashboard, container, false);
        childManager = getChildFragmentManager();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            loadFragments();
        }

        int currentDate = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String message = "Welcome!";
        if (currentDate < 12) {
            message = "Good Morning!";
        } else if (currentDate < 17) {
            message = "Good Afternoon!";
        } else {
            message = "Good Evening!";
        }
        TextView welcomeMessage = view.findViewById(R.id.welcome_message);
        welcomeMessage.setText(message);

        LocalDate now = LocalDate.now();
        String dateMessage = "";
        String dayName = now.getDayOfWeek().name();
        dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase(Locale.ROOT);
        dateMessage += (dayName + " ");
        dateMessage += (now.getDayOfMonth() + getDayOfMonthSuffix(now.getDayOfMonth()) +" ");
        String monthName = now.getMonth().name();
        monthName = monthName.charAt(0) + monthName.substring(1).toLowerCase(Locale.ROOT);
        dateMessage += (monthName + " ");
        dateMessage += (now.getYear() + " ");
        TextView dateMessageView = view.findViewById(R.id.date_message);
        dateMessageView.setText(dateMessage);

        //Switch to VR mode
        FlexboxLayout vrMode = view.findViewById(R.id.vr_mode_button);
        vrMode.setOnClickListener(v -> searchForSceneTrigger("vr"));

        //Launch the new session flow
        FlexboxLayout newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            ((SideMenuFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(ApplicationSelectionFragment.class, "session", null);
            ApplicationSelectionFragment.setStationId(0);
        });

        //End session on all/selected stations
        FlexboxLayout endSession = view.findViewById(R.id.end_session_button);
        endSession.setOnClickListener(v -> {
            BooleanCallbackInterface selectStationsCallback = confirmationResult -> {
                if (confirmationResult) {
                    endAllSessionsConfirmation();
                } else {
                    ArrayList<Station> stationsToSelectFrom = (ArrayList<Station>) StationsFragment.getInstance().getRoomStations().clone();
                    DialogManager.createEndSessionDialog(stationsToSelectFrom);
                }
            };
            DialogManager.createConfirmationDialog("End session on all stations?", "This will stop any running experiences", selectStationsCallback, "End on select", "End on all");
        });

        //Restart all stations
        FlexboxLayout restart = view.findViewById(R.id.restart_button);
        TextView restartHeading = view.findViewById(R.id.restart_heading);
        TextView restartContent = view.findViewById(R.id.restart_content);
        restart.setOnClickListener(v -> {
            ArrayList<Integer> active = new ArrayList<>();

            //Check what stations are still running an experience
            ArrayList<Station> stations = StationsFragment.getInstance().getRoomStations();
            for(Station station: stations) {
                if(station.gameName == null) {
                    continue;
                }
                if(station.gameName.length() == 0 || station.gameName.equals("null")) {
                    continue;
                }
                active.add(station.id);
            }

            if(active.size() > 0 && SettingsFragment.checkAdditionalExitPrompts()) {
                BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                    if (confirmationResult) {
                        restartAllStations(restartHeading, restartContent);
                    }
                };

                DialogManager.createConfirmationDialog(
                        "Confirm shutdown",
                        "Are you sure you want to restart? Experiences are still running on " + (active.size() > 1 ? "stations " : "station ") + TextUtils.join(", ", active) + ". Please confirm this action.",
                        confirmAppExitCallback,
                        "Cancel",
                        "Confirm");
            } else {
                restartAllStations(restartHeading, restartContent);
            }
        });

        //Switch to classroom mode
        FlexboxLayout classroomMode = view.findViewById(R.id.classroom_mode_button);
        classroomMode.setOnClickListener(v -> searchForSceneTrigger("classroom"));

        //Run the identify flow
        FlexboxLayout identify = view.findViewById(R.id.identify_button);
        identify.setOnClickListener(v -> {
            List<Station> stations = StationsFragment.getInstance().getRoomStations();
            Identifier.identifyStations(stations);
        });
      
        //Open the help page
        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            ((SideMenuFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(HelpPageFragment.class, "help", null);
        });

        SettingsViewModel settingsViewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel.class);
        settingsViewModel.getHideStationControls().observe(getViewLifecycleOwner(), hideStationControls -> {
            View stationControls = view.findViewById(R.id.station_controls);
            stationControls.setVisibility(hideStationControls ? View.GONE : View.VISIBLE);
            View stations = view.findViewById(R.id.stations);
            stations.setVisibility(hideStationControls ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * Search the appliances list looking for a scene whose name contains the supplied sceneName. Only
     * appliances with type 'scenes' will be looked at. If found send a message to the NUC to trigger
     * the scene. Prompt the user for any Stations that will be turned off in the process.
     * @param sceneName A lowercase string that the scene name will contain.
     */
    private void searchForSceneTrigger(String sceneName) {
        ApplianceViewModel applianceViewModel = ViewModelProviders.of(requireActivity()).get(ApplianceViewModel.class);
        List<Appliance> appliances = applianceViewModel.getAppliances().getValue();

        if (appliances == null) {
            return;
        }

        //Collect scene(s) matching the name. NOTE: There may be multiple rooms containing the same scene
        // - The name contains the supplied name
        // - Is of 'type' scenes
        // - Is in one of the currently focused rooms
        List<Appliance> matchingAppliances = appliances.stream()
                .filter(appliance -> appliance.name.toLowerCase().contains(sceneName)
                        && "scenes".equals(appliance.type)
                        && SettingsFragment.checkLockedRooms(appliance.room))
                .collect(Collectors.toList());

        if (matchingAppliances.isEmpty()) {
            matchingAppliances = searchForBackupSceneTrigger(appliances, sceneName);
        }

        //There really is nothing
        if (matchingAppliances.isEmpty()) return;

        // If turning on stations update the Station statuses.
        ArrayList<JSONObject> stationsToTurnOn = collectStationsWithActions(matchingAppliances, "On");
        if (!stationsToTurnOn.isEmpty()) {
            for (JSONObject stationObject: stationsToTurnOn) {
                try {
                    int id = Integer.parseInt(stationObject.getString("id"));
                    Station station = ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).getStationById(id);
                    if (station.status.equals("On")) continue; //Do not do anything if the Station is already on

                    station.status = "Turning On";
                    NetworkService.sendMessage("NUC", "UpdateStation", id + ":SetValue:status:Turning On");
                    station.powerStatusCheck(3 * 60 * 1000);
                    ViewModelProviders.of(MainActivity.getInstance()).get(StationsViewModel.class).updateStationById(id, station);
                } catch (JSONException e) {
                    Log.e("JSON OBJECT", "Cannot extract ID from: " + stationObject);
                }
            }
        }

        ArrayList<JSONObject> stationsToTurnOff = collectStationsWithActions(matchingAppliances, "Off");
        if (stationsToTurnOff.isEmpty()) {
            for (Appliance sceneAppliance: matchingAppliances) {
                String automationValue = sceneAppliance.id.substring(sceneAppliance.id.length() - 1);
                String message = "Set:" + sceneAppliance.id + ":" + automationValue + ":" + NetworkService.getIPAddress();
                NetworkService.sendMessage("NUC", "Automation", message);
            }
            return;
        }

        List<Appliance> finalMatchingAppliances = matchingAppliances;
        BooleanCallbackInterface confirmShutdownCallback = confirmationResult -> {
            if (confirmationResult) {
                for (Appliance sceneAppliance: finalMatchingAppliances) {
                    String automationValue = sceneAppliance.id.substring(sceneAppliance.id.length() - 1);
                    String message = "Set:" + sceneAppliance.id + ":" + automationValue + ":" + NetworkService.getIPAddress();
                    NetworkService.sendMessage("NUC", "Automation", message);
                }
            }
        };

        DialogManager.createConfirmationDialog("Confirm station shutdown",
                "All Station(s) will shutdown. Please confirm this scene.",
                confirmShutdownCallback, "Cancel", "Confirm");
    }

    /**
     * If there are no scenes that contain the original sceneName ('classroom' or 'vr') search for a
     * scene that would trigger connected stations in the same way.
     * @param appliances A list of all the appliances in the ApplianceViewModel.
     * @param originalSceneName A string of the original scene that was searched for.
     * @return A list of appliances that act the same as the initial search scene, this may be empty.
     */
    private List<Appliance> searchForBackupSceneTrigger(List<Appliance> appliances, String originalSceneName) {
        List<Appliance> matchingAppliances = new ArrayList<>();

        List<Appliance> sceneAppliances = appliances.stream()
                .filter(appliance -> !appliance.name.toLowerCase().contains("all off")
                        &&"scenes".equals(appliance.type)
                        && SettingsFragment.checkLockedRooms(appliance.room))
                .collect(Collectors.toList());

        //Sort through each scene to see if there are computer actions assigned to it.
        for (Appliance sceneAppliance : sceneAppliances) {
            List<Appliance> singleApplianceList = new ArrayList<>();
            singleApplianceList.add(sceneAppliance);

            String desiredAction = originalSceneName.equals("classroom") ? "Off" : "On";
            ArrayList<JSONObject> stationsWithActions = collectStationsWithActions(singleApplianceList, desiredAction);

            if (!stationsWithActions.isEmpty()) {
                matchingAppliances.add(sceneAppliance); // Scene appliance matches desired actions
            }
        }

        return matchingAppliances;
    }

    /**
     * Loop through the supplied list of appliances, check each appliance to see if there are associated
     * stations. Detect if there are any actions connected to the stations if there are any.
     * @param matchingAppliances A list of appliances,
     * @param action A string of the station action to look for.
     * @return A list of stations that require an action be performed on them.
     */
    private ArrayList<JSONObject> collectStationsWithActions(List<Appliance> matchingAppliances, String action) {
        ArrayList<JSONObject> stationsWithActions = new ArrayList<>();

        for (Appliance sceneAppliance: matchingAppliances) {
            if (sceneAppliance.stations == null || sceneAppliance.stations.length() == 0) {
                continue;
            }

            for (int i = 0; i < sceneAppliance.stations.length(); i++) {
                try {
                    JSONObject station = sceneAppliance.stations.getJSONObject(i);
                    if (!station.has("action")) {
                        continue;
                    }
                    if (station.getString("action").equals(action)) {
                        stationsWithActions.add(station);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return stationsWithActions;
    }

    /**
     * If Additional exit prompts are turned on in the settings. Ask the user if they are sure they
     * want to exit the current experiences.
     */
    private void endAllSessionsConfirmation() {
        if(SettingsFragment.checkAdditionalExitPrompts()) {
            BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                if (confirmationResult) {
                    int[] selectedIds = StationsFragment.getInstance().getRoomStations().stream().mapToInt(station -> station.id).toArray();
                    String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

                    NetworkService.sendMessage("Station," + stationIds, "CommandLine", "StopGame");
                }
            };

            DialogManager.createConfirmationDialog(
                    "Confirm experience exit",
                    "Are you sure you want to exit? Some users may require saving their progress. Please confirm this action.",
                    confirmAppExitCallback,
                    "Cancel",
                    "Confirm");
        } else {
            int[] selectedIds = StationsFragment.getInstance().getRoomStations().stream().mapToInt(station -> station.id).toArray();
            String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

            NetworkService.sendMessage("Station," + stationIds, "CommandLine", "StopGame");
        }
    }

    private void restartAllStations(TextView restartHeading, TextView restartContent) {
        CountdownCallbackInterface shutdownCountDownCallback = seconds -> {
            if (seconds <= 0) {
                restartHeading.setText(R.string.restart_space);
                restartContent.setText(R.string.restart_stations);
            } else {
                if (!cancelledShutdown) {
                    restartHeading.setText(MessageFormat.format("Cancel ({0})", seconds));
                    restartContent.setText(R.string.cancel_reboot);
                }
            }
        };

        int[] stationIds = StationsFragment.getInstance().getRoomStations().stream().mapToInt(station -> station.id).toArray();
        if (restartHeading.getText().toString().startsWith("Restart")) {
            cancelledShutdown = false;
            DialogManager.buildShutdownOrRestartDialog(getContext(), "Restart", stationIds, shutdownCountDownCallback);
        } else {
            cancelledShutdown = true;
            String stationIdsString = String.join(", ", Arrays.stream(stationIds).mapToObj(String::valueOf).toArray(String[]::new));
            NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", "CancelShutdown");
            restartHeading.setText(R.string.restart_space);
            restartContent.setText(R.string.restart_stations);
        }
    }

    /**
     * Load in the initial fragments for the main view.
     */
    private void loadFragments() {
        childManager.beginTransaction()
                .replace(R.id.stations, StationsFragment.class, null)
                .replace(R.id.logo, LogoFragment.class, null)
                .replace(R.id.rooms, RoomFragment.class, null)
                .commitNow();
    }

    String getDayOfMonthSuffix(final int n) {
        if (n < 1 || n > 31) {
            return "";
        }
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }
}
