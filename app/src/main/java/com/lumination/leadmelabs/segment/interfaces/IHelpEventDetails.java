package com.lumination.leadmelabs.segment.interfaces;

import com.segment.analytics.Properties;

public interface IHelpEventDetails extends IEventDetails {
    String getTopic();
    Properties toProperties();
}
