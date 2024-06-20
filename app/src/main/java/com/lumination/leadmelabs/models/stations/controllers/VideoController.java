package com.lumination.leadmelabs.models.stations.controllers;

import android.util.Log;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.models.Video;
import com.lumination.leadmelabs.models.VrOption;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.sentry.Sentry;

public class VideoController {
    private final int stationId;

    //Track an active video
    private Video activeVideoFile;

    /**
     * The current playback time of the current video.
     */
    private int playbackTime;

    /**
     * The playback state of the current video (e.g., paused, stopped, playing).
     */
    private String playbackState;

    /**
     * Is the Video player set to repeat the videos when they finish.
     */
    private Boolean playbackRepeat = true;

    private boolean sliderTracking = false;
    private int sliderValue = 0;

    public String videosRaw;  //a string of the raw json information

    public List<Video> videos = new ArrayList<>();

    public VideoController(int stationId) {
        this.stationId = stationId;
    }

    /**
     * Parses JSON data to create a list of Video objects.
     * The JSON data should contain an array of objects with "name", "source", "length & "isVR" properties.
     *
     * @param jsonData The JSON data to parse.
     */
    public void setVideos(String jsonData) {
        List<Video> videos = new ArrayList<>();

        try {
            videosRaw = jsonData;
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
                            + ": VideoController - setVideos - " + e);
        }
    }

    /**
     * Get the current videos in string form for comparison against incoming data. The videos are
     * only updated if something has changed.
     * @return A string of the raw videos json as it was first received
     */
    public String getRawVideos() {
        return videosRaw;
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
     * Update the settings for the Video Player as they are received, these include:_isRepeat
     * 	    isRepeat - will the video loop
     * 		isMuted - is the video player muted
     * 		fullScreen - is the video player in full screen
     * 		videoState - the current playback state (Playing, Paused, Stopped)
     *
     * @param jsonData A string of Json data containing the latest video player settings.
     */
    public void updateVideoPlayerDetails(String jsonData) {
        try {
            JSONObject settingsJson = new JSONObject(jsonData);

            //Fallback values are the default at start up for the Video Player
            Boolean repeat = settingsJson.optBoolean("isRepeat", true);
            String state = settingsJson.optString("videoState", "");

            //Values below are not required yet
            //Boolean muted = settingsJson.optBoolean("isMuted", false);
            //Boolean fullScreen = settingsJson.optBoolean("fullScreen", true);

            //VR video values
            String scene = settingsJson.optString("scene", "");
            String projection = settingsJson.optString("projection", "");

            //Set the individual values
            this.setVideoPlaybackRepeat(repeat);
            this.setVideoPlaybackState(state);

            //Set the VR values
            this.setActiveScene(scene);
            this.setActiveProjection(projection);
        } catch (JSONException e) {
            Sentry.captureException(e);
        }
    }

    /**
     * Set whether a user is currently moving the slider for the video time control.
     * @param tracking A boolean of if the slider is being moved.
     */
    public void setSliderTracking(boolean tracking) {
        this.sliderTracking = tracking;
    }

    /**
     * As the user moves the slider, update this value so the playback messages from the video player
     * do not move the slider.
     * @param value An int of the current slider value.
     */
    public void setSliderValue(int value) {
        this.sliderValue = value;
    }

    /**
     * Retrieves the currently active video.
     *
     * @return The currently active video or null if there is no active video.
     */
    public Video getActiveVideoFile() {
        return activeVideoFile;
    }

    /**
     * Retrieves the length of the currently active video.
     *
     * @return The length of the currently active video in seconds, or 0 if there is no active video.
     */
    public int getVideoLength() {
        if (activeVideoFile == null) return 1;

        return activeVideoFile.getLength();
    }

    /**
     * Sets the currently active video based on its unique identifier.
     *
     * @param video The video to set as active.
     */
    public void setActiveVideo(Video video) {
        activeVideoFile = video;
    }

    /**
     * Update the video time as received from another source, this is most likely a video player
     * sending this information.
     * @param input A string of the time in seconds the video has played for, this is converted inside
     *             the function.
     */
    public void updateVideoPlaybackTime(String input) {
        if (activeVideoFile == null) return;
        if (Helpers.isNullOrEmpty(input) || input.equals("null")) return;
        if (Integer.parseInt(input) > getVideoLength()) return;

        try {
            this.playbackTime = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Log.e("Conversion", "Conversion failed. Input is not a valid integer.");
        }
    }

    /**
     * A user has set the time using the custom slider, update the value and send a message to the
     * video player to skip to this time.
     * @param time An int of the time in seconds the video should skip to.
     */
    public void setVideoPlaybackTime(int time) {
        if (activeVideoFile == null) return;

        this.playbackTime = time;
        trigger("time," + time);
    }

    /**
     * Get the current video playback time as an int
     * @return An int representing the time in seconds.
     */
    public int getVideoPlaybackTimeInt() {
        if (activeVideoFile == null) return 0;

        if (sliderTracking) {
            return sliderValue;
        } else {
            return playbackTime;
        }
    }

    /**
     * Get the current video playback time in the format of 00:00
     * @return A string in the format of 00:00.
     */
    public String getVideoPlaybackTimeString() {
        if (activeVideoFile == null) return "00:00";

        int time = playbackTime;
        int minutes = time / 60;
        int remainingSeconds = time % 60;

        return String.format(Locale.ENGLISH, "%02d:%02d", minutes, remainingSeconds);
    }

    public void setVideoPlaybackState(String state) {
        if (this.playbackState != null && this.playbackState.equals(state)) return;

        this.playbackState = state;
    }

    public void setVideoPlaybackRepeat(Boolean repeat) {
        if (this.playbackRepeat != null && this.playbackRepeat.equals(repeat)) return;

        this.playbackRepeat = repeat;
    }

    public Boolean getVideoPlaybackRepeat() {
        return this.playbackRepeat;
    }

    //region Triggers
    /**
     * Toggle if the Video player should repeat the videos or not. This is on by default.
     */
    public void repeatTrigger() {
        trigger("media,repeat");
    }

    /**
     * If the Video player has a source set, start playing the video if the state is paused or
     * stopped otherwise this does nothing.
     */
    public void playTrigger() {
        if (activeVideoFile == null) return;
        trigger("resume");
    }

    /**
     * If the Video player has a source set, pause the video.
     */
    public void pauseTrigger() {
        if (activeVideoFile == null) return;
        trigger("pause");
    }

    public void playPauseTrigger() {
        if (canPlay()) {
            playTrigger();
        } else if (canPause()) {
            pauseTrigger();
        }
    }

    /**
     * If the Video player has a source set, stop the video. This will return the playback time to
     * 00:00.
     */
    public void stopTrigger() {
        if (activeVideoFile == null) return;
        trigger("media,stop");
    }

    /**
     * If the Video player has a source set, skip the video x seconds forward or y seconds backwards.
     */
    public void skipTrigger(boolean forwards) {
        if (activeVideoFile == null) return;

        if (forwards) {
            trigger("media,skipForwards");
        } else {
            trigger("media,skipBackwards");
        }
    }

    /**
     * Set this videos source on the video player.
     */
    public void loadTrigger(String source) {
        trigger("source," + source);
    }

    /**
     * Send a message to the currently open Video Player. The message will be read by the internal
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

        Properties properties = new Properties();
        properties.put("classification", StationSingleFragment.segmentClassification);
        properties.put("stationId", this.stationId);
        properties.put("name", trigger);

        Segment.trackEvent(SegmentConstants.Video_Playback_Control, properties);
    }
    //endregion

    //region VR Video
    private String activeScene;
    private String activeProjection;

    public void setActiveScene(String scene) {
        this.activeScene = scene;
    }

    public String getActiveScene() {
        return this.activeScene;
    }

    public void setActiveProjection(String projection) {
        this.activeProjection = projection;
    }

    public String getActiveProjection() {
        return this.activeProjection;
    }

    /**
     * Depending on the VR scene, collect the associated projections that are allowed.
     * @param scene A string of the currently active scene.
     * @return A list of VrOptions that the scene can handle.
     */
    public List<VrOption> getProjectionOptions(String scene) {
        List<VrOption> projections = new ArrayList<>();
        switch (scene) {
            case "180":
                projections.add(new VrOption("MONO", "Monoscopic", "projection,VR180,MONO"));
                projections.add(new VrOption("OU", "Over Under", "projection,VR180,OU"));
                projections.add(new VrOption("SBS", "Side By Side", "projection,VR180,SBS"));
                break;

            case "360":
                projections.add(new VrOption("MONO", "Monoscopic", "projection,VR360,MONO"));
                projections.add(new VrOption("OU", "Over Under", "projection,VR360,OU"));
                projections.add(new VrOption("SBS", "Side By Side", "projection,VR360,SBS"));
                projections.add(new VrOption("EAC", "EAC", "projection,VR360,EAC"));
                projections.add(new VrOption("EAC3D", "EAC 3D", "projection,VR360,EAC3D"));
                break;

            case "Flat screen":
            default:
                break;
        }

        return projections;
    }
    //endregion

    //region Binding
    public String currentVideoName() {
        if (activeVideoFile == null || activeVideoFile.getName() == null) {
            return "No source set";
        }

        return activeVideoFile.getName();
    }

    public boolean canPlay() {
        if (activeVideoFile == null || playbackState == null) return false;

        return (playbackState.equals("Stopped") || playbackState.equals("Paused"));
    }

    public boolean canPause() {
        if (activeVideoFile == null || playbackState == null) return false;

        return playbackState.equals("Playing");
    }

    public boolean canStop() {
        if (activeVideoFile == null || playbackState == null) return false;

        return (playbackState.equals("Playing") || playbackState.equals("Paused"));
    }

    public boolean canSkip() {
        if (activeVideoFile == null || playbackState == null) return false;

        return (playbackState.equals("Playing") || playbackState.equals("Paused"));
    }
    //endregion
}
