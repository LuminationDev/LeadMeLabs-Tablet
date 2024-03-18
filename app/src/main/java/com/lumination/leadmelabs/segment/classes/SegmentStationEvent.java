package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.interfaces.IStationEventDetails;

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
        super(event);
        this.stationId = stationId;
    }

    @Override
    public int getStationId() {
        return stationId;
    }
}
