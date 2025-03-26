package com.lumination.leadmelabs.segment.interfaces;

import com.segment.analytics.Properties;

public interface IExperienceEventDetails extends IEventDetails {
    int getStationId();
    String getName();
    String getId();
    String getType();
    Properties toProperties();
}
