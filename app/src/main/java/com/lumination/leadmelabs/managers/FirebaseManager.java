package com.lumination.leadmelabs.managers;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.models.Notification;
import com.lumination.leadmelabs.notifications.NotificationManager;
import com.lumination.leadmelabs.services.jobServices.NotificationJobService;
import com.lumination.leadmelabs.ui.pages.NotificationPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import io.sentry.Sentry;

public class FirebaseManager {
    private static final String TAG = "Firebase Manager";
    private static FirebaseAnalytics mFirebaseAnalytics;

    /**
     * Obtain the FirebaseAnalytics instance.
     * @param instance A FirebaseAnalytics instance.
     */
    public static void setupFirebaseManager(FirebaseAnalytics instance) {
        mFirebaseAnalytics = instance;
    }

    public static void reportTrafficFlags() {
        if (Boolean.TRUE.equals(SettingsFragment.mViewModel.getInternalTrafficValue().getValue())) {
            logAnalyticEvent("internal_traffic", new HashMap<String, String>() {});
        }
        if (Boolean.TRUE.equals(SettingsFragment.mViewModel.getDeveloperTrafficValue().getValue())) {
            logAnalyticEvent("developer_traffic", new HashMap<String, String>() {});
        }
    }

    /**
     * Check if the local license key is present on the Firestore.
     */
    public static void validateLicenseKey() {
        String key = SettingsFragment.mViewModel.getLicenseKey().getValue();

        //No key present
        if(key == null || key.isEmpty()) {
            Log.e(TAG, "No key present");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("keys").document(key);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.e(TAG, "DocumentSnapshot data: " + document.getData()); //Valid key
                } else {
                    Log.e(TAG, "No such document"); //No valid key - do something
                }
            } else {
                Log.e(TAG, "get failed with ", task.getException());
            }
        });
    }

    /**
     * Create a manual log, only enter the log if analytics is enabled.
     * @param event A FirebaseAnalytics.Event describing what a user has just interacted with.
     */
    public static void logAnalyticEvent(String event, Map<String, String> attributes) {
        try {
            Thread thread = new Thread(() -> {
                if(Boolean.TRUE.equals(SettingsFragment.mViewModel.getAnalyticsEnabled().getValue())) {
                    Bundle bundle = new Bundle();

                    // add custom attributes
                    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                        bundle.putString(attribute.getKey(), attribute.getValue());
                    }

                    mFirebaseAnalytics.logEvent(event, bundle);
                }
            });
            thread.start();
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    public static void setDefaultAnalyticsParameter(String key, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value);
        mFirebaseAnalytics.setDefaultEventParameters(bundle);
    }

    /**
     * Allow a user to disable all analytics. This stops any manual log events and also all background
     * events as well.
     * @param enabled A boolean representing if analytics is to be collected.
     */
    public static void toggleAnalytics(boolean enabled) {
        mFirebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
    }

    private static final HashSet<String> acknowledgements = new HashSet<>();

    /**
     * Check for current acknowledgements on Firebase before calling for any notification data.
     */
    public static void checkForNotifications() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        if (SettingsFragment.mViewModel == null) return;

        LiveData<String> labLocationData = SettingsFragment.mViewModel.getLabLocation();
        if (labLocationData == null) return;

        String labLocation = SettingsFragment.mViewModel.getLabLocation().getValue();
        if (labLocation == null) return;

        //Reset the notification page in case a user is currently viewing it.
        if (NotificationPageFragment.mViewModel != null) {
            NotificationPageFragment.mViewModel.setCurrentPage(1);
        }

        // Fetch the acknowledgements first
        DatabaseReference acknowledgmentsRef = db.getReference(String.format("lab_notifications/acknowledgements/%s", labLocation));
        acknowledgmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot acknowledgmentSnapshot : dataSnapshot.getChildren()) {
                    acknowledgements.add(acknowledgmentSnapshot.getKey());
                }

                //Only after acknowledgements have been collected do we check for data.
                checkForNotificationData(db);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
                Log.w("DatabaseError", "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    /**
     * Check if there are any new notifications on Firebase.
     */
    private static void checkForNotificationData(FirebaseDatabase db) {
        DatabaseReference notificationsRef = db.getReference("lab_notifications/notifications");
        // Fetch the data once
        notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Clear any saved past notifications
                NotificationPageFragment.mViewModel.clearNotifications();

                for (DataSnapshot notificationSnapshot : dataSnapshot.getChildren()) {
                    // Get the key of the current child - check if the message has already been acknowledged
                    String key = notificationSnapshot.getKey();

                    // Get the Notification object
                    Notification notification = notificationSnapshot.getValue(Notification.class);
                    if (notification != null) {
                        notification.setKey(key);

                        //Add the notification to the 'past' notifications list
                        boolean acknowledgementRequired = needToAcknowledge(key, notification.get_timeStamp());
                        notification.setStatus(acknowledgementRequired ? Constants.STATUS_SKIPPED : Constants.STATUS_ACCEPTED);
                        NotificationPageFragment.mViewModel.addNotification(notification);

                        //If the notification has already been acknowledged do not show the popup by default
                        if (!acknowledgementRequired) {
                            continue;
                        }
                        String urgency = notification.getUrgency();

                        switch (urgency) {
                            case Constants.ALERT:
                                createAlertNotification(notification);
                                break;

                            case Constants.EMERGENCY:
                                createEmergencyNotification(key, notification, false);
                                break;

                            default:
                                Log.e("Notification", "Unknown urgency: " + urgency);
                        }
                    }
                }

                NotificationPageFragment.refreshData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors
                Log.w("DatabaseError", "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    /**
     * Check if the tablet has already acknowledged the notification or if it the acknowledgement is
     * too old to be of any use - i.e. New tablets being configured for a lab.
     * @param key A string of the notification key on firebase.
     * @param timestamp A string of the timestamp the notification was created.
     * @return A boolean of if the tablet needs to acknowledge
     *              true - show the notification
     *              false- do not show it.
     */
    private static boolean needToAcknowledge(String key, String timestamp) {
        //Check if already acknowledged
        if (acknowledgements.contains(key)) {
            return false;
        }

        //Check if the notification is stale (too old)
        return !Helpers.isOlderThanXWeeks(timestamp, 2);
    }

    /**
     * Create an alert notification.
     * @param notification A notification object created from the retrieved firebase data.
     */
    private static void createAlertNotification(Notification notification) {
        NotificationManager.createAlertNotificationDialog(notification);
    }

    /**
     * Create an emergency notification.
     * @param notification A notification object created from the retrieved firebase data.
     */
    public static void createEmergencyNotification(String key, Notification notification, boolean canCancel) {
        BooleanCallbackInterface acknowledgmentCallback = confirmationResult -> {
            if (confirmationResult) {
                acknowledgeNotification(key);
            } else if (!canCancel) { //Only snooze if the notification is auto-prompted, not manually viewed from the notifications page
                //Set the timestamp to check against for snoozing.
                NotificationJobService.lastRunTimestamp = System.currentTimeMillis();
                NotificationManager.trackNotificationSnoozed(notification.getTitle());
            }
        };

        NotificationManager.createEmergencyNotificationDialog(notification, acknowledgmentCallback, canCancel);
    }

    /**
     * A notification has been acknowledged, record this in the firebase database.
     * @param key A string of the firebase UUID associated with the notification being acknowledged.
     */
    public static void acknowledgeNotification(String key) {
        String labLocation = SettingsFragment.mViewModel.getLabLocation().getValue();
        if (labLocation == null) return;

        NotificationPageFragment.refreshData();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference acknowledgmentsRef = database.getReference("lab_notifications/acknowledgements/" + labLocation);

        Map<String, Object> dataMap = createAcknowledgement(key);

        // Write data to the database
        acknowledgmentsRef.updateChildren(dataMap)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Data successfully written!"))
                .addOnFailureListener(e -> Log.e("Firebase", "Error writing data", e));
    }

    /**
     * Creates an acknowledgement map containing a timestamp and an acknowledged status.
     *
     * <p>This method generates the current timestamp in the format "EEE, dd MMM yyyy HH:mm:ss z"
     * (e.g., "Mon, 27 May 2024 14:55:02 GMT") and creates a nested map structure with the given key.
     * The inner map contains two entries: "_timeStamp" with the formatted timestamp and
     * "acknowledged" with a boolean value of true.
     *
     * @param key The key to associate with the acknowledgement data in the outer map.
     * @return A map containing the specified key associated with another map that holds
     * the timestamp and acknowledged status.
     */
    @NonNull
    private static Map<String, Object> createAcknowledgement(String key) {
        // Get the current time stamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        Date now = new Date();

        // Create a map to hold the key-value pair
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("_timeStamp", dateFormat.format(now));
        dictionary.put("acknowledged", true);

        // Create the object to set as the firebase acknowledgement
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(key, dictionary);

        return dataMap;
    }
}
