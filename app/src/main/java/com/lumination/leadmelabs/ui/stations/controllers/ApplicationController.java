package com.lumination.leadmelabs.ui.stations.controllers;

import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.applications.ReviveApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import io.sentry.Sentry;

public class ApplicationController {
    private String gameName = null;
    private String gameId;
    private String gameType;

    public ArrayList<Application> applications = new ArrayList<>();

    public ApplicationController(Object applications) {
        this.setApplications(applications);
    }

    //region Setters & Getters
    public void setGameName(String name) {
        this.gameName = name;
    }

    public String getGameName() {
        return this.gameName;
    }

    public void setGameId(String id) {
        this.gameId = id;
    }

    public String getGameId() {
        return this.gameId;
    }

    public void setGameType(String type) {
        this.gameType = type;
    }

    public String getGameType() {
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

        if (applications instanceof String) {
            this.setApplicationsFromJsonString((String) applications);
        } else if (applications instanceof JSONArray) {
            try {
                this.setApplicationsFromJson((JSONArray) applications);
            } catch (JSONException e) {
                Sentry.captureException(e);
            }
        }
    }

    //BACKWARDS COMPATIBILITY
    /**
     * Parses a JSON string containing application data and updates the list of applications.
     *
     * @param applicationsJson The JSON string containing application data (string list).
     */
    public void setApplicationsFromJsonString(String applicationsJson) {
        // Wrap with a catch for unforeseen characters - this will method will be removed in the future
        try {
            ArrayList<Application> newApplications = new ArrayList<>();
            String[] apps = applicationsJson.split("/");

            for (String app : apps) {
                String[] appData = app.split("\\|");
                if (appData.length <= 1) continue;

                String appType = appData[0];
                String appName = appData[2].replace("\"", "");
                String appId = appData[1];
                boolean isVr = appData.length >= 4 && Boolean.parseBoolean(appData[3]); // Backwards compatibility
                String subtype = appData.length >= 5 ? appData[4]: ""; // Backwards compatibility

                JSONObject appSubtype = null;
                try {
                    appSubtype = new JSONObject(subtype);
                }
                catch (Exception ignored) {}

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
            ImageManager.CheckLocalCache(applicationsJson);
        } catch (Exception e) {
            Sentry.captureException(e);
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
        ImageManager.CheckLocalCache(jsonArray);
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
}
