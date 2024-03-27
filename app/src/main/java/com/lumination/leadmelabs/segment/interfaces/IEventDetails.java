package com.lumination.leadmelabs.segment.interfaces;

import com.segment.analytics.Properties;

public interface IEventDetails {
    String getSessionId();
    String getEvent();
    String getClassification();
    Properties toProperties();
}