package com.lumination.leadmelabs.models;

/**
 * When a video is set as a source in the LeadMe Video Player or LeadMe VR Video Player, a message
 * is sent back to the tablet triggering the population the 'currentVideo' variable on a Station.
 * The Triggers for Play, Pause, Stop & Skip are always the same regardless of the Video application,
 * this is ensured by the leadme_api.dll
 */
public class Video {
    /**
     * The unique id of the video.
     */
    private final String id;

    /**
     * The name of the video.
     */
    private final String name;

    /**
     * The source of the video (e.g., URL, file path).
     */
    private final String source;

    /**
     * The total length of the video in seconds.
     */
    private final int length;

    /**
     * If the video has an associated subtitles file.
     */
    private final boolean hasSubtitles;

    private final String videoType;

    /**
     * Constructs a Video object with the specified name, source, playback state, length, and playback time.
     *
     * @param id            The unique Id of the video.
     * @param name          The name of the video.
     * @param source        The source of the video (e.g., URL, file path).
     * @param length        The total length of the video in seconds.
     * @param hasSubtitles  If the video has a subtitles file associated with it.
     * @param videoType     The type of video, (Normal, Vr, Backdrop, etc..)
     */
    public Video(String id, String name, String source, int length, boolean hasSubtitles, String videoType) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.length = length;
        this.hasSubtitles = hasSubtitles;
        this.videoType = videoType;
    }

    //region Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public int getLength() {
        return length;
    }

    public boolean getHasSubtitles() {
        return hasSubtitles;
    }

    public String getVideoType() {
        return videoType;
    }
    //endregion
}
