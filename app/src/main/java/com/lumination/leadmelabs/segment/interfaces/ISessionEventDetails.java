package com.lumination.leadmelabs.segment.interfaces;

public interface ISessionEventDetails extends IEventDetails {
    String getStart();
    String getEnd();
    String getDuration();
}
