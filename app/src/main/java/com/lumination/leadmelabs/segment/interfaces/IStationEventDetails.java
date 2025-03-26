package com.lumination.leadmelabs.segment.interfaces;

import com.segment.analytics.Properties;

public interface IStationEventDetails extends IEventDetails {
    int getStationId();
    Properties toProperties();
}
