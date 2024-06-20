package com.lumination.leadmelabs.models.stations.controllers;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.adapters.FileAdapter;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.MultiStringCallbackInterface;
import com.lumination.leadmelabs.interfaces.StringCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.models.LocalFile;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.sentry.Sentry;

public class FileController {
    private static WeakReference<FileAdapter> fileAdapterRef;
    private static String currentFileCategory; //track what load dialog is open

    private String filesRaw;  //a string of the raw json information

    /**
     * Get the current files in string form for comparison against incoming data. The files are
     * only updated if something has changed.
     * @return A string of the raw files json as it was first received
     */
    public String getRawFiles() {
        return this.filesRaw;
    }

    /**
     * Sets files based on the provided file type and JSON data.
     * This method processes the JSON data according to the specified file type.
     * If the file type is "Videos", it delegates to the `setVideos` method.
     * For other file types, it parses the JSON data to extract keys and handles
     * specific keys accordingly.
     *
     * @param fileType The type of files to set (e.g., "Videos", "LocalFiles").
     * @param jsonData The JSON data string containing file information.
     */
    public void setFiles(String fileType, String jsonData) {
        if (fileType.equals("Videos")) {
            setVideos(jsonData);
            return;
        }

        try {
            filesRaw = jsonData;
            JSONObject jsonObject = new JSONObject(jsonData);

            // Iterate through the keys
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray jsonArray = jsonObject.getJSONArray(key);

                //Expand later with more file options
                if (key.equals(Constants.OPEN_BRUSH_FILE)) {
                    setOpenBrushFiles(jsonArray);

                    //Update the file adapter - if open
                    if (key.equals(currentFileCategory)) {
                        notifyAdapterDataChanged(this.openBrushFiles);
                    }
                } else {
                    Log.e("File Controller", "Unknown local file key: " + key);
                }
            }
        } catch (JSONException e) {
            Log.e("File Controller", e.toString());
        }
    }

    /**
     * Send a message to the Station to delete a local file.
     * @param stationId An Id of the Station to send the refresh message.
     * @param fileName A string of the file name for deletion confirmation on the station.
     * @param filePath A string of the absolute file path on the station.
     */
    public void deleteFile(int stationId, String fileName, String filePath) {
        JSONObject message = new JSONObject();
        try {
            message.put("Action", "delete");
            message.put("fileName", fileName);
            message.put("filePath", filePath);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        NetworkService.sendMessage("Station," + stationId, "FileControl", message.toString());
    }

    /**
     * Manually refresh the Station's file list.
     * @param stationId An Id of the Station to send the refresh message.
     */
    public void refreshFiles(int stationId) {
        JSONObject message = new JSONObject();
        try {
            message.put("Action", "refresh");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        NetworkService.sendMessage("Station," + stationId, "FileControl", message.toString());
    }

    //region Open Brush files
    private ArrayList<LocalFile> openBrushFiles;

    public ArrayList<LocalFile> getOpenBrushFiles() {
        return this.openBrushFiles;
    }

    /**
     * Sets the OpenBrush files based on the provided JSON array.
     * This method parses a JSON array containing file information and constructs
     * a list of `LocalFile` objects. Each `LocalFile` object is created from the
     * data in the JSON array and added to a temporary list. The temporary list
     * is then assigned to the `openBrushFiles` field.
     *
     * @param jsonArray The JSON array containing file information for OpenBrush files.
     * @throws JSONException If there is an error parsing the JSON data.
     */
    public void setOpenBrushFiles(JSONArray jsonArray) throws JSONException {
        ArrayList<LocalFile> temp = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject fileObject = jsonArray.getJSONObject(i);
            String fileType = fileObject.getString("fileType");
            String name = fileObject.getString("name");
            String path = fileObject.getString("path");

            LocalFile tempFile = new LocalFile(fileType, name, path);
            temp.add(tempFile);
        }

        this.openBrushFiles = temp;
    }
    //endregion

    //region Video Files
    public List<Video> videos = new ArrayList<>();

    /**
     * Parses JSON data to create a list of Video objects.
     * The JSON data should contain an array of objects with "name", "source", "length & "isVR" properties.
     *
     * @param jsonData The JSON data to parse.
     */
    public void setVideos(String jsonData) {
        List<Video> videos = new ArrayList<>();

        try {
            JSONArray devices = new JSONArray(jsonData);

            for (int i = 0; i < devices.length(); i++) {
                JSONObject videoJson = devices.getJSONObject(i);

                String id = videoJson.optString("id", "");
                String name = videoJson.optString("name", "");
                String source = videoJson.optString("source", "");
                int length = videoJson.optInt("length", 0);
                boolean hasSubtitles = videoJson.optBoolean("hasSubtitles", false);
                String videoType = videoJson.optString("videoType", "Normal");
                if (name.isEmpty() || source.isEmpty()) continue;

                Video temp = new Video(id, name, source, length, hasSubtitles, videoType);
                videos.add(temp);
            }

            this.videos = videos;

            //Check for missing thumbnails
            ImageManager.CheckLocalVideoCache(devices);
        } catch (JSONException e) {
            Sentry.captureMessage(
                    ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getLabLocation().getValue()
                            + ": FileController - setVideos - " + e);
        }
    }

    /**
     * Finds a video by its unique identifier.
     *
     * @param id The unique identifier of the video to find.
     * @return The video with the specified ID if found; otherwise, returns null.
     * @example Video video = findVideoById("123");
     */
    public Video findVideoById(String id) {
        if (videos == null) {
            return null;
        }

        return videos.stream()
                .filter(video -> video.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Collect all the videos that are of a certain type.
     * @return A List of videos matching the supplied type.
     */
    public List<Video> getVideosOfType(String type) {
        return videos.stream()
                .filter(video -> video.getVideoType().equals(type))
                .collect(Collectors.toList());
    }

    /**
     * Detect if a particular station has a video on it
     * @param checkVideo A Video object to check for.
     * @return A boolean if the a video exists.
     */
    public Boolean hasLocalVideo(Video checkVideo) {
        if (checkVideo == null) return false;

        for (Video video:this.videos) {
            if (Objects.equals(video.getId(), checkVideo.getId())) {
                return true;
            }
        }
        return false;
    }
    //endregion

    //region File saving & loading
    /**
     * Sets the FileAdapter instance in a WeakReference.
     * This method should be called to initialise or update the FileAdapter reference.
     *
     * @param fileAdapter The FileAdapter instance to be managed.
     */
    private static void setFileAdapter(String fileCategory, FileAdapter fileAdapter) {
        currentFileCategory = fileCategory;
        fileAdapterRef = new WeakReference<>(fileAdapter);
    }

    /**
     * Notifies the FileAdapter that the data set has changed.
     * This method should be called whenever the underlying data changes and the adapter needs to
     * refresh the UI.
     */
    private void notifyAdapterDataChanged(ArrayList<LocalFile> localFiles) {
        FileAdapter fileAdapter = fileAdapterRef != null ? fileAdapterRef.get() : null;
        if (fileAdapter != null) {
            fileAdapter.Update(localFiles);
        }
    }

    private static final Set<Character> INVALID_CHARACTERS = new HashSet<>(Arrays.asList(
            '\\', '/', ':', '*', '?', '"', '<', '>', '|'
    ));

    /**
     * Check if a supplied file name will be valid for a computer.
     * @param fileName A string of the proposed file name.
     * @return A boolean of if it is valid (true), or not valid (false)
     */
    public static boolean isFileNameValid(String fileName) {
        if (Helpers.isNullOrEmpty(fileName)) {
            return false;
        }

        for (char ch : fileName.toCharArray()) {
            if (INVALID_CHARACTERS.contains(ch)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a dialog with an input text for saving a file.
     * @param stringCallbackInterface A callback that returns the input string.
     */
    public static void buildSaveFileDialog(StringCallbackInterface stringCallbackInterface) {
        Context context = MainActivity.getInstance();
        View view = View.inflate(context, R.layout.dialog_file_save, null);
        AlertDialog saveDialog = new AlertDialog.Builder(context).setView(view).create();

        EditText fileName = view.findViewById(R.id.file_name_input);
        fileName.requestFocus();
        TextView errorText = view.findViewById(R.id.error_text);

        Button submit = view.findViewById(R.id.submit_button);
        submit.setOnClickListener(v -> {
            errorText.setVisibility(View.GONE);
            String input = fileName.getText().toString();

            //Check for invalid characters
            if (isFileNameValid(input)) {
                if(stringCallbackInterface != null) {
                    stringCallbackInterface.callback(input);
                    saveDialog.dismiss();
                } else {
                    errorText.setText(R.string.unknown_error_please_save_manually);
                    errorText.setVisibility(View.VISIBLE);
                }
            } else {
                errorText.setText(R.string.invalid_file_name);
                errorText.setVisibility(View.VISIBLE);
            }
        });

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> saveDialog.dismiss());

        saveDialog.show();
    }

    /**
     * Build the file loading dialog.
     * @param stationId A integer of the Station Id to load the file for.
     * @param fileCategory A string of the category (application) for instantiating the available files
     *                     for.
     * @param multiStringCallbackInterface A callback that returns the selected files name and
     *                                     its absolute path.
     */
    public static void buildLoadFileDialog(int stationId, String fileCategory, MultiStringCallbackInterface multiStringCallbackInterface) {
        Context context = MainActivity.getInstance();
        View view = View.inflate(context, R.layout.dialog_file_load, null);
        AlertDialog loadDialog = new AlertDialog.Builder(context).setView(view).create();

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> loadDialog.dismiss());

        TextView errorText = view.findViewById(R.id.error_text);
        TextView selectedItemTextView = view.findViewById(R.id.selectedItemTextView);

        //Build the adapter based on the supplied Station Id and file type
        Station station = StationsFragment.mViewModel.getStationById(stationId);
        if (station == null) {
            errorText.setText(R.string.unable_to_load_station_details);
            errorText.setVisibility(View.VISIBLE);
            loadDialog.show();
            return;
        }

        ArrayList<LocalFile> localFiles;
        if (fileCategory.equals(Constants.OPEN_BRUSH_FILE)) {
            localFiles = station.fileController.getOpenBrushFiles();
        } else {
            //No other file categories implemented yet
            errorText.setText(String.format("No files available for %s", fileCategory));
            errorText.setVisibility(View.VISIBLE);
            loadDialog.show();
            return;
        }

        if (localFiles == null) {
            errorText.setText(String.format("No files available for %s", fileCategory));
            errorText.setVisibility(View.VISIBLE);
            loadDialog.show();
            return;
        }

        //Set the file adapter with the supplied files
        RecyclerView recyclerView = view.findViewById(R.id.local_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        FileAdapter fileAdapter = new FileAdapter(context, selectedItemTextView, localFiles);
        setFileAdapter(fileCategory, fileAdapter); //setup the weak reference
        recyclerView.setAdapter(fileAdapter);

        Button load = view.findViewById(R.id.load_button);
        load.setOnClickListener(v -> {
            errorText.setVisibility(View.GONE);

            //Check if something is selected
            if (fileAdapter.getSelectedFileName() == null || fileAdapter.getSelectedFilePath() == null) {
                errorText.setText(R.string.file_must_be_selected);
                errorText.setVisibility(View.VISIBLE);
            } else if(multiStringCallbackInterface != null) {
                multiStringCallbackInterface.callback(fileAdapter.getSelectedFileName(), fileAdapter.getSelectedFilePath());
                loadDialog.dismiss();
            } else {
                errorText.setText(R.string.unknown_error_please_save_manually);
                errorText.setVisibility(View.VISIBLE);
            }
        });

        Button refresh = view.findViewById(R.id.refresh_button);
        refresh.setOnClickListener(v -> station.fileController.refreshFiles(stationId));

        Button delete = view.findViewById(R.id.delete_button);
        delete.setOnClickListener(v -> {
            if (fileAdapter.getSelectedFileName() == null || fileAdapter.getSelectedFilePath() == null) {
                errorText.setText(R.string.file_must_be_selected);
                errorText.setVisibility(View.VISIBLE);
                return;
            }

            errorText.setVisibility(View.GONE);
            String selectedFile = fileAdapter.getSelectedFileName();

            BooleanCallbackInterface confirmDeleteFileCallback = confirmationResult -> {
                if (confirmationResult) {
                    station.fileController.deleteFile(stationId,
                            fileAdapter.getSelectedFileName(),
                            fileAdapter.getSelectedFilePath());
                }
            };
            DialogManager.createConfirmationDialog("Are you sure?", "This will permanently delete the file: " + selectedFile, confirmDeleteFileCallback);
        });

        //Remove the weak reference
        loadDialog.setOnDismissListener(v -> setFileAdapter(null, null));
        loadDialog.show();
    }
    //endregion
}
