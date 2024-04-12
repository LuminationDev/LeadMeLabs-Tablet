package com.lumination.leadmelabs.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentSettingsBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.ui.pages.SettingsPageFragment;
import com.segment.analytics.Properties;

import java.util.HashSet;

public class SettingsFragment extends Fragment {

    public static SettingsViewModel mViewModel;
    private FragmentSettingsBinding binding;

    private static int ipAddressPresses = 0;
    public static SettingsFragment instance;
    public static SettingsFragment getInstance() { return instance; }

    public static final String segmentClassification = "Settings";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ipAddressPresses = 0;

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setSettings(mViewModel);

        FlexboxLayout nucDetails = view.findViewById(R.id.nuc_details);
        nucDetails.setOnClickListener(v ->
                DialogManager.buildNucDetailsDialog(getContext())
        );

        FlexboxLayout setNucAddressButton = view.findViewById(R.id.set_nuc_address);
        setNucAddressButton.setOnClickListener(v ->
                DialogManager.buildSetNucDialog(getContext())
        );

        FlexboxLayout setPinCodeButton = view.findViewById(R.id.set_pin_code);
        setPinCodeButton.setOnClickListener(v ->
                DialogManager.buildSetPINCodeDialog(getContext())
        );

        FlexboxLayout setEncryptionKeyButton = view.findViewById(R.id.set_encryption_key);
        setEncryptionKeyButton.setOnClickListener(v ->
                DialogManager.buildSetEncryptionKeyDialog(getContext())
        );

        FlexboxLayout setLabLocationButton = view.findViewById(R.id.set_lab_location);
        setLabLocationButton.setOnClickListener(v ->
                DialogManager.buildSetLabLocationDialog(getContext())
        );

        FlexboxLayout setLicenseKeyButton = view.findViewById(R.id.set_license_key);
        setLicenseKeyButton.setOnClickListener(v ->
                DialogManager.buildSetLicenseKeyDialog(getContext())
        );

        FlexboxLayout howToButton = view.findViewById(R.id.how_to_button);
        howToButton.setOnClickListener(v ->
                DialogManager.buildWebViewDialog(getContext(), "https://drive.google.com/file/d/14h2zlMhjIK_cZnGobyfysYmOPzjnmkjK/view?usp=drive_link")
        );

        //The toggle for turning wall mode on and off
        FlexboxLayout hideStationControlsLayout = view.findViewById(R.id.hide_station_controls);
        SwitchCompat hideStationControlsToggle = view.findViewById(R.id.hide_station_controls_toggle);
        hideStationControlsToggle.setChecked(Boolean.TRUE.equals(mViewModel.getHideStationControls().getValue()));
        hideStationControlsLayout.setOnClickListener(v ->
                hideStationControlsToggle.setChecked(!hideStationControlsToggle.isChecked())
        );

        CompoundButton.OnCheckedChangeListener hideStationControlsToggleListener = (compoundButton, isChecked) -> {
            mViewModel.setHideStationControls(isChecked);

            if(isChecked) {
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragmentManager.beginTransaction()
                        .replace(R.id.main, SettingsPageFragment.class, null)
                        .addToBackStack("menu:settings")
                        .commit();

                fragmentManager.executePendingTransactions();
                Segment.trackScreen("menu:settings");
            }
            this.trackSettingChanged("Hide Station Controls", String.valueOf(isChecked));
        };
        hideStationControlsToggle.setOnCheckedChangeListener(hideStationControlsToggleListener);

