package com.lumination.leadmelabs.models.stations.controllers;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.applications.ReviveApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;
import com.lumination.leadmelabs.models.applications.information.Information;
import com.lumination.leadmelabs.models.applications.information.InformationConstants;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.utilities.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.sentry.Sentry;

public class ApplicationController {
    //NOTE: Cannot change *game* out of the variables without changing it on the NUC and Station as well
    private String gameName = null;
    private String gameId;
    private String gameType;

    public String applicationsRaw; //a string of the raw json information

    public ArrayList<Application> applications = new ArrayList<>();

    public ApplicationController(Object applications) {
        this.setApplications(applications);
    }

    //region Setters & Getters
    public void setExperienceName(String name) {
        this.gameName = name;
    }

    public String getExperienceName() {
        return this.gameName;
    }

    public void setExperienceId(String id) {
        this.gameId = id;
    }

    public String getExperienceId() {
        return this.gameId;
    }

    public void setExperienceType(String type) {
        this.gameType = type;
    }

    public String getExperienceType() {
        return this.gameType;
    }
    //endregion

    /**
     * Sets the applications for the station.
     *
     * @param applications The applications to be set. This can be either a JSON string
     *                     representing application data or a JSONArray containing application
     *                     objects.
     */
    private void setApplications(Object applications) {
        if (applications == null) return;
        if (Helpers.isNullOrEmpty(applications.toString())) return;

        if (applications instanceof JSONArray) {
            try {
                this.setApplicationsFromJson((JSONArray) applications);
            } catch (JSONException e) {
                Sentry.captureException(e);
            }
        } else {
            Sentry.captureMessage(
                    ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getLabLocation().getValue()
                    + ": ApplicationController - applications not sent in JSON format: " + applications);
        }
    }

    /**
     * Parses a JSON string containing application data and updates the list of applications.
     *
     * @param jsonArray The JSON array containing application data (JsonObjects).
     */
    public void setApplicationsFromJson(JSONArray jsonArray) throws JSONException {
        ArrayList<Application> newApplications = new ArrayList<>();

        // Iterate over each entry in the JSONArray using a traditional for loop
        for (int i = 0; i < jsonArray.length(); i++) {
            // Get the JSONObject at the current index
            JSONObject entry = jsonArray.getJSONObject(i);

            // Extract values for WrapperType, Name, Id, and IsVr
            String appType = entry.getString("WrapperType");
            String appName = entry.getString("Name");
            String appId = entry.getString("Id");
            boolean isVr = entry.getBoolean("IsVr");
            JSONObject appSubtype = entry.optJSONObject("Subtype");

            Application newApplication = createNewApplication(appType, appName, appId, isVr, appSubtype);
            if (newApplication == null) continue;
            newApplications.add(newApplication);
        }

        if (newApplications.isEmpty()) {
            return;
        }

        newApplications.sort((application, application2) -> application.name.compareToIgnoreCase(application2.name));
        this.applications = newApplications;

        //Check for missing thumbnails
        ImageManager.CheckLocalExperienceCache(jsonArray);
    }

    private Application createNewApplication(String appType, String appName, String appId, boolean isVr, JSONObject appSubtype) {
        Application temp = null;

        switch (appType) {
            case "Custom":
                temp = new CustomApplication(appType, appName, appId, isVr, appSubtype);
                break;
            case "Embedded":
                temp = new EmbeddedApplication(appType, appName, appId, isVr, appSubtype);
                break;
            case "Steam":
                temp = new SteamApplication(appType, appName, appId, isVr, appSubtype);
                break;
            case "Vive":
                temp = new ViveApplication(appType, appName, appId, isVr, appSubtype);
                break;
            case "Revive":
                temp = new ReviveApplication(appType, appName, appId, isVr, appSubtype);
                break;
        }

        //Collect the description, tags & yearLevels from the InformationConstants file
        Information information = InformationConstants.getValue(appId);
        if (temp != null && information != null) {
            temp.setInformation(information);
        }
        else if (temp != null) {
            //No information exists, add a default information class
            temp.setInformation(new Information(null,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    null
            ));
        }

        return temp;
    }

    /**
     * Detect if a particular station has an application installed on it
     * @param applicationId A long that represents the ID of an experience.
     * @return A boolean if the application is installed.
     */
    public boolean hasApplicationInstalled(String applicationId) {
        for (Application application:this.applications) {
            if (Objects.equals(application.id, applicationId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the application or the current gameId in the Station's applications list.
     *
     * @return The application with the current gameId if found, or null otherwise.
     */
    public Application findCurrentApplication() {
        Optional<Application> optionalApp = applications.stream()
                .filter(app -> Objects.equals(app.getId(), this.gameId))
                .findFirst();
        return optionalApp.orElse(null);
    }

    /**
     * Finds an application in the Station's applications list by its name.
     *
     * @return The application with the supplied name, or null otherwise.
     */
    public Application findApplicationByName(String name) {
        Optional<Application> optionalApp = applications.stream()
                .filter(app -> Objects.equals(app.getName(), name))
                .findFirst();
        return optionalApp.orElse(null);
    }

    /**
     * Retrieves all applications of a certain type.
     *
     * @param isVr A boolean representing whether to collect VR applications (true) or regular ones (false)
     * @return A list of Application objects.
     */
    public List<Application> getAllApplicationsByType(boolean isVr) {
        return applications.stream()
                .filter(app -> app.isVr == isVr)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Checks if the Station has an active experience.
     * @return True if the Station has an experience, false otherwise.
     */
    public boolean hasGame() {
        return this.gameName != null && !this.gameName.isEmpty() && !this.gameName.equals("null");
    }

    /**
     * Get the current installed applications in string form for comparison against incoming data. The
     * installed applications are only updated if something has changed.
     * @return A string of the raw installed applications json as it was first received
     */
    public String getRawInstalledApplications() {
        return applicationsRaw;
    }
}
