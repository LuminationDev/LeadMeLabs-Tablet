package com.lumination.leadmelabs.models.stations.controllers;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;

import org.json.JSONException;
import org.json.JSONObject;

public class OpenBrushController {
    private final int stationId;

    //Track if the active file is saved
    private String activeFile;

    public void setActiveFile(String fileName) {
        this.activeFile = fileName;
    }

    public OpenBrushController(int stationId) {
        this.stationId = stationId;
    }

    //region Triggers
    public void newSketchTrigger() {
        setActiveFile(null);
        trigger("canvas,new");
    }

    /**
     *
     */
    public void saveFile() {
        FileController.buildSaveFileDialog(this::saveTrigger);
    }

    private void saveTrigger(String name) {
        trigger("option,save," + name);
    }

    /**
     *
     */
    public void loadFile() {
        FileController.buildLoadFileDialog(stationId, Constants.OPEN_BRUSH_FILE, this::loadTrigger);
    }

    private void loadTrigger(String name, String path) {
        setActiveFile(name);
        trigger("option,load," + path);
    }

    public void reloadTrigger() {
        BooleanCallbackInterface confirmCallback = confirmationResult -> {
            if (confirmationResult) {
                setActiveFile(null);
                trigger("canvas,reload");
            }
        };
        DialogManager.createConfirmationDialog("Are you sure?", "This will clear any unsaved work!", confirmCallback);
    }

    public void undoTrigger() {
        trigger("canvas,undo");
    }

    public void redoTrigger() {
        trigger("canvas,redo");
    }

    public void clearTrigger() {
        BooleanCallbackInterface confirmCallback = confirmationResult -> {
            if (confirmationResult) {
                trigger("canvas,clear");
            }
        };
        DialogManager.createConfirmationDialog("Are you sure?", "This will clear any unsaved work!", confirmCallback);
    }

    public void centerTrigger() {
        BooleanCallbackInterface confirmCallback = confirmationResult -> {
            if (confirmationResult) {
                trigger("camera,center");
            }
        };
        DialogManager.createConfirmationDialog("Are you sure?", "This will move the user back to the starting position!", confirmCallback);
    }

    /**
     * Send a message to Open Brush. The message will be read by the internal
     * leadme_api and the action propagated to it's controller.
     * @param trigger A string of what the application should do.
     */
    public void trigger(String trigger) {
        if (MainActivity.isNucJsonEnabled) {
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "PassToExperience");
                message.put("Trigger", trigger);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            NetworkService.sendMessage("Station," + this.stationId, "Experience", message.toString());
        }
        else {
            NetworkService.sendMessage("Station," + this.stationId, "Experience", "PassToExperience:" + trigger);
        }

//        Properties properties = new Properties();
//        properties.put("classification", StationSingleFragment.segmentClassification);
//        properties.put("stationId", this.stationId);
//        properties.put("name", trigger);
//
//        Segment.trackEvent(SegmentConstants.Open_Brush_Control, properties);
    }
    //endregion

    //region Binding
    public String currentFileName() {
        if (Helpers.isNullOrEmpty(activeFile)) {
            return "No file loaded";
        }

        return activeFile;
    }
    //endregion
}
