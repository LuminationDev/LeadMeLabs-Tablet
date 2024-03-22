package com.lumination.leadmelabs.segment.classes;

import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.interfaces.IExperienceEventDetails;

/**
 * Represents an event related to a Station experience, to be tracked using Segment analytics.
 * This event includes details such as the name, id, type, and station id.
 * Implements the {@link IExperienceEventDetails} interface to provide access to specific experience event details.
 */
public class SegmentExperienceEvent extends SegmentEvent implements IExperienceEventDetails {
    private final int stationId; // The id of the station related to the experience event
    private final String name; // The name associated with the experience event
    private final String id; // The id associated with the experience event
    private final String type; // The type associated with the experience event

    /**
     * Constructs a new SegmentExperienceEvent with the specified parameters.
     *
     * @param event     The name of the event.
     * @param stationId The id of the station related to the experience event.
     * @param name      The name associated with the experience event.
     * @param id        The id associated with the experience event.
     * @param type      The type associated with the experience event.
     */
    public SegmentExperienceEvent(String event, int stationId, String name, String id, String type) {
        super(Segment.getSessionId(), event);
        this.stationId = stationId;
        this.name = name;
        this.id = id;
        this.type = type;
    }

    @Override
    public int getStationId() {
        return stationId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }
}
