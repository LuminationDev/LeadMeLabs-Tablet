package com.lumination.leadmelabs.segment.interfaces;

import com.segment.analytics.Properties;
public interface ISessionEventDetails extends IEventDetails {
    String getStart();
    String getEnd();
    String getDuration();
    Properties toProperties();
}
