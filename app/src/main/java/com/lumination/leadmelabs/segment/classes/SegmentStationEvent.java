package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.interfaces.IStationEventDetails;
import com.segment.analytics.Properties;

/**
 * Represents an event related to a station, to be tracked using Segment analytics.
 * This event includes the station id.
 * Implements the {@link IStationEventDetails} interface to provide access to specific station event details.
 */
public class SegmentStationEvent extends SegmentEvent implements IStationEventDetails {
    private final int stationId; // The id of the station related to the event

    /**
     * Constructs a new SegmentStationEvent with the specified parameters.
     *
     * @param event     The name of the event.
     * @param stationId The id of the station related to the event.
     */
    public SegmentStationEvent(String event, int stationId) {
        super(Segment.getSessionId(), event, SegmentConstants.Event_Type_Station);
        this.stationId = stationId;
    }

    @Override
    public int getStationId() {
        return stationId;
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
        map.put("stationId", getStationId());
        map.put("classification", getClassification());
        return map;
    }
}
