package com.lumination.leadmelabs.segment;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.segment.classes.SegmentEvent;
import com.lumination.leadmelabs.segment.classes.SegmentSessionEvent;
import com.lumination.leadmelabs.segment.interfaces.IEventDetails;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Analytics;
import com.segment.analytics.Traits;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A class to handle Segment data collection setup and functionality.
 */
public class Segment {
    private final static String writeKey = "val2jLThqRiUEQGFGzDyHzT4ehtMXsKA";
    private static boolean isIdSet = false;
    private static String sessionId = "Session not started";
    private static Date sessionStart;

    private static Analytics analytics;

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
        if (analytics == null) {
            analytics = new Analytics.Builder(MainActivity.getInstance().getBaseContext(), writeKey).build();
            Analytics.setSingletonInstance(analytics); // Set the initialized instance as a globally accessible instance.
        }

        userId = SettingsFragment.mViewModel.getLabLocation().getValue();

        // If the location is not set, when the tablet connects to the NUC it will ask for its
        // location. Upon receiving, the location will be saved in shared preferences and set as
        // the userId. Or if the Id is already set, do not attempt to set again.
        if (Helpers.isNullOrEmpty(userId) || isIdSet) {
            return;
        }

        isIdSet = true;
        setUserId();
    }

    /**
     * Set the user for Segment to track against for the current session.
     */
    private static void setUserId() {
        Traits map =  new Traits();
        map.putUsername(userId);
        map.putName(userId);
        map.put("location", userId);
        map.put("app_version", Helpers.getAppVersion());

        Analytics.with(MainActivity.getInstance().getBaseContext()).identify(userId, map, null);
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
            Segment.trackAction(event);
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

        try {
            // Parse the dateString back into a Date object
            sessionStart = dateFormat.parse(newSessionStart);
        } catch (ParseException e) {
            e.printStackTrace(); // Handle parsing exception
        }
    }

    /**
     * Tracks an action/event with associated details using Segment analytics.
     * @param <T> The type of values stored in the details hashtable.
     * @param details A {@link SegmentEvent} containing additional details or properties associated with the event.
     */
    public static <T extends IEventDetails> void trackAction(T details) {
        if (!isIdSet) return;

        Analytics.with(MainActivity.getInstance().getBaseContext())
                .track(details.getEvent(), details.toProperties());
    }
}
