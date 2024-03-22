package com.lumination.leadmelabs.segment;

import com.lumination.leadmelabs.segment.classes.SegmentEvent;
import com.lumination.leadmelabs.segment.classes.SegmentSessionEvent;
import com.lumination.leadmelabs.segment.interfaces.IEventDetails;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A class to handle Segment data collection setup and functionality.
 */
public class Segment {
    private final static String writeKey = "val2jLThqRiUEQGFGzDyHzT4ehtMXsKA";
    private static Analytics analytics;
    private static boolean isIdSet = false;
    private static String sessionId = "Session not started";
    private static Date sessionStart;

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

    public static String getSessionId() { return sessionId; }

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
     * Generates a new session ID using a random UUID.
     * This method generates a new session ID by creating a random UUID
     * and extracting the first 10 characters, removing any hyphens.
     * The resulting session ID is stored in the 'sessionId' variable.
     */
    public static void generateNewSessionId() {
        if (!sessionId.equals("Session not started")) {
            return;
        }

        // Generate random UUID
        UUID uuid = UUID.randomUUID();

        if (sessionStart == null) {
            sessionStart = new Date();
        }

        // Extract the first 10 characters and remove any hyphens
        sessionId = uuid.toString().substring(0, 10).replaceAll("-", "");

        // Send a message to the NUC to update the NUC and any other tablets SessionId
        JSONObject message = new JSONObject();
        try {
            message.put("Action", "Update");
            message.put("SessionId", sessionId);
            message.put("SessionStart", sessionStart.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        NetworkService.sendMessage("NUC", "Segment", message.toString());
    }

    public static void resetSession() {
        if (sessionStart != null) {
            Date sessionEnd = new Date();

            // Calculate the time difference in milliseconds
            long differenceInMillis = Math.abs(sessionStart.getTime() - sessionEnd.getTime());

            // Convert milliseconds to hours, minutes, and seconds
            long hours = TimeUnit.MILLISECONDS.toHours(differenceInMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(differenceInMillis - TimeUnit.HOURS.toMillis(hours));
            long seconds = TimeUnit.MILLISECONDS.toSeconds(differenceInMillis - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes));

            String start = sessionStart.toString();
            String end = sessionEnd.toString();
            String duration = String.format(Locale.ENGLISH, "%d hours, %d minutes, %d seconds", hours, minutes, seconds);

            // Send an end Session event
            SegmentSessionEvent event = new SegmentSessionEvent(SegmentConstants.Event_Session_End, start, end, duration);
            Segment.trackAction(SegmentConstants.Event_Type_Lab, event);
        }

        sessionStart = null;
        sessionId = "Session not started";

        // Send a message to the NUC to update the NUC and any other tablets SessionId
        JSONObject message = new JSONObject();
        try {
            message.put("Action", "Update");
            message.put("SessionId", sessionId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        NetworkService.sendMessage("NUC", "Segment", message.toString());
    }

    /**
     * The NUC has sent details over about a session that another tablet has started. These need to
     * be synced between the devices so that one can start a session and another one may end it.
     * @param newSessionId A string of the current global session id.
     * @param newSessionStart A string of the current global session start time.
     */
    public static void setSessionFromExternal(String newSessionId, String newSessionStart) {
        if (newSessionId.equals(sessionId)) return;

        sessionId = newSessionId;

        //TODO update this? -
        // SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Date dateTime = format.parse(dateTimeString);
        sessionStart = new Date(newSessionStart);
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
