package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.interfaces.IEventDetails;

public class SegmentEvent implements IEventDetails {
    private final String sessionId;
    private final String key;

    SegmentEvent(String sessionId, String key) {
        this.sessionId = sessionId;
        this.key = key;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getEvent() {
        return key;
    }
}
