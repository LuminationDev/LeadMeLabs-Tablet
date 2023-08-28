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
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentHelpBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;

public class HelpPageFragment extends Fragment {

    private static FragmentHelpBinding binding;
    public static FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);

        FlexboxLayout roomAutomations = view.findViewById(R.id.room_automations);
        roomAutomations.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Room Automations", "The server that connects to the room automations may have lost connection. Contact your local ICT department and ask them to restart it.");
        });

        FlexboxLayout serverConnection = view.findViewById(R.id.server_connection);
        serverConnection.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Server Connection", "The server that connects the lab may have turned off or lost network connection. Contact your local ICT department and ask them to restart it.");
        });

        FlexboxLayout vrLibrary = view.findViewById(R.id.vr_library);
        vrLibrary.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("VR Library", "Go to the VR library and press refresh. After approximately 1 minute, the experiences should be available in the list. If this doesn’t work, try restarting the station by shutting it down and then turning it back on. This can be done on the individual station screen.");
        });

        FlexboxLayout headsetConnection = view.findViewById(R.id.headset_connection);
        headsetConnection.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Headset Connection Issues", "Check that the headset for that station is inside the LED ring and has a charged battery plugged in and turned on. If this doesn’t fix the issue, press ‘Restart VR System’ and wait while it restarts. Then try to launch the experience again. If this doesn’t work, try restarting the station by shutting it down and then turning it back on. This can be done on the individual station screen. If this still doesn’t work, contact your local ICT department.");
        });

        FlexboxLayout steamVRErrors = view.findViewById(R.id.steam_vr_errors);
        steamVRErrors.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("SteamVR Errors", "Press ‘Restart VR System’ and wait while it restarts. This can be done on the individual station screen. Then try to launch the experience again. If this doesn’t work, try restarting the station by shutting it down and then turning it back on. This can be done on the individual station screen. If this still doesn’t work, contact your IT department.");
        });

        FlexboxLayout launchingExperience = view.findViewById(R.id.launching_an_experience);
        launchingExperience.setOnClickListener(v -> {
            DialogManager.createTroubleshootingTextDialog("Launching an experience", "Ensure that your headsets have batteries plugged in and are positioned in the center of their LED rings. Navigate to the VR library and select an experience. Select which stations you would like to launch the experience on. Press play and wait while the experience is launched.");
        });

        FlexboxLayout quickstartGuide = view.findViewById(R.id.quickstart_guide);
        quickstartGuide.setOnClickListener(v -> {
            DialogManager.buildWebViewDialog(getContext(), "https://drive.google.com/file/d/1OSnrUnQwggod2IwialnfbJ32nT-1q9mQ/view?usp=sharing");
        });

        FlexboxLayout lessonPlans = view.findViewById(R.id.lesson_plans);
        lessonPlans.setOnClickListener(v -> {
            DialogManager.createTextDialog("Lesson Plans", "Visit https://lms.lumination.com.au in a web browser to access lesson plans designed for use in a Lumination Learning Lab");
        });

        FlexboxLayout submitTicketButton = view.findViewById(R.id.submit_ticket_button);
        submitTicketButton.setOnClickListener(v -> {
            DialogManager.createSubmitTicketDialog();
        });

        FlexboxLayout updateDetails = view.findViewById(R.id.update_details);
        updateDetails.setOnClickListener(v -> {
            DialogManager.createUpdateDetailsDialog();
        });

        FlexboxLayout contactSupportButton = view.findViewById(R.id.contact_support);
        contactSupportButton.setOnClickListener(v -> {
            DialogManager.createSubmitTicketDialog();
        });
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            childManager.beginTransaction()
                    .replace(R.id.logo, LogoFragment.class, null)
                    .commitNow();
        }
    }
}