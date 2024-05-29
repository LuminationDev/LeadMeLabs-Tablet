package com.lumination.leadmelabs.notifications;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.models.Notification;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.segment.analytics.Properties;

public class NotificationManager {
    private static final String segmentNotificationClassification = "Notification";

    /**
     * Creates and displays an alert notification dialog based on the given Notification object.
     * This dialog includes basic notification information such as title, messages, and optional image or video.
     * Users can proceed or dismiss the notification.
     * Upon dismissal, the function tracks the notification as dismissed.
     * @param notification The Notification object containing the information to display in the dialog.
     */
    public static void createAlertNotificationDialog(Notification notification) {
        View notificationDialogView = setupBasicNotification(notification, R.layout.dialog_notification_alert);
        if (notificationDialogView == null) return;

        AlertDialog confirmationDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertNotificationTheme).setView(notificationDialogView).create();
        confirmationDialog.setCancelable(false);
        confirmationDialog.setCanceledOnTouchOutside(false);

        FlexboxLayout nextButton = notificationDialogView.findViewById(R.id.next_dialog);
        nextButton.setOnClickListener(w -> {
            Log.e("Notification", "Go next somehow...");
        });

        FlexboxLayout skipButton = notificationDialogView.findViewById(R.id.skip_dialog);
        skipButton.setOnClickListener(w -> {
            confirmationDialog.dismiss();
            trackNotificationDismissed(notification.getTitle());
        });

        confirmationDialog.show();

        if (confirmationDialog.getWindow() != null) {
            confirmationDialog.getWindow().setLayout(700, 780);
        }
        trackNotificationShown(notification.getTitle());
    }

    /**
     * Creates and displays an emergency notification dialog based on the given Notification object.
     * This dialog includes basic notification information such as title, messages, and optional image or video.
     * Users can acknowledge or snooze the notification.
     * Upon acknowledgment or dismissal, the function invokes the provided callback interface with the corresponding action.
     * It also tracks the notification as acknowledged or dismissed accordingly.
     * @param notification The Notification object containing the information to display in the dialog.
     * @param booleanCallbackInterface The callback interface to handle user actions (acknowledge or snooze).
     */
    public static void createEmergencyNotificationDialog(Notification notification, BooleanCallbackInterface booleanCallbackInterface) {
        View notificationDialogView = setupBasicNotification(notification, R.layout.dialog_notification_emergency);
        if (notificationDialogView == null) return;

        AlertDialog confirmationDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertNotificationTheme).setView(notificationDialogView).create();
        confirmationDialog.setCancelable(false);
        confirmationDialog.setCanceledOnTouchOutside(false);

        FlexboxLayout acknowledgeButton = notificationDialogView.findViewById(R.id.acknowledge_dialog);
        acknowledgeButton.setOnClickListener(w -> {
            booleanCallbackInterface.callback(true);
            confirmationDialog.dismiss();
            trackNotificationAcknowledged(notification.getTitle());
        });

        FlexboxLayout snoozeButton = notificationDialogView.findViewById(R.id.snooze_dialog);
        snoozeButton.setOnClickListener(w -> {
            booleanCallbackInterface.callback(false);
            confirmationDialog.dismiss();
            trackNotificationDismissed(notification.getTitle());
        });

        confirmationDialog.show();

        if (confirmationDialog.getWindow() != null) {
            if (notification.getVideoLink() == null && notification.getPhotoLinks().isEmpty()) {
                confirmationDialog.getWindow().setLayout(700, 580);
            } else {
                confirmationDialog.getWindow().setLayout(700, 780);
            }
        }
        trackNotificationShown(notification.getTitle());
    }

    /**
     * Sets up a basic notification dialog view based on the provided Notification object and layout resource.
     *
     * @param notification The Notification object containing data to be displayed.
     * @param dialog       The layout resource ID for the notification dialog view.
     * @return The inflated View representing the notification dialog.
     */
    private static View setupBasicNotification(Notification notification, int dialog) {
        View notificationDialogView = View.inflate(MainActivity.getInstance(), dialog, null);

        LinearLayout dotsLayout = notificationDialogView.findViewById(R.id.dotsLayout);
        ImageView[] dots;

        //There is always messages
        ViewPager2 viewPager = notificationDialogView.findViewById(R.id.viewPager);
        if (notification.getVideoLink() == null) {
            NotificationImageAdapter adapter = new NotificationImageAdapter(notification.getTitle(), notification.getMessages(), notification.getPhotoLinks());
            viewPager.setAdapter(adapter);
        } else {
            VideoView videoView = notificationDialogView.findViewById(R.id.supplied_video);
            videoView.setVideoPath(notification.getVideoLink());
            videoView.setVisibility(View.VISIBLE);

            NotificationTextAdapter adapter = new NotificationTextAdapter(notification.getTitle(), notification.getMessages());
            viewPager.setAdapter(adapter);
        }

        if (viewPager.getAdapter() == null) return null;

        if (viewPager.getAdapter().getItemCount() > 1) {
            dots = setupDots(viewPager.getAdapter().getItemCount(), dotsLayout);
            // Set OnClickListener for each dot to navigate to the corresponding page
            for (int i = 0; i < dots.length; i++) {
                final int position = i;
                dots[i].setOnClickListener(v -> {
                    viewPager.setCurrentItem(position, true);
                });
            }

            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateDots(position, dots);
                }
            });
        }

        return notificationDialogView;
    }

    /**
     * Sets up dots below a ViewPager2 to indicate the number of pages.
     *
     * @param count      The number of pages.
     * @param dotsLayout The LinearLayout where the dots will be added.
     * @return An array of ImageView representing the dots.
     */
    private static ImageView[] setupDots(int count, LinearLayout dotsLayout) {
        ImageView[] dots = new ImageView[count];
        dotsLayout.removeAllViews();

        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(MainActivity.getInstance());
            //Page 0 is always loaded first
            dots[i].setImageResource(i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            dotsLayout.addView(dots[i], params);
        }

        return dots;
    }

    /**
     * Updates the dots below a ViewPager2 to highlight the current page.
     *
     * @param currentPage The index of the current page.
     * @param dots        An array of ImageView representing the dots.
     */
    private static void updateDots(int currentPage, ImageView[] dots) {
        for (int i = 0; i < dots.length; i++) {
            if (i == currentPage) {
                dots[i].setImageResource(R.drawable.dot_active); // Replace with your active dot drawable
            } else {
                dots[i].setImageResource(R.drawable.dot_inactive); // Replace with your inactive dot drawable
            }
        }
    }

    //region Tracking
    private static void trackNotificationShown(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentNotificationClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Show_Notification, segmentProperties);
    }

    private static void trackNotificationAcknowledged(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentNotificationClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Acknowledge_Notification, segmentProperties);
    }

    public static void trackNotificationSnoozed(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentNotificationClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Snoozed_Notification, segmentProperties);
    }

    private static void trackNotificationDismissed(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentNotificationClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Dismiss_Notification, segmentProperties);
    }
    //endregion
}
