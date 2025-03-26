package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.interfaces.IEventDetails;
import com.segment.analytics.Properties;

public class SegmentEvent implements IEventDetails {
    private final String sessionId;
    private final String key;
    private final String classification;

    SegmentEvent(String sessionId, String key, String classification) {
        this.sessionId = sessionId;
        this.key = key;
        this.classification = classification;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getEvent() {
        return key;
    }

    @Override
    public String getClassification() { return this.classification; }

    @Override
    public Properties toProperties() { return null; }
}
