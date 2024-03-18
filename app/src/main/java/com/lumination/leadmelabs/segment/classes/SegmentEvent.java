package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.interfaces.IEventDetails;

public class SegmentEvent implements IEventDetails {
    private final String key;

    SegmentEvent(String key) {
        this.key = key;
    }

    @Override
    public String getEvent() {
        return key;
    }
}
