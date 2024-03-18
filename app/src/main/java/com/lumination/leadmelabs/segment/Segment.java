package com.lumination.leadmelabs.segment;

import com.lumination.leadmelabs.segment.classes.SegmentEvent;
import com.lumination.leadmelabs.segment.interfaces.IEventDetails;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;

import java.util.Hashtable;

/**
 * A class to handle Segment data collection setup and functionality.
 */
public class Segment {
    private final static String writeKey = "val2jLThqRiUEQGFGzDyHzT4ehtMXsKA"; //TODO hide the write key?
    private static Analytics analytics;
    private static boolean isIdSet = false;

    /**
     * The unique identifier to distinguish the users on the database. The location of the lab is
     * being used for this.
     */
    private static String userId;

    /**
     * The location has now been set from an external source or the settings page. Initialise Segment
     * with the new user data.
     */
    public static void updateUserId() {
        isIdSet = true;
        initialise();
    }

    public static boolean getIsIdSet() {
        return isIdSet;
    }

    /**
     * Setup the analytics class and the user id (location) for the session.
     */
    public static void initialise() {
        analytics = Analytics.builder(writeKey).build();
        userId = SettingsFragment.mViewModel.getLabLocation().getValue();

        // If the location is not set, when the tablet connects to the NUC it will ask for its
        // location. Upon receiving, the location will be saved in shared preferences and set as
        // the userId.
        if (Helpers.isNullOrEmpty(userId)) {
            return;
        }

        isIdSet = true;
        setUserId();
    }

    /**
     * Set the user for Segment to track against for the current session.
     */
    private static void setUserId() {
        Hashtable<String, String> map = new Hashtable<>();
        map.put("location", userId);
        map.put("app_version", Helpers.getAppVersion());
        analytics.enqueue(IdentifyMessage.builder()
                .userId(userId)
                .traits(map));
    }

    /**
     * Tracks an action/event with associated details using Segment analytics.
     * @param <T> The type of values stored in the details hashtable.
     * @param event A string of the event type to store the action against in Segment.
     * @param details A {@link SegmentEvent} containing additional details or properties associated with the event.
     */
    public static <T extends IEventDetails> void trackAction(String event, T details) {
        if (!isIdSet) return;

        Hashtable<String, T> action = new Hashtable<>();
        action.put(details.getEvent(), details);

        analytics.enqueue(TrackMessage.builder(event)
                .userId(userId)
                .properties(action)
        );
    }
}
