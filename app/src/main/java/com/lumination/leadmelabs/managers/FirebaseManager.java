package com.lumination.leadmelabs.managers;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

import java.util.HashMap;
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

    /**
     * Determine the type of traffic being sent.
     */
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
        if(key == null) {
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
}
