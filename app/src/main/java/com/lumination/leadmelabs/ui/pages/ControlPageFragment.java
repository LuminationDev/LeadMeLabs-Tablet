package com.lumination.leadmelabs.ui.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuFragment;
import com.segment.analytics.Properties;

import java.util.HashSet;
import java.util.Iterator;

public class ControlPageFragment extends Fragment {
    public static FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page_controls, container, false);
        childManager = getChildFragmentManager();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(HelpPageFragment.class, "help", null);
            ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).removeSubMenu();
        });

        if (savedInstanceState == null) {
            loadScenes();
        }

        view.findViewById(R.id.refresh_appliances_btn).setOnClickListener(v -> {
            if(NetworkService.getNUCAddress() != null) {
                NetworkService.sendMessage("NUC", "Appliances", "RefreshList");
                Properties segmentProperties = new Properties();
                segmentProperties.put("classification", "Appliance");
                segmentProperties.put("name", SubMenuFragment.mViewModel.getSelectedPage().getValue());
                Segment.trackEvent(SegmentConstants.Appliances_Refreshed, segmentProperties);
                Toast.makeText(getContext(), "Appliances refreshing", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Load in the initial fragments for the main view and replace the side menu. Check what page
     * type actually has appliances and load that first so there is no blank page to start with.
     */
    private void loadScenes() {
        HashSet<String> types = SubMenuFragment.checkedApplianceTypes;

        Bundle args = new Bundle();

        //Defaults
        String title = "Scenes";
        String type = "scenes";

        if(types != null) {
            //Check for lights and scenes first as this is the natural order
            if(types.contains("scenes")) {
                title = "Scenes";
                type = "scenes";
            } else if(types.contains("lights")) {
                title = SubMenuFragment.getTitle("lights");
                type = "lights";
            } else {
                Iterator<String> iterator = types.iterator();
                if (iterator.hasNext()) {
                    String firstType = iterator.next();
                    title = SubMenuFragment.getTitle(firstType);
                    type = firstType;
                }
            }
        }

        args.putString("title", title);
        args.putString("type", type);

        childManager.beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.subpage, ApplianceFragment.class, args)
                .replace(R.id.logo, LogoFragment.class, null)
                .replace(R.id.rooms, RoomFragment.class, null)
                .addToBackStack("submenu:" + type)
                .commit();
        Segment.trackScreen("submenu:" + type);

        childManager.executePendingTransactions();
    }
}
