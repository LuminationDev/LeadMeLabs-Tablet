package com.lumination.leadmelabs.ui.help;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.classes.SegmentHelpEvent;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Properties;

import java.util.HashMap;

public class HelpPageFragment extends Fragment {

    public static FragmentManager childManager;
    public static final String segmentClassification = "Troubleshooting";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        childManager = getChildFragmentManager();
        DataBindingUtil.bind(view);

        FlexboxLayout roomAutomations = view.findViewById(R.id.room_automations);
        roomAutomations.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Room Automations", "The server that connects to the room automations may have lost connection. Contact your local ICT department and ask them to restart it.");

            this.trackHelpQuestionOpened("Room Automations");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "room_automations");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout serverConnection = view.findViewById(R.id.server_connection);
        serverConnection.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Server Connection", "The server that connects the lab may have turned off or lost network connection. Contact your local ICT department and ask them to restart it.");

            this.trackHelpQuestionOpened("Server Connection");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "server_connection");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout vrLibrary = view.findViewById(R.id.vr_library);
        vrLibrary.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("VR Library", "Go to the VR library and press refresh. After approximately 1 minute, the experiences should be available in the list. If this doesn’t work, try restarting the station by shutting it down and then turning it back on. This can be done on the individual station screen.");

            this.trackHelpQuestionOpened("VR Library");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "vr_library");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout headsetConnection = view.findViewById(R.id.headset_connection);
        headsetConnection.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Headset Connection Issues", "Check that the headset for that station is inside the LED ring and has a charged battery plugged in and turned on. If this doesn’t fix the issue, press ‘Restart VR System’ and wait while it restarts. Then try to launch the experience again. If this doesn’t work, try restarting the station by shutting it down and then turning it back on. This can be done on the individual station screen. If this still doesn’t work, contact your local ICT department.");

            this.trackHelpQuestionOpened("Headset Connection Issues");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "headset_connection");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout steamVRErrors = view.findViewById(R.id.steam_vr_errors);
        steamVRErrors.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("SteamVR Errors", "Press ‘Restart VR System’ and wait while it restarts. This can be done on the individual station screen. Then try to launch the experience again. If this doesn’t work, try restarting the station by shutting it down and then turning it back on. This can be done on the individual station screen. If this still doesn’t work, contact your IT department.");

            this.trackHelpQuestionOpened("SteamVR Errors");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "steam_vr_errors");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout launchingExperience = view.findViewById(R.id.controller_issues);
        launchingExperience.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Controller connectivity", "For detailed instructions visit: https://help.lumination.com.au/knowledge/vive-controller-troubleshooting-connectivity-issues\n1. Make sure that your device has enough battery charge.\n2. Ensure the controllers are set to never timeout in SteamVR.\n3. Re-pair the controllers in SteamVR.\n4. Ensure that the base stations have adequate line-of-sight to the controllers.\n5. Enable bluetooth connectivity and update the base station firmware.\n");

            this.trackHelpQuestionOpened("Controller Issues");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "controller_issues");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout headsetIsBlank = view.findViewById(R.id.headset_is_blank);
        headsetIsBlank.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Headset is blank", "Check that the headset battery is charged and try unplugging and plugging it and then waiting 30 seconds. If this does not resolve the issue, try pressing 'restart session'. If this does not resolve the issue, try pressing 'Restart VR system'. If this does not resolve the issue, try restarting the computer.");

            this.trackHelpQuestionOpened("Headset Is Blank");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "headset_is_blank");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout quickstartGuide = view.findViewById(R.id.quickstart_guide);
        quickstartGuide.setOnClickListener(v -> {
            DialogManager.buildWebViewDialog(getContext(), "https://drive.google.com/file/d/14h2zlMhjIK_cZnGobyfysYmOPzjnmkjK/view?usp=drive_link");

            this.trackHelpGuideOpened("Quickstart Guide");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "quickstart_guide");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout lessonPlans = view.findViewById(R.id.lesson_plans);
        lessonPlans.setOnClickListener(v -> {
            DialogManager.createTextDialog("Lesson Plans", "Visit https://lms.lumination.com.au in a web browser to access lesson plans designed for use in a Lumination Learning Lab");

            this.trackHelpGuideOpened("Lesson Plans");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "lesson_plans");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout updateDetails = view.findViewById(R.id.update_details);
        updateDetails.setOnClickListener(v -> {
            DialogManager.createUpdateDetailsDialog();

            this.trackHelpGuideOpened("Update Details");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "update_details");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout contactSupportButton = view.findViewById(R.id.knowledge_bank);
        contactSupportButton.setOnClickListener(v -> {
            DialogManager.createTextDialog("Knowledge Bank", "Visit https://help.lumination.com.au/knowledge/lumination-learning-labs in a web browser to access Lumination's Knowledge Bank.");

            this.trackHelpGuideOpened("Knowledge Bank");

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "knowledge_bank");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout submitTicketButton = view.findViewById(R.id.submit_ticket_button);
        submitTicketButton.setOnClickListener(v -> {
            DialogManager.createSubmitTicketDialog();

            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", segmentClassification);
            segmentProperties.put("name", "Submit Ticket Hero");
            Segment.trackEvent(SegmentConstants.Submit_Ticket_Opened, segmentProperties);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "troubleshooting");
                put("content_id", "submit_ticket_button");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        FlexboxLayout submitTicketOnlineLayout = view.findViewById(R.id.submit_ticket_online);
        FlexboxLayout submitTicketOfflineLayout = view.findViewById(R.id.submit_ticket_offline);
        Thread thread = new Thread(() -> {
            boolean isOnline = Helpers.urlIsAvailable("https://us-central1-leadme-labs.cloudfunctions.net/status");
            MainActivity.runOnUI(() -> {
                if (isOnline) {
                    submitTicketOnlineLayout.setVisibility(View.VISIBLE);
                    submitTicketOfflineLayout.setVisibility(View.GONE);
                } else {
                    submitTicketOnlineLayout.setVisibility(View.GONE);
                    submitTicketOfflineLayout.setVisibility(View.VISIBLE);
                }
            });
        });
        thread.start();

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void trackHelpQuestionOpened(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Help_Question_Opened, segmentProperties);
    }

    private void trackHelpGuideOpened(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Help_Guide_Opened, segmentProperties);
    }
}
