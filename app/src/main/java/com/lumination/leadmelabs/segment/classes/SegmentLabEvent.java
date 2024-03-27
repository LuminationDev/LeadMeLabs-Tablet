package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.interfaces.ILabEventDetails;
import com.segment.analytics.Properties;

/**
 * Represents an event related to the entire lab, to be tracked using Segment analytics.
 * This event includes the .....
 * Implements the {@link ILabEventDetails} interface to provide access to specific lab event details.
 */
public class SegmentLabEvent extends SegmentEvent implements ILabEventDetails {
    /**
     * Constructs a new SegmentLabEvent with the specified parameters.
     *
     * @param event     The name of the event.
     */
    public SegmentLabEvent(String event) {
        super(Segment.getSessionId(), event, SegmentConstants.Event_Type_Lab);
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
        map.put("classification", getClassification());
        return map;
    }
}
