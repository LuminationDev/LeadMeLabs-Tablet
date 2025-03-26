package com.lumination.leadmelabs.segment.interfaces;

import com.segment.analytics.Properties;

public interface ILabEventDetails extends IEventDetails {
    Properties toProperties();
}
