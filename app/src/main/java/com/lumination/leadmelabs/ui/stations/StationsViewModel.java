package com.lumination.leadmelabs.ui.stations;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.stations.StatusManager;
import com.lumination.leadmelabs.models.stations.VrStation;
import com.lumination.leadmelabs.models.applications.details.Actions;
import com.lumination.leadmelabs.models.applications.details.Details;
import com.lumination.leadmelabs.models.applications.details.Levels;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StationsViewModel extends ViewModel {
    private MutableLiveData<List<Station>> stations;
    private MutableLiveData<Station> selectedStation = new MutableLiveData<>();
    private MutableLiveData<Integer> selectedStationId = new MutableLiveData<>();

    private MutableLiveData<String> selectionType = new MutableLiveData<>("application");
    private MutableLiveData<Application> selectedApplication = new MutableLiveData<>();
    private MutableLiveData<String> selectedApplicationId = new MutableLiveData<>();
    private MutableLiveData<Video> selectedVideo = new MutableLiveData<>();

    //region Stations
    public LiveData<List<Station>> getStations() {
        if (stations == null) {
            stations = new MutableLiveData<>();
            loadStations();
        }
        return stations;
    }

    public List<String> getStationNames(int[] stationIds) {
        ArrayList<Station> stations = (ArrayList<Station>) this.getStations().getValue();
        if (stations == null) return new ArrayList<>();

        stations = new ArrayList<>(stations);
        List<Integer> stationIdsList =  new ArrayList<>(stationIds.length);
        for (int i : stationIds)
        {
            stationIdsList.add(i);
        }
        stations.removeIf(station -> !stationIdsList.contains(station.id));
        return stations.stream().map(station -> station.name).collect(Collectors.toList());
    }

    public int[] getSelectedStationIds() {
        ArrayList<Station> selectedStations = getSelectedStations();
        int[] selectedIds = new int[selectedStations.size()];
        for (int i = 0; i < selectedStations.size(); i++) {
            selectedIds[i] = selectedStations.get(i).id;
        }
        return selectedIds;
    }

    public int[] getAllStationIds() {
        ArrayList<Station> stations = (ArrayList<Station>) getStations().getValue();
        if (stations == null) {
            return new int[0];
        }
        int[] ids = new int[stations.size()];
        for (int i = 0; i < stations.size(); i++) {
            ids[i] = stations.get(i).id;
        }
        return ids;
    }

    public ArrayList<Station> getSelectedStations() {
        ArrayList<Station> selectedStations = (ArrayList<Station>) stations.getValue();
        if (selectedStations == null) return new ArrayList<>();

        selectedStations = new ArrayList<>(selectedStations);
        selectedStations.removeIf(station -> !station.selected);
        return selectedStations;
    }

    public Station getStationById(int id) {
        ArrayList<Station> stationsData = (ArrayList<Station>) stations.getValue();
        if (stationsData == null) {
            return null;
        }
        for (Station station:stationsData) {
            if (station.id == id) {
                return station;
            }
        }
        return null;
    }

    /**
     * Turn the JSON object that is supplied into the Details class with its associated Global actions,
     * levels and level specific actions.
     * @param JSONvalue A JSON object that has been sent from a Station.
     * @return A Details class instantiation
     */
    private Details parseDetails(JSONObject JSONvalue) throws JSONException {
        Details details = new Details(JSONvalue.getString("name"));

        JSONArray globalActions = JSONvalue.getJSONArray("globalActions");
        for (int i = 0; i < globalActions.length(); i++) {
            JSONObject temp = globalActions.getJSONObject(i);
            details.addGlobalAction(new Actions(temp.getString("name"), temp.getString("trigger")));
        }

        JSONArray levels = JSONvalue.getJSONArray("levels");
        for (int i = 0; i < levels.length(); i++) {
            JSONObject temp = levels.getJSONObject(i);
            Levels level = new Levels(temp.getString("name"), temp.getString("trigger"));

            //Add the level trigger as it's first action
            if(!level.trigger.equals("")) {
                level.addAction(new Actions("Set", level.trigger));
            }

            //Add the rest of the actions
            JSONArray actions = temp.getJSONArray("actions");
            for (int y = 0; y < actions.length(); y++) {
                JSONObject action = actions.getJSONObject(y);
                level.addAction(new Actions(action.getString("name"), action.getString("trigger")));
            }

            details.addLevels(level);
        }

        return details;
    }

    /**
     * Retrieves all applications across all stations, ensuring each application's ID is unique.
     * Applications are sorted alphabetically by name.
     * @return A list of Application objects.
     */
    public List<Application> getAllApplications() {
        HashSet<String> idSet = new HashSet<>();

        if(stations.getValue() == null) {
            return new ArrayList<>();
        }

        //Only add applications with a unique id
        return stations.getValue().stream()
                .flatMap(station -> station.applicationController.applications.stream())
                .filter(app -> idSet.add(app.id))
                .distinct()
                .sorted((application, application2) -> application.name.compareToIgnoreCase(application2.name))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Retrieves all applications across all stations, ensuring each application's ID is unique.
     * Applications are sorted alphabetically by name.
     *
     * @param isVr A boolean representing whether to collect VR applications (true) or regular ones (false)
     * @return A list of Application objects.
     */
    public List<Application> getAllApplicationsByType(boolean isVr) {
        HashSet<String> idSet = new HashSet<>();

        if(stations.getValue() == null) {
            return new ArrayList<>();
        }

        //Only add applications with a unique id
        return stations.getValue().stream()
                .flatMap(station -> station.applicationController.applications.stream())
                .filter(app -> idSet.add(app.id))
                .filter(app -> app.isVr == isVr)
                .distinct()
                .sorted((application, application2) -> application.name.compareToIgnoreCase(application2.name))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Retrieves applications associated with a specific station, subject to any room locks.
     * Applications are sorted alphabetically by name.
     *
     * @param stationId The ID of the station to retrieve applications from.
     * @param isVr A boolean representing whether to collect VR applications (true) or regular ones (false)
     * @return A list of Application objects.
     */
    public List<Application> getStationApplications(int stationId, boolean isVr) {
        ArrayList<Application> list = new ArrayList<>();
        if(stations.getValue() == null) {
            return list;
        }
        for (Station station: stations.getValue()) {
            if (station.id == stationId) {
                if(SettingsFragment.checkLockedRooms(station.room)) {
                    // Iterate through applications and add only those with isVr = true
                    for (Application application : station.applicationController.applications) {
                        if (application.isVr == isVr) {
                            list.add(application);
                        }
                    }
                    list.sort((application, application2) -> application.name.compareToIgnoreCase(application2.name));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all videos across all stations, ensuring each video's ID is unique.
     * Videos are sorted alphabetically by name.
     * @return A list of Video objects.
     */
    public List<Video> getAllVideos() {
        HashSet<String> idSet = new HashSet<>();

        if(stations.getValue() == null) {
            return new ArrayList<>();
        }

        //Only add applications with a unique id
        return stations.getValue().stream()
                .flatMap(station -> station.videoController.videos.stream())
                .filter(video -> idSet.add(video.getId()))
                .distinct()
                .sorted((video, video2) -> video.getName().compareToIgnoreCase(video2.getName()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Retrieves videos associated with a specific station, subject to certain conditions.
     * Videos are sorted alphabetically by name.
     * @param stationId The ID of the station to retrieve videos from.
     * @return A list of Video objects.
     */
    public List<Video> getStationVideos(int stationId) {
        ArrayList<Video> list = new ArrayList<>();
        if(stations.getValue() == null) {
            return list;
        }
        for (Station station: stations.getValue()) {
            if (station.id == stationId) {
                if(SettingsFragment.checkLockedRooms(station.room)) {
                    list = new ArrayList<>(station.videoController.videos);
                    list.sort((video, video2) -> video.getName().compareToIgnoreCase(video2.getName()));
                }
            }
        }
        return list;
    }

    /**
     * Updates the data of a station with the provided ID.
     * @param id The ID of the station to update.
     * @param station The updated Station object.
     */
    public void updateStationById(int id, Station station) {
        List<Station> stationsData = stations.getValue();
        if (stationsData == null) return;

        int index = -1;
        for(int i = 0; i < stationsData.size(); i++) {
            if (stationsData.get(i).id == id) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            stationsData.set(index, station);
            stations.setValue((stationsData));
        }
        if (getSelectedStation().getValue() != null && id == getSelectedStation().getValue().id) {
            setSelectedStation(station);
        }
    }

    /**
     * A message has been sent from the NUC determine which station has values that are changing.
     * The values for the computer will be in CBUS format i.e. numbers. Therefore run a switch case
     * to determine what the status should be.
     */
    public void syncStationStatus(String id, String status, String ipAddress) {
        Station station = StationsFragment.mViewModel.getStationById(Integer.parseInt(id));

        //Exit the function if the tablet is in wall mode.
        if(Boolean.TRUE.equals(SettingsFragment.mViewModel.getHideStationControls().getValue())) {
            return;
        }

        //Disregard the message if from the same IP address
        if(NetworkService.getIPAddress().equals(ipAddress)) {
            return;
        }

        //If the station is on and the message is restarting then the status should be restarting
        //else the station is off and the message is restarting then the status should be turning on
        if(station.isOff()) {
            status = StatusManager.TURNING_ON;
        }

        //Nothing to update or the station is already on and is not restarting.
        if(station.getStatus().equals(status) || (station.isOn() && !Objects.equals(status, StatusManager.RESTARTING))) {
            return;
        }

        String finalStatus = status;
        MainActivity.runOnUI(() -> {
            station.statusManager.powerStatusCheck(station.getId(),3 * 1000 * 60);
            station.setStatus(finalStatus);
            StationsFragment.mViewModel.updateStationById(Integer.parseInt(id), station);
        });
    }

    /**
     * Run through the Station list finding the one that matches the supplied ID. This is now set as
     * the selected station. If this station is running an experience it checks whether there are
     * details associated with that experience.
     *
     * @param id An int representing the Station number.
     */
    public void selectStation(int id) {
        selectStationById(id);
        if (getSelectedStation().getValue() != null) {
            selectApplicationByGameName(getSelectedStation().getValue().applicationController.getExperienceName());
        }
        getSelectedStation();
    }

    private void selectStationById(int id) {
        List<Station> stationList = stations.getValue();
        if (stationList == null) return;

        List<Station> filteredStations = stationList.stream()
                .filter(station -> station.id == id)
                .collect(Collectors.toList());

        if (!filteredStations.isEmpty()) {
            setSelectedStation(filteredStations.get(0));
        }
    }

    public void setSelectedStation(Station station) {
        this.selectedStation.setValue(station);
    }

    public LiveData<Station> getSelectedStation() {
        if (selectedStation == null) {
            selectedStation = new MutableLiveData<>();
        }
        return selectedStation;
    }

    public void setStations(JSONArray stations) throws JSONException {
        List<Station> st = new ArrayList<>();
        for (int i = 0; i < stations.length(); i++) {
            JSONObject stationJson = stations.getJSONObject(i);

            Station station = StationFactory.createStation(stationJson);
            if (station == null) continue;

            st.add(station);
        }
        this.setStations(st);

        //Ask for the device statuses
        for (Station station: Objects.requireNonNull(this.stations.getValue())) {
            NetworkService.sendMessage("Station," + station.id, "Station", "GetValue:devices");
        }
    }

    public void setStations(List<Station> stations) {
        this.stations.setValue(stations);
    }

    public void loadStations() {
        NetworkService.sendMessage("NUC", "Stations", "List");
    }

    public void setSelectedStationId(int stationId) {
        LibrarySelectionFragment.setStationId(stationId);
        this.selectedStationId.setValue(stationId);
    }
    public LiveData<Integer> getSelectedStationId() {
        if (selectedStationId == null) {
            selectedStationId = new MutableLiveData<>(0);
        }

        return selectedStationId;
    }

    /**
     * Start of Stop the VR icons flashing on an associated station.
     *
     * @param flashing A boolean representing if the icon should start (true) or stop (false)
     *                 flashing.
     */
    public void setStationFlashing(int stationId, boolean flashing) {
        Station station = getStationById(stationId);

        if (!(station instanceof VrStation)) {
            return;
        }

        VrStation vrStation = (VrStation) getStationById(stationId);
        if(vrStation == null) return;
        vrStation.animationFlag = flashing;
        vrStation.handleAnimationTimer();
        updateStationById(stationId, vrStation);
    }
    //endregion

    public LiveData<String> getSelectionType() {
        if (this.selectionType == null) {
            this.selectionType = new MutableLiveData<>("application");
        }

        return this.selectionType;
    }

    public void setSelectionType(String type) {
        this.selectionType.setValue(type);
    }

    //region Applications
    /**
     * Set the details of an application on a specified experience. The details can include, global
     * actions and different levels. As there is no full list of experiences it needs to set the
     * details for each instance of that experience. I.e one for each station it is installed on.
     * @param applicationDetails A string representing the name of the application to update.
     */
    public void setApplicationDetails(JSONObject applicationDetails) throws JSONException {
        if(stations.getValue() == null) {
            return;
        }

        //Deconstruct the JSON object into the new details class and associated subclasses.
        Details details = parseDetails(applicationDetails);

        for (Station station: stations.getValue()) {
            //Check each station for the experience
            for (Application application: station.applicationController.applications) {
                if (Objects.equals(application.name, details.name)) {
                    application.details = details;

                    if(station.applicationController.getExperienceName() != null) {
                        // Set as the selected application if this Station currently has it launched
                        if (station.applicationController.getExperienceName().equals(application.name)) {
                            this.setSelectedApplication(application);
                        }
                    }
                }
            }
        }
    }

    public String getSelectedApplicationId() {
        return selectedApplicationId.getValue();
    }

    public String getSelectedApplicationName(String applicationId) {
        List<Application> allApps = getAllApplications();
        allApps.removeIf(application -> !Objects.equals(application.id, applicationId));
        if(allApps.size() == 0) return "experience";
        return allApps.get(0).name;
    }

    public String getSelectedApplicationByName(String name) {
        List<Application> allApps = getAllApplications();
        allApps.removeIf(application -> !Objects.equals(application.getName(), name));
        if(allApps.size() == 0) return "experience";
        return allApps.get(0).name;
    }

    public void selectSelectedApplication(String id) {
        if (this.selectedApplicationId == null) {
            this.selectedApplicationId = new MutableLiveData<>();
        }

        this.selectedApplicationId.setValue(id);
    }

    private void selectApplicationByGameName(String gameName) {
        Station selectedStation = getSelectedStation().getValue();
        if(selectedStation == null) return;

        if (selectedStation.applicationController.applications != null) {
            Optional<Application> selectedApplication = selectedStation.applicationController.applications.stream()
                    .filter(application -> Objects.equals(application.name, gameName))
                    .findFirst();

            selectedApplication.ifPresent(this::setSelectedApplication);
            selectedApplication.orElseGet(() -> {
                this.setSelectedApplication(null);
                return null;
            });
        } else {
            this.setSelectedApplication(null);
        }
    }

    public void setSelectedApplication(Application application) {
        this.selectedApplication.setValue(application);
    }

    public LiveData<Application> getSelectedApplication() {
        if (selectedApplication == null) {
            selectedApplication = new MutableLiveData<>();
        }
        return selectedApplication;
    }
    //endregion

    //region Videos
    public LiveData<Video> getSelectedVideo() {
        return this.selectedVideo;
    }

    public void setSelectedVideo(Video video) {
        if (this.selectedVideo == null) {
            this.selectedVideo = new MutableLiveData<>();
        }

        this.selectedVideo.setValue(video);
    }
    //endregion

    //region Layouts
    private MutableLiveData<String> layoutTab = new MutableLiveData<>("layouts");
    public MutableLiveData<String> getLayoutTab() {
        if (layoutTab == null) {
            layoutTab = new MutableLiveData<>("layouts");
        }
        return layoutTab;
    }

    public void setLayoutTab(String libraryType) {
        this.layoutTab.setValue(libraryType);
    }
    //endregion
}
