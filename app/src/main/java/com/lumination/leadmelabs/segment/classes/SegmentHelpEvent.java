package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.interfaces.IHelpEventDetails;

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
        super(event);
        this.topic = topic;
    }

    @Override
    public String getTopic() {
        return topic;
    }
}
