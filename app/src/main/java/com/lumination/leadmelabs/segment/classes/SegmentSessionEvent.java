package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.interfaces.ILabEventDetails;
import com.lumination.leadmelabs.segment.interfaces.ISessionEventDetails;

/**
 * Represents an event related to the a Session, to be tracked using Segment analytics.
 * This event includes the .....
 * Implements the {@link ISessionEventDetails} interface to provide access to specific session event details.
 */
public class SegmentSessionEvent extends SegmentEvent implements ISessionEventDetails {
    private final String start; // The date stamp of the start of session
    private final String end; // The date stamp of the end of the session
    private final String duration; // The duration of the session

    /**
     * Constructs a new SegmentLabEvent with the specified parameters.
     *
     * @param event     The name of the event.
     */
    public SegmentSessionEvent(String event, String start, String end, String duration) {
        super(Segment.getSessionId(), event);
        this.start = start;
        this.end = end;
        this.duration = duration;
    }

    @Override
    public String getStart() {
        return start;
    }

    @Override
    public String getEnd() {
        return end;
    }

    @Override
    public String getDuration() {
        return duration;
    }
}
