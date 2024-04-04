package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.interfaces.ITabletEventDetails;
import com.segment.analytics.Properties;

/**
 * Represents an event related to a tablet, to be tracked using Segment analytics.
 * This event includes the .....
 * Implements the {@link ITabletEventDetails} interface to provide access to specific tablet event details.
 */
public class SegmentTabletEvent extends SegmentEvent implements ITabletEventDetails {
    private final boolean wallMode; // If the tablet is in wall mode

    /**
     * Constructs a new SegmentLabEvent with the specified parameters.
     *
     * @param event     The name of the event.
     */
    public SegmentTabletEvent(String event, boolean wallMode) {
        super(Segment.getSessionId(), event, SegmentConstants.Event_Type_Tablet);

        this.wallMode = wallMode;
    }

    @Override
    public boolean getWallMode() {
        return wallMode;
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
        map.put("wallMode", getWallMode());
        map.put("classification", getClassification());
        return map;
    }
}
