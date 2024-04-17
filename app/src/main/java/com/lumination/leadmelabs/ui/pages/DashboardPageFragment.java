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
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.databinding.PageDashboardBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.handlers.StatusHandler;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsConstants;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.unique.snowHydro.SnowyHydroConstants;
import com.lumination.leadmelabs.utilities.Identifier;
import com.segment.analytics.Properties;

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
    private PageDashboardBinding binding;
    public static final String segmentClassification = "Dashboard";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page_dashboard, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        SettingsViewModel settingsViewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel.class);
        binding.setSettings(settingsViewModel);

        if (savedInstanceState == null) {
            loadFragments();
        }

        // Setup the Dashboard action buttons depending on the set layout
        setupDashboardLayout(view);

        //Run the identify flow
        FlexboxLayout identify = view.findViewById(R.id.identify_button);
        identify.setOnClickListener(v -> {
            List<Station> stations = StationsFragment.getInstance().getRoomStations();
            Identifier.identifyStations(stations);
        });

        //Open the help page
        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(HelpPageFragment.class, "help", null);
        });
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

    private void trackDashboardEvent(String event) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        Segment.trackEvent(event, segmentProperties);
    }

    //region Dashboard Layout
    /**
     * Check what layout version is currently set and setup up the action buttons accordingly.
     * @param view The parent view where the information will be displayed.
     */
    private void setupDashboardLayout(View view) {
        String layout = SettingsFragment.mViewModel.getTabletLayoutScheme().getValue();

        //This happens regardless of the layout type
        setupTimeDateDisplay(view);

        if (layout == null) {
            setupWelcomeTitle(view);
            setupStandardDashboardButtons(view);
            return;
        }

        switch (layout) {
            case SettingsConstants.SNOWY_HYDRO_LAYOUT:
                setupSnowyTitle(view);
                setupSnowHydroDashboardButtons(view);
                break;

            case SettingsConstants.DEFAULT_LAYOUT:
            default:
                setupWelcomeTitle(view);
                setupStandardDashboardButtons(view);
                break;
        }
    }

    /**
     * Setup the snowy hydro dashboard buttons.
     * @param view The parent view where the information will be displayed.
     */
    private void setupSnowHydroDashboardButtons(View view) {
        //Switch to VR mode
        setupVrModeButton(view);

        //Switch to Presentation mode
        setupPresentationModeButton(view);

        //End session on all/selected stations
        setupEndSessionButton(view);

        //Restart all stations
        setupRestartAllButton(view);

        //Shutdown the lab
        setupShutdownButton(view);
    }

    /**
     * Setup the standard dashboard buttons.
     * @param view The parent view where the information will be displayed.
     */
    private void setupStandardDashboardButtons(View view) {
        //Switch to VR mode
        setupVrModeButton(view);

        //Launch the new session flow
        setupNewSessionButton(view);

        //End session on all/selected stations
        setupEndSessionButton(view);

        //Restart all stations
        setupRestartAllButton(view);

        //Switch to classroom mode
        setupClassModeButton(view);
    }

    //region Common Dashboard Buttons
    private void setupVrModeButton(View view) {
        FlexboxLayout vrMode = view.findViewById(R.id.vr_mode_button);
        vrMode.setOnClickListener(v -> {
            searchForSceneTrigger("vr");
            // Send data to Segment
            Segment.generateNewSessionId(); //Before the Segment track in order to set the sessionId
            trackDashboardEvent(SegmentConstants.Event_Lab_VR_Mode);
        });
    }

    private void setupNewSessionButton(View view) {
        FlexboxLayout newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            StationsFragment.mViewModel.setSelectedStationId(0);
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(LibrarySelectionFragment.class, "session", null);
            trackDashboardEvent(SegmentConstants.New_Session_Button);
        });
    }

    private void setupPresentationModeButton(View view) {
        FlexboxLayout presentationMode = view.findViewById(R.id.presentation_mode_button);
        presentationMode.setOnClickListener(v -> {
            searchForSceneTrigger("presentation");
            // Send data to Segment
            trackDashboardEvent(SegmentConstants.Event_Lab_Presentation_Mode);
        });
    }

    private void setupEndSessionButton(View view) {
        FlexboxLayout endSession = view.findViewById(R.id.end_session_button);
        endSession.setOnClickListener(v -> {
            BooleanCallbackInterface selectStationsCallback = confirmationResult -> {
                if (confirmationResult) {
                    endAllSessionsConfirmation();
                    trackDashboardEvent(SegmentConstants.End_Session_On_All);
                } else {
                    ArrayList<Station> stationsToSelectFrom = StationsFragment.getInstance().getRoomStations();
                    if (stationsToSelectFrom == null) return;

                    stationsToSelectFrom = new ArrayList<>(stationsToSelectFrom);
                    List<Station> filteredList = stationsToSelectFrom.stream()
                            .filter(station -> !station.getIsHidden())
                            .collect(Collectors.toList());

                    DialogManager.createEndSessionDialog(new ArrayList<>(filteredList));
                }
            };
            DialogManager.createConfirmationDialog("End session on all stations?", "This will stop any running experiences",
                    selectStationsCallback,
                    "End on select",
                    "End on all",
                    true);
            trackDashboardEvent(SegmentConstants.New_Session_Button);
        });
    }

    private void setupRestartAllButton(View view) {
        FlexboxLayout restart = view.findViewById(R.id.restart_button);
        TextView restartHeading = view.findViewById(R.id.restart_heading);
        TextView restartContent = view.findViewById(R.id.restart_content);
        restart.setOnClickListener(v -> {
            ArrayList<Integer> active = new ArrayList<>();

            //Check what stations are still running an experience
            ArrayList<Station> stations = StationsFragment.getInstance().getRoomStations();
            for(Station station: stations) {
                if(station.applicationController.getExperienceName() == null) {
                    continue;
                }
                if(station.applicationController.getExperienceName().length() == 0 || station.applicationController.getExperienceName().equals("null")) {
                    continue;
                }
                active.add(station.id);
            }

            if(active.size() > 0 && SettingsFragment.checkAdditionalExitPrompts()) {
                BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                    if (confirmationResult) {
                        restartOrShutdownAllStations(restartHeading, restartContent, false);

                        trackDashboardEvent(SegmentConstants.Event_Lab_Restart);
                    }
                };

                DialogManager.createConfirmationDialog(
                        "Confirm shutdown",
                        "Are you sure you want to restart? Experiences are still running on " + (active.size() > 1 ? "stations " : "station ") + TextUtils.join(", ", active) + ". Please confirm this action.",
                        confirmAppExitCallback,
                        "Cancel",
                        "Confirm",
                        false);
            } else {
                restartOrShutdownAllStations(restartHeading, restartContent, false);

                trackDashboardEvent(SegmentConstants.Event_Lab_Restart);
            }
        });
    }

    private void setupShutdownButton(View view) {
        FlexboxLayout shutdown = view.findViewById(R.id.shutdown_button);
        TextView shutdownHeading = view.findViewById(R.id.shutdown_heading);
        TextView shutdownContent = view.findViewById(R.id.shutdown_content);
        shutdown.setOnClickListener(v -> {
            ArrayList<Integer> active = new ArrayList<>();

            //Check what stations are still running an experience
            ArrayList<Station> stations = StationsFragment.getInstance().getRoomStations();
            for(Station station: stations) {
                if(station.applicationController.getExperienceName() == null) {
                    continue;
                }
                if(station.applicationController.getExperienceName().length() == 0 || station.applicationController.getExperienceName().equals("null")) {
                    continue;
                }
                active.add(station.id);
            }

            if(active.size() > 0 && SettingsFragment.checkAdditionalExitPrompts()) {
                BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                    if (confirmationResult) {
                        restartOrShutdownAllStations(shutdownHeading, shutdownContent, true);

                        trackDashboardEvent(SegmentConstants.Event_Lab_Restart);
                    }
                };

                DialogManager.createConfirmationDialog(
                        "Confirm shutdown",
                        "Are you sure you want to restart? Experiences are still running on " + (active.size() > 1 ? "stations " : "station ") + TextUtils.join(", ", active) + ". Please confirm this action.",
                        confirmAppExitCallback,
                        "Cancel",
                        "Confirm",
                        false);
            } else {
                restartOrShutdownAllStations(shutdownHeading, shutdownContent, true);

                trackDashboardEvent(SegmentConstants.Event_Lab_Restart);
            }
        });
    }

    private void setupClassModeButton(View view) {
        FlexboxLayout classroomMode = view.findViewById(R.id.classroom_mode_button);
        classroomMode.setOnClickListener(v -> {
            searchForSceneTrigger("classroom");
            trackDashboardEvent(SegmentConstants.Event_Lab_Classroom_Mode);
            Segment.resetSession(); //After the Segment track as to record the last sessionId
        });
    }
    //endregion

    //region Common Dashboard Actions
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
                    if (station.isOn()) continue; //Do not do anything if the Station is already on

                    station.setStatus(StatusHandler.TURNING_ON);
                    NetworkService.sendMessage("NUC", "UpdateStation", id + ":SetValue:status:" + StatusHandler.TURNING_ON);
                    station.statusHandler.powerStatusCheck(station.getId(), 3 * 60 * 1000);
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
                confirmShutdownCallback,
                "Cancel",
                "Confirm",
                false);
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
                    "Confirm",
                    false);
        } else {
            int[] selectedIds = StationsFragment.getInstance().getRoomStations().stream().mapToInt(station -> station.id).toArray();
            String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

            NetworkService.sendMessage("Station," + stationIds, "CommandLine", "StopGame");
        }
    }

    /**
     * Send a command to shutdown or restart all Stations within the lab. If a command has already
     * been sent or is in the process, it will instead cancel the initial shutdown or restart.
     * @param heading A Textview of the heading text to set.
     * @param content A Textview of the content text to set.
     * @param shutdown A boolean if the function should shutdown (true) or restart (false).
     */
    private void restartOrShutdownAllStations(TextView heading, TextView content, boolean shutdown) {
        CountdownCallbackInterface shutdownCountDownCallback = seconds -> {
            if (seconds <= 0) {
                heading.setText(shutdown ? R.string.restart_space : R.string.shut_down_space);
                content.setText(shutdown ? R.string.restart_stations : R.string.shut_down_stations);
            } else {
                if (!cancelledShutdown) {
                    heading.setText(MessageFormat.format("Cancel ({0})", seconds));
                    content.setText(shutdown ? R.string.cancel_reboot : R.string.cancel_shutdown);
                }
            }
        };

        int[] stationIds = StationsFragment.getInstance().getRoomStations().stream().mapToInt(station -> station.id).toArray();
        if (heading.getText().toString().startsWith(shutdown ? "Shutdown" : "Restart")) {
            cancelledShutdown = false;
            DialogManager.buildShutdownOrRestartDialog(getContext(), shutdown ? "Shutdown" : "Restart", stationIds, shutdownCountDownCallback);
        } else {
            cancelledShutdown = true;
            String stationIdsString = String.join(", ", Arrays.stream(stationIds).mapToObj(String::valueOf).toArray(String[]::new));
            NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", "CancelShutdown");
            heading.setText(shutdown ? R.string.restart_space : R.string.shut_down_space);
            content.setText(shutdown ? R.string.restart_stations : R.string.shut_down_stations);
        }
    }
    //endregion

    //region Welcome Text
    /**
     * Setup the welcome text based on the welcome constant saved in the Snowy Hydro folder.
     * @param view The parent view where the information will be displayed.
     */
    private void setupSnowyTitle(View view) {
        TextView welcomeMessage = view.findViewById(R.id.welcome_message);
        welcomeMessage.setText(SnowyHydroConstants.WELCOME);
    }

    /**
     * Setup the welcome text for the user that changes depending on the time of day.
     * selected tablet layout.
     * @param view The parent view where the information will be displayed.
     */
    private void setupWelcomeTitle(View view) {
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
    }

    /**
     * Setup the time and date display that appears under the welcome text.
     * @param view The parent view where the information will be displayed.
     */
    private void setupTimeDateDisplay(View view) {
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
    //endregion
    //endregion
}
