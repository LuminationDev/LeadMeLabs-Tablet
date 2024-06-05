package com.lumination.leadmelabs.ui.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentDashboardBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.handlers.StatusHandler;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.pages.LibraryPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsConstants;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.unique.snowHydro.stations.SnowyHydroStationsFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardFragment extends Fragment {
    public static DashboardViewModel mViewModel;
    public static FragmentManager childManager;
    private static boolean cancelledShutdown = false;

    public static DashboardFragment instance;
    public static DashboardFragment getInstance() { return instance; }
    private FragmentDashboardBinding binding;
    public static final String segmentClassification = "Dashboard";
    public DashboardModeManagement dashboardModeManagement = new DashboardModeManagement();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setDashboard(mViewModel);
        SettingsViewModel settingsViewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel.class);
        binding.setSettings(settingsViewModel);

        // Setup the Dashboard action buttons depending on the set layout
        setupDashboardLayout(view);

        instance = this;
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

        if (layout == null) {
            loadStandardFragments();
            setupStandardDashboardButtons(view);
            return;
        }

        switch (layout) {
            case SettingsConstants.SNOWY_HYDRO_LAYOUT:
                loadSnowyHydroFragments();
                setupSnowHydroDashboardButtons(view);
                break;

            case SettingsConstants.DEFAULT_LAYOUT:
            default:
                loadStandardFragments();
                setupStandardDashboardButtons(view);
                break;
        }
    }

    /**
     * Load in the standard initial fragments for the main view.
     */
    private void loadStandardFragments() {
        childManager.beginTransaction()
                .replace(R.id.stations, StationsFragment.class, null)
                .commitNow();
    }

    /**
     * Load in the snowy hydro initial fragments for the main view.
     */
    private void loadSnowyHydroFragments() {
        childManager.beginTransaction()
                .replace(R.id.stations, SnowyHydroStationsFragment.class, null)
                .commitNow();
    }

    /**
     * Setup the snowy hydro dashboard buttons.
     * @param view The parent view where the information will be displayed.
     */
    private void setupSnowHydroDashboardButtons(View view) {
        //Switch to VR mode
        setupVrModeButton(view);

        //Switch to Presentation mode
        setupShowcaseModeButton(view);

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

    /**
     * Show a toast to the user if the mode is currently changing and they attempt to select
     * another mode. Lodge a segment event when this happens.
     */
    public void changingModePrompt(String scene) {
        Toast.makeText(getContext(), "A mode is currently being set, please wait...", Toast.LENGTH_LONG).show();

        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("scene", scene);
        Segment.trackEvent(SegmentConstants.Event_Disabled_Scene_Selected, segmentProperties);
    }

    //region Common Dashboard Buttons
    private void setupVrModeButton(View view) {
        FlexboxLayout vrMode = view.findViewById(R.id.vr_mode_button);
        vrMode.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(mViewModel.getChangingMode().getValue())) {
                changingModePrompt(SegmentConstants.Event_Lab_VR_Mode);
                return;
            }

            dashboardModeManagement.changeModeButtonAvailability("vr", "", Constants.VR_MODE);
            searchForSceneTrigger("vr");
            // Send data to Segment
            Segment.generateNewSessionId(); //Before the Segment track in order to set the sessionId
            trackDashboardEvent(SegmentConstants.Event_Lab_VR_Mode);
        });
    }

    private void setupNewSessionButton(View view) {
        FlexboxLayout newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(mViewModel.getChangingMode().getValue())) {
                changingModePrompt(SegmentConstants.New_Session_Button);
                return;
            }

            StationsFragment.mViewModel.setSelectedStationId(0);
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(LibraryPageFragment.class, "session", null);
            trackDashboardEvent(SegmentConstants.New_Session_Button);
        });
    }

    private void setupShowcaseModeButton(View view) {
        FlexboxLayout showcaseMode = view.findViewById(R.id.showcase_mode_button);
        showcaseMode.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(mViewModel.getChangingMode().getValue())) {
                changingModePrompt(SegmentConstants.Event_Lab_Showcase_Mode);
                return;
            }

            dashboardModeManagement.changeModeButtonAvailability("showcase", "", Constants.SHOWCASE_MODE);
            searchForSceneTrigger("showcase");
            // Send data to Segment
            trackDashboardEvent(SegmentConstants.Event_Lab_Showcase_Mode);
        });
    }

    private void setupEndSessionButton(View view) {
        FlexboxLayout endSession = view.findViewById(R.id.end_session_button);
        endSession.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(mViewModel.getChangingMode().getValue())) {
                changingModePrompt(SegmentConstants.End_Session_Modal);
                return;
            }

            BooleanCallbackInterface selectStationsCallback = confirmationResult -> {
                if (confirmationResult) {
                    endAllSessionsConfirmation();
                    trackDashboardEvent(SegmentConstants.End_Session_On_All);
                } else {
                    ArrayList<Station> stationsToSelectFrom = Helpers.getRoomStations();
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
            trackDashboardEvent(SegmentConstants.End_Session_Modal);
        });
    }

    private void setupRestartAllButton(View view) {
        FlexboxLayout restart = view.findViewById(R.id.restart_button);
        TextView restartHeading = view.findViewById(R.id.restart_heading);
        TextView restartContent = view.findViewById(R.id.restart_content);
        restart.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(mViewModel.getChangingMode().getValue())) {
                changingModePrompt(SegmentConstants.Event_Lab_Restart);
                return;
            }

            ArrayList<Integer> active = getActive();

            if(!active.isEmpty() && SettingsFragment.checkAdditionalExitPrompts()) {
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
            if (Boolean.TRUE.equals(mViewModel.getChangingMode().getValue())) {
                changingModePrompt(SegmentConstants.Event_Lab_Shutdown);
                return;
            }

            ArrayList<Integer> active = getActive();

            if(!active.isEmpty() && SettingsFragment.checkAdditionalExitPrompts()) {
                BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                    if (confirmationResult) {
                        restartOrShutdownAllStations(shutdownHeading, shutdownContent, true);

                        trackDashboardEvent(SegmentConstants.Event_Lab_Shutdown);
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

                trackDashboardEvent(SegmentConstants.Event_Lab_Shutdown);
            }
        });
    }

    @NonNull
    private static ArrayList<Integer> getActive() {
        ArrayList<Integer> active = new ArrayList<>();

        //Check what stations are still running an experience
        ArrayList<Station> stations = Helpers.getRoomStations();
        for(Station station: stations) {
            if(station.applicationController.getExperienceName() == null) {
                continue;
            }
            if(station.applicationController.getExperienceName().isEmpty() || station.applicationController.getExperienceName().equals("null")) {
                continue;
            }
            active.add(station.id);
        }
        return active;
    }

    private void setupClassModeButton(View view) {
        FlexboxLayout classroomMode = view.findViewById(R.id.classroom_mode_button);
        classroomMode.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(mViewModel.getChangingMode().getValue())) {
                changingModePrompt(SegmentConstants.Event_Lab_Classroom_Mode);
                return;
            }

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
            matchingAppliances = searchForBackupSceneTrigger(appliances, sceneName, null);
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
                    if (station == null || station.isOn()) continue; //Do not do anything if the Station is already on

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

        BooleanCallbackInterface confirmShutdownCallback = getBooleanCallbackInterface(matchingAppliances);

        DialogManager.createConfirmationDialog("Confirm station shutdown",
                "All Station(s) will shutdown. Please confirm this scene.",
                confirmShutdownCallback,
                "Cancel",
                "Confirm",
                false);
    }

    /**
     * Create the required boolean callback that triggers associated appliances.
     * @param matchingAppliances A list of appliances that are to be set.
     * @return A BooleanCallbackInterface
     */
    @NonNull
    private BooleanCallbackInterface getBooleanCallbackInterface(List<Appliance> matchingAppliances) {
        return confirmationResult -> {
            if (confirmationResult) {
                dashboardModeManagement.changeModeButtonAvailability("classroom", "", Constants.CLASSROOM_MODE);

                for (Appliance sceneAppliance: matchingAppliances) {
                    String automationValue = sceneAppliance.id.substring(sceneAppliance.id.length() - 1);
                    String message = "Set:" + sceneAppliance.id + ":" + automationValue + ":" + NetworkService.getIPAddress();
                    NetworkService.sendMessage("NUC", "Automation", message);
                }
            }
        };
    }

    /**
     * If Additional exit prompts are turned on in the settings. Ask the user if they are sure they
     * want to exit the current experiences.
     */
    private void endAllSessionsConfirmation() {
        if(SettingsFragment.checkAdditionalExitPrompts()) {
            BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                if (confirmationResult) {
                    int[] selectedIds = Helpers.getRoomStations().stream().mapToInt(station -> station.id).toArray();
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
            int[] selectedIds = Helpers.getRoomStations().stream().mapToInt(station -> station.id).toArray();
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
                heading.setText(shutdown ? R.string.shut_down_space : R.string.restart_space);
                content.setText(shutdown ? R.string.shut_down_stations : R.string.restart_stations);
            } else {
                if (!cancelledShutdown) {
                    heading.setText(MessageFormat.format("Cancel ({0})", seconds));
                    content.setText(shutdown ? R.string.cancel_reboot : R.string.cancel_shutdown);
                }
            }
        };

        int[] stationIds = Helpers.getRoomStations().stream().mapToInt(station -> station.id).toArray();
        if (heading.getText().toString().startsWith(shutdown ? "Shutdown" : "Restart")) {
            cancelledShutdown = false;
            DialogManager.buildShutdownOrRestartDialog(getContext(), shutdown ? "Shutdown" : "Restart", stationIds, shutdownCountDownCallback);
        } else {
            mViewModel.resetMode(shutdown ? Constants.SHUTDOWN_MODE : Constants.RESTART_MODE);
            cancelledShutdown = true;
            String stationIdsString = String.join(", ", Arrays.stream(stationIds).mapToObj(String::valueOf).toArray(String[]::new));
            NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", "CancelShutdown");
            heading.setText(shutdown ? R.string.shut_down_space : R.string.restart_space);
            content.setText(shutdown ? R.string.shut_down_stations : R.string.restart_stations);
        }
    }

    /**
     * If there are no scenes that contain the original sceneName ('classroom' or 'vr') search for a
     * scene that would trigger connected stations in the same way.
     * @param appliances A list of all the appliances in the ApplianceViewModel.
     * @param originalSceneName A string of the original scene that was searched for.
     * @return A list of appliances that act the same as the initial search scene, this may be empty.
     */
    protected List<Appliance> searchForBackupSceneTrigger(List<Appliance> appliances, String originalSceneName, String desiredAction) {
        List<Appliance> matchingAppliances = new ArrayList<>();

        List<Appliance> sceneAppliances = appliances.stream()
                .filter(appliance -> !appliance.name.toLowerCase().contains("all off")
                        && "scenes".equals(appliance.type)
                        && SettingsFragment.checkLockedRooms(appliance.room))
                .collect(Collectors.toList());

        //Sort through each scene to see if there are computer actions assigned to it.
        for (Appliance sceneAppliance : sceneAppliances) {
            List<Appliance> singleApplianceList = new ArrayList<>();
            singleApplianceList.add(sceneAppliance);

            if (desiredAction == null) {
                desiredAction = originalSceneName.equals("classroom") ? "Off" : "On";
            }

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
    protected ArrayList<JSONObject> collectStationsWithActions(List<Appliance> matchingAppliances, String action) {
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
                    Log.e("DashboardFragment", e.toString());
                }
            }
        }

        return stationsWithActions;
    }
    //endregion
    //endregion
}
