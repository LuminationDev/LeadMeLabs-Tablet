package com.lumination.leadmelabs.models;

import java.util.ArrayList;
import java.util.List;

public class Notification {
    private String title;
    private List<String> messages;
    private String urgency;
    private String _timeStamp;
    private List<String> photoLinks;
    private String videoLink;

    public Notification() {
        // Default constructor required for calls to DataSnapshot.getValue(Notification.class)
    }

    // Getters and setters for each field
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String get_timeStamp() {
        return _timeStamp;
    }

    public void set_timeStamp(String timeStamp) {
        this._timeStamp = timeStamp;
    }

    public List<String> getPhotoLinks() {
        if (photoLinks == null) {
            return new ArrayList<>();
        }

        return photoLinks;
    }

    public void setPhotoLinks(List<String> photoLinks) {
        this.photoLinks = photoLinks;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public void setVideoLink(String videoLink) {
        this.videoLink = videoLink;
    }
}
