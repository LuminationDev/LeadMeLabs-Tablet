package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.interfaces.IHelpEventDetails;
import com.segment.analytics.Properties;

/**
 * Represents an event related to the a help issue, to be tracked using Segment analytics.
 * This event includes the .....
 * Implements the {@link IHelpEventDetails} interface to provide access to specific lab event details.
 */
public class SegmentHelpEvent extends SegmentEvent implements IHelpEventDetails {
    private final String topic;

    /**
     * Constructs a new SegmentHelpEvent with the specified parameters.
     *
     * @param event     The name of the event.
     * @param topic     The details associated with the help event.
     */
    public SegmentHelpEvent(String event, String topic) {
        super(Segment.getSessionId(), event, SegmentConstants.Event_Type_Help);
        this.topic = topic;
    }

    @Override
    public String getTopic() {
        return topic;
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
        map.put("topic", topic);
        map.put("classification", getClassification());
        return map;
    }
}
