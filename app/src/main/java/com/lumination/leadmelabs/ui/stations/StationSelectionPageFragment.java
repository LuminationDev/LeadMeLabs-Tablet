package com.lumination.leadmelabs.ui.stations;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentPageStationSelectionBinding;
import com.lumination.leadmelabs.interfaces.IApplicationLoadedCallback;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.classes.SegmentExperienceEvent;
import com.lumination.leadmelabs.segment.classes.SegmentHelpEvent;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.library.application.ApplicationShareCodeFragment;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.unique.snowHydro.StationSingleNestedFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Identifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StationSelectionPageFragment extends Fragment {
    private static final int CHECK_INTERVAL = 1000; // Interval to check the variable in milliseconds
    private static final int TIMEOUT_DURATION = 60000; // Timeout duration in milliseconds
    private int elapsedTime = 0;

    private FragmentPageStationSelectionBinding binding;

    public static FragmentManager childManager;
    public static StationsViewModel mViewModel;

    public static StationSelectionPageFragment instance;
    public static StationSelectionPageFragment getInstance() { return instance; }

    private EditText[] editTexts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page_station_selection, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);

        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
        stations = (ArrayList<Station>) stations.clone();
        for (Station station:stations) {
            station.selected = false;
            mViewModel.updateStationById(station.id, station);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setStations(mViewModel);

        Bundle bundle = getArguments();
        String selectionType = bundle != null ? bundle.getString("selection") : "application";
        if (selectionType == null) {
            selectionType = "application";
        }
        mViewModel.setSelectionType(selectionType);

        loadFragments();
        setupButtons(view);

        switch (selectionType) {
            case "application":
                setupApplicationSelection(view);
                break;

            case "video":
                setupVideoSelection(view);
                break;
        }

        instance = this;
    }

    //region Setup
    private void setupApplicationSelection(View view) {
        //Specifically set the selected application
        Application selectedApplication = mViewModel.getSelectedApplication().getValue();
        binding.setSelectedApplication(selectedApplication);

        if (selectedApplication != null) {
            //TODO uncomment when application experiences have descriptions
            //Helpers.SetExperienceImage(selectedApplication.type, selectedApplication.name, selectedApplication.id, view);
        }
        //TODO uncomment when application experiences have descriptions
        //SetupEditText(view);

        CheckBox selectCheckbox = view.findViewById(R.id.select_all_checkbox);
        selectCheckbox.setOnCheckedChangeListener((checkboxView, checked) -> {
            ArrayList<Station> stations = StationSelectionFragment.getInstance().getRoomStations();
            stations = (ArrayList<Station>) stations.clone();
            for (Station station:stations) {
                if (!station.status.equals("Off") && station.applicationController.hasApplicationInstalled(mViewModel.getSelectedApplicationId())) {
                    station.selected = checked;
                    mViewModel.updateStationById(station.id, station);
                }
            }
        });
    }

    private void setupVideoSelection(View view) {
        //Specifically set the selected video
        Video selectedVideo = mViewModel.getSelectedVideo().getValue();
        binding.setSelectedVideo(selectedVideo);

        CheckBox selectCheckbox = view.findViewById(R.id.select_all_checkbox);
        selectCheckbox.setOnCheckedChangeListener((checkboxView, checked) -> {
            ArrayList<Station> stations = StationSelectionFragment.getInstance().getRoomStations();
            stations = (ArrayList<Station>) stations.clone();
            for (Station station:stations) {
                if (!station.status.equals("Off") && station.videoController.hasLocalVideo(selectedVideo)) {
                    station.selected = checked;
                    mViewModel.updateStationById(station.id, station);
                }
            }
        });
    }

    private void setupButtons(View view) {
        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(HelpPageFragment.class, "help", null);
            // Send data to Segment
            SegmentHelpEvent event = new SegmentHelpEvent(SegmentConstants.Event_Help_Page_Accessed, "Station Selection Page");
            Segment.trackAction(SegmentConstants.Event_Type_Help, event);
        });

        Button backButton = view.findViewById(R.id.cancel_button);
        backButton.setOnClickListener(v -> {
            mViewModel.selectSelectedApplication("");
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(LibrarySelectionFragment.class, "session", null);
        });

        Button playButton = view.findViewById(R.id.select_stations);
        playButton.setOnClickListener(v -> {
            int[] selectedIds = mViewModel.getSelectedStationIds();
            if (selectedIds.length > 0) {
                performAction(selectedIds);
            }
        });

        View identifyStations = view.findViewById(R.id.identify_button);
        identifyStations.setOnClickListener(v -> {
            List<Station> stations = StationSelectionFragment.getInstance().getRoomStations();
            Identifier.identifyStations(stations);
        });
    }
    //endregion

    private void performAction(int[] selectedIds) {
        String type = mViewModel.getSelectionType().getValue();
        switch (type != null ? type : "application") {
            case "application":
                confirmLaunchApplication(selectedIds);
                break;

            case "video":
                confirmLaunchVideo(selectedIds);
                break;
        }
    }

    /**
     * Confirms the launch of an application on selected stations.
     * Generates parameters, constructs a message, and sends it over the network.
     * Handles backward compatibility and updates the UI accordingly.
     *
     * @param selectedIds An array of selected station IDs
     */
    private void confirmLaunchApplication(int[] selectedIds) {
        Application selectedApplication = binding.getSelectedApplication();
        String parameters = ApplicationShareCodeFragment.generateParameters(selectedApplication.name, editTexts);
        if (parameters.equals(Constants.Invalid)) {
            return;
        }

        //TODO not sure if I like this here
        //STRICTLY FOR SNOWY HYDRO - If launching the Snowy Hydro Story, launch on the primary and the nested stations
        int[] allStations = combineStationIdsWithNested(selectedIds, selectedApplication.getName());
        String stationIds = String.join(", ", Arrays.stream(allStations).mapToObj(String::valueOf).toArray(String[]::new));

        //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
        if (MainActivity.isNucJsonEnabled) {
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "Launch");
                message.put("ExperienceId", selectedApplication.id);
                if (!parameters.isEmpty()) {
                    message.put("Parameters", parameters);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            NetworkService.sendMessage("Station," + stationIds, "Experience", message.toString());
        }
        else {
            String additionalData = "Launch:" + selectedApplication.id;
            if (!parameters.isEmpty()) {
                additionalData += ":Parameters:" + parameters;
            }

            NetworkService.sendMessage("Station," + stationIds, "Experience", additionalData);
        }

        // Send data to Segment - track the launch for each Station
        for (int station: selectedIds) {
            SegmentExperienceEvent event = new SegmentExperienceEvent(
                    SegmentConstants.Event_Experience_Launch,
                    station,
                    selectedApplication.getName(),
                    selectedApplication.getId(),
                    selectedApplication.getType());
            Segment.trackAction(SegmentConstants.Event_Type_Experience, event);
        }

        SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
        if (fragment == null) return;

        fragment.loadFragment(DashboardPageFragment.class, "dashboard", null);
        DialogManager.awaitStationApplicationLaunch(selectedIds, mViewModel.getSelectedApplicationName(selectedApplication.id), false);
    }

    /**
     * Combines the selected station ids with their nested station ids for Snowy Hydro application.
     *
     * @param selectedIds           The array of selected station ids.
     * @param experienceName        The name of the experience that will be launched.
     * @return An array containing the original station ids along with their nested station ids.
     */
    public int[] combineStationIdsWithNested(int[] selectedIds, String experienceName) {
        return Arrays.stream(selectedIds)
                .flatMap(id -> {
                    Station station = mViewModel.getStationById(id);
                    if (experienceName.toLowerCase().contains("snowy hydro")) {
                        int[] nestedStations = StationSingleNestedFragment.collectNestedStations(station, int[].class);
                        return IntStream.concat(IntStream.of(id), Arrays.stream(nestedStations));
                    } else {
                        return IntStream.of(id);
                    }
                })
                .toArray();
    }

    /**
     * Confirms the launch of a video on selected stations.
     * Checks for synchronization, modifies the list of selected stations if necessary, sends commands
     * to open the video player, waits for the video players to open, and updates the UI accordingly.
     *
     * @param selectedIds An array of selected station IDs
     */
    public void confirmLaunchVideo(int[] selectedIds) {
        Video selectedVideo = binding.getSelectedVideo();
        ArrayList<Integer> modifiedSelectedIds = new ArrayList<>();

        //TODO create a sync toggle
        //Check if sync has been enabled
        boolean sync = false;

        //Cycle through the selectedIds, if a station does not have the video player open send
        //the open command and add it to the awaitStationApplicationLaunch stationIds
        //If sync = true; Only send the source & sync command once all Stations have the video player open
        if (sync) {
            for (int id: selectedIds) {
                Station station = mViewModel.getStationById(id);
                //Check if the video player is active
                if (!station.applicationController.getExperienceName().equals(Constants.VideoPlayerName)) {
                    modifiedSelectedIds.add(station.getId());
                }
            }

            String stationIds = modifiedSelectedIds.stream().map(Object::toString).collect(Collectors.joining(", "));
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "PassToExperience");
                message.put("Trigger", "source," + selectedVideo.getSource());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            //Start a check and once all the Stations have the video player opened send the source
            startPeriodicChecks(modifiedSelectedIds, Constants.VideoPlayerName, () -> NetworkService.sendMessage("Station," +stationIds,"Experience", message.toString()));
        } else {
            for (int id: selectedIds) {
                Station station = mViewModel.getStationById(id);
                boolean ready = station.checkForVideoPlayer(selectedVideo);

                //If the video player was not ready.
                if (!ready) {
                    modifiedSelectedIds.add(station.getId());
                }
            }
        }

        //Wait for the video players to be open (if some of the Stations do not have it open)
        SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
        if (fragment == null) return;

        fragment.loadFragment(DashboardPageFragment.class, "dashboard", null);
        if (modifiedSelectedIds.isEmpty()) return;

        //Convert the ArrayList into an int[]
        int[] selectedIdsArray = new int[modifiedSelectedIds.size()];
        for (int i = 0; i < modifiedSelectedIds.size(); i++) {
            selectedIdsArray[i] = modifiedSelectedIds.get(i);
        }
        DialogManager.awaitStationApplicationLaunch(selectedIdsArray, mViewModel.getSelectedApplicationByName(Constants.VideoPlayerName), false);
    }

    /**
     * Initiates periodic checks for a certain condition that must be met by all supplied Stations.
     * This method schedules periodic checks to determine if a certain condition is met.
     *
     * @param selectedIds A list of Stations to check for the condition.
     * @param applicationName The application that is going to be launched.
     * @param callback The callback to be executed after opening the application.
     */
    private void startPeriodicChecks(ArrayList<Integer> selectedIds, String applicationName, IApplicationLoadedCallback callback) {
        final Runnable checkRunnable = new Runnable() {
            @Override
            public void run() {
                boolean ready = true;
                ArrayList<Integer> failedStationIds = new ArrayList<>();

                for (int id: selectedIds) {
                    Station station = mViewModel.getStationById(id);
                    //Check if the video player is active
                    if (!station.applicationController.getExperienceName().equals(applicationName)) {
                        failedStationIds.add(station.getId());
                        ready = false;
                    }
                }

                if (ready) {
                    //When it is set to the correct application, send the additional message
                    callback.onApplicationLoaded();
                    elapsedTime = 0; //reset
                    return;
                }
                // Check if timeout occurred
                if (elapsedTime >= TIMEOUT_DURATION) {
                    elapsedTime = 0; //reset

                    String stationIds = failedStationIds.stream().map(Object::toString).collect(Collectors.joining(", "));
                    MainActivity.runOnUI(() ->
                            DialogManager.createBasicDialog(
                                    "Experience launch failed",
                                    "Launch of " + applicationName + " failed on " + stationIds
                            )
                    );
                    return;
                }
                // Schedule the next check after the interval
                MainActivity.handler.postDelayed(this, CHECK_INTERVAL);
                elapsedTime += CHECK_INTERVAL;
            }
        };

        // Start the first check immediately
        MainActivity.handler.post(checkRunnable);
    }

    /**
     * Sets up a pin-like input mechanism for a group of EditText fields.
     *
     * @param view The view containing the EditText fields.
     */
    private void SetupEditText(View view) {
        editTexts = new EditText[]{
                view.findViewById(R.id.et1),
                view.findViewById(R.id.et2),
                view.findViewById(R.id.et3),
                view.findViewById(R.id.et4),
                view.findViewById(R.id.et5),
                view.findViewById(R.id.et6)
        };

        for (int i = 0; i < editTexts.length - 1; i++) {
            int finalI = i;
            editTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        editTexts[finalI + 1].requestFocus();
                    }
                }
            });
        }

        for (int i = 1; i < editTexts.length; i++) {
            int finalI = i;
            editTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (editTexts[finalI].getText().toString().isEmpty()) {
                        editTexts[finalI - 1].requestFocus();
                        return true;
                    }
                }
                return false;
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        ArrayList<Station> stations = (ArrayList<Station>) mViewModel.getStations().getValue();
        stations = (ArrayList<Station>) stations.clone();
        for (Station station:stations) {
            station.selected = false;
            mViewModel.updateStationById(station.id, station);
        }
    }

    private void loadFragments() {
        childManager.beginTransaction()
                .replace(R.id.station_selection_list_container, StationSelectionFragment.class, null)
                .commitNow();
    }
}