        //The toggle for turning show hidden stations
        FlexboxLayout showHiddenStationsLayout = view.findViewById(R.id.show_hidden_stations);
        SwitchCompat showHiddenStationsToggle = view.findViewById(R.id.show_hidden_stations_toggle);
        showHiddenStationsToggle.setChecked(Boolean.TRUE.equals(mViewModel.getShowHiddenStations().getValue()));
        showHiddenStationsLayout.setOnClickListener(v ->
                showHiddenStationsToggle.setChecked(!showHiddenStationsToggle.isChecked())
        );
        showHiddenStationsToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            mViewModel.setShowHiddenStations(isChecked);
            this.trackSettingChanged("Show Hidden Stations", String.valueOf(isChecked));
        });

        //The toggle for turning analytics on and off
        FlexboxLayout enableAnalyticsLayout = view.findViewById(R.id.enable_analytical_collection);
        SwitchCompat enableAnalyticsToggle = view.findViewById(R.id.enable_analytical_collection_toggle);
        enableAnalyticsToggle.setChecked(Boolean.TRUE.equals(mViewModel.getAnalyticsEnabled().getValue()));
        enableAnalyticsLayout.setOnClickListener(v ->
                enableAnalyticsToggle.setChecked(!enableAnalyticsToggle.isChecked())
        );
        enableAnalyticsToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            mViewModel.setAnalyticsEnabled(isChecked);
            if (isChecked) {
                Segment.initialise();
                this.trackSettingChanged("Analytics Enabled", String.valueOf(isChecked));
            }
        });

        //The toggle for turning exit prompts on and off
        FlexboxLayout enableExitPromptsLayout = view.findViewById(R.id.exit_prompt_controls);
        SwitchCompat enableExitPromptsToggle = view.findViewById(R.id.exit_prompt_controls_toggle);
        enableExitPromptsToggle.setChecked(Boolean.TRUE.equals(mViewModel.getAdditionalExitPrompts().getValue()));
        enableExitPromptsLayout.setOnClickListener(v ->
                enableExitPromptsToggle.setChecked(!enableExitPromptsToggle.isChecked())
        );
        enableExitPromptsToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            mViewModel.setAdditionalExitPrompts(isChecked);
            this.trackSettingChanged("Additional Exit Prompts", String.valueOf(isChecked));
        });

        //Send the user to the play store listing of LeadMe Labs whilst unpinning the application
        FlexboxLayout updateLeadMeButton = view.findViewById(R.id.update_leadme);
        updateLeadMeButton.setOnClickListener(v -> MainActivity.UIHandler.post(() -> {
            //Unpin the application
            MainActivity.getInstance().stopLockTask();

            //Send the user to the Play Store: getPackageName() from Context or Activity object
            final String appPackageName = MainActivity.getInstance().getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException e) {
                if (e.getLocalizedMessage() != null) {
                    Log.e("Activity Not found", e.getLocalizedMessage());
                }
            }
            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", segmentClassification);
            Segment.trackEvent(SegmentConstants.Visit_Play_Store, segmentProperties);
        }));

        instance = this;

        //The toggle for turning room lock on and off
        FlexboxLayout enableRoomLockLayout = view.findViewById(R.id.enable_room_lock);
        SwitchCompat enableRoomLockToggle = view.findViewById(R.id.enable_room_lock_toggle);
        enableRoomLockToggle.setChecked(Boolean.TRUE.equals(mViewModel.getAnalyticsEnabled().getValue()));
        enableRoomLockLayout.setOnClickListener(v ->
                enableRoomLockToggle.setChecked(!enableRoomLockToggle.isChecked())
        );
        enableRoomLockToggle.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            mViewModel.setRoomLockEnabled(isChecked);
            this.trackSettingChanged("Room Lock", String.valueOf(isChecked));
        });

        FlexboxLayout setLockedRoomButton = view.findViewById(R.id.set_locked_room);
        setLockedRoomButton.setOnClickListener(v ->
                DialogManager.buildLockedRoomDialog(getContext())
        );

        FlexboxLayout internalTrafficLayout = view.findViewById(R.id.internal_traffic);
        SwitchCompat internalTrafficToggle = view.findViewById(R.id.internal_traffic_toggle);
        internalTrafficToggle.setChecked(Boolean.TRUE.equals(mViewModel.getInternalTrafficValue().getValue()));
        internalTrafficLayout.setOnClickListener(v ->
                internalTrafficToggle.setChecked(!internalTrafficToggle.isChecked())
        );
        internalTrafficToggle.setOnCheckedChangeListener((compoundButton, isChecked) ->
                mViewModel.setInternalTrafficValue(isChecked)
        );

        FlexboxLayout developerTrafficLayout = view.findViewById(R.id.developer_traffic);
        SwitchCompat developerTrafficToggle = view.findViewById(R.id.developer_traffic_toggle);
        developerTrafficToggle.setChecked(Boolean.TRUE.equals(mViewModel.getDeveloperTrafficValue().getValue()));
        developerTrafficLayout.setOnClickListener(v ->
                developerTrafficToggle.setChecked(!developerTrafficToggle.isChecked())
        );
        developerTrafficToggle.setOnCheckedChangeListener((compoundButton, isChecked) ->
                mViewModel.setDeveloperTrafficValue(isChecked)
        );

        FlexboxLayout ipAddress = view.findViewById(R.id.ip_address);
        ipAddress.setOnClickListener(l -> {
            ipAddressPresses++;
            if (ipAddressPresses == 20) {
                internalTrafficLayout.setVisibility(View.VISIBLE);
                developerTrafficLayout.setVisibility(View.VISIBLE);
            }
            if (ipAddressPresses == 21) {
                internalTrafficLayout.setVisibility(View.GONE);
                developerTrafficLayout.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Check if additional exit prompts are enabled.
     * @return A boolean if the application should display additional prompts.
     */
    public static boolean checkAdditionalExitPrompts() {
        return Boolean.TRUE.equals(mViewModel.getAdditionalExitPrompts().getValue());
    }

    /**
     * Check whether a supplied object's room is held within a locked room or if there are no
     * locked rooms.
     */
    public static boolean checkLockedRooms(String room) {
        //Only add the appliance if it is in the locked room or there is no current locked rooms
        HashSet<String> locked = SettingsFragment.mViewModel.getLockedIfEnabled().getValue();
        if(locked == null) { //need to have a null check
            return true;
        } else return locked.size() == 0 || locked.contains(room);
    }

    private void trackSettingChanged(String name, String value) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("name", name);
        segmentProperties.put("newValue", value);
        Segment.trackEvent(SegmentConstants.Setting_Changed, segmentProperties);
    }

    private void trackSettingModalOpened(String name) {

    }
}