package com.lumination.leadmelabs.segment.interfaces;

import com.segment.analytics.Properties;

public interface ITabletEventDetails extends IEventDetails {
    boolean getWallMode();
    Properties toProperties();
}
