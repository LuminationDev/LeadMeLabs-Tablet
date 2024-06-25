package com.lumination.leadmelabs.models;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Notification {
    private String key;
    private String title;
    private List<String> messages;
    private String urgency;
    private String _timeStamp;
    private List<String> photoLinks;
    private String videoLink;
    private String status;

    public Notification() {
        // Default constructor required for calls to DataSnapshot.getValue(Notification.class)
    }

    // Getters and setters for each field
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

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

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public String transformDateFormat() {
        SimpleDateFormat oldFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        SimpleDateFormat newFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

        try {
            Date date = oldFormat.parse(this._timeStamp);
            return newFormat.format(date);
        } catch (ParseException e) {
            Log.e("Notification", "Invalid date format:" + e);
            return this._timeStamp;
        }
    }
}
