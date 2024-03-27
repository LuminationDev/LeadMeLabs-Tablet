package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.interfaces.ISessionEventDetails;
import com.segment.analytics.Properties;

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
        super(Segment.getSessionId(), event, SegmentConstants.Event_Type_Session);
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

    /**
     * Converts the SegmentExperienceEvent object into a Map<String, ?>.
     *
     * @return A map representing the SegmentExperienceEvent object.
     */
    @Override
    public Properties toProperties() {
        Properties map = new Properties();
        map.put("sessionId", Segment.getSessionId());
        map.put("sessionStart", getStart());
        map.put("sessionEnd", getEnd());
        map.put("duration", getDuration());
        map.put("classification", getClassification());
        return map;
    }
}
