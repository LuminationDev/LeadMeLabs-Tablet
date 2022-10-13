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
import com.lumination.leadmelabs.ui.pages.SettingsPageFragment;
import com.lumination.leadmelabs.utilities.WakeOnLan;

public class SettingsFragment extends Fragment {

    public static SettingsViewModel mViewModel;
    private FragmentSettingsBinding binding;

    public static SettingsFragment instance;
    public static SettingsFragment getInstance() { return instance; }

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

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setSettings(mViewModel);

        FlexboxLayout nucDetails = view.findViewById(R.id.nuc_details);
        nucDetails.setOnClickListener(v -> {
            DialogManager.buildNucDetailsDialog(getContext());
        });

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
            DialogManager.buildWebViewDialog(getContext(), "https://drive.google.com/file/d/1OSnrUnQwggod2IwialnfbJ32nT-1q9mQ/view?usp=sharing")
        );

        //The toggle for turning wall mode on and off
        FlexboxLayout hideStationControlsLayout = view.findViewById(R.id.hide_station_controls);
        SwitchCompat hideStationControlsToggle = view.findViewById(R.id.hide_station_controls_toggle);
        hideStationControlsToggle.setChecked(mViewModel.getHideStationControls().getValue());
        hideStationControlsLayout.setOnClickListener(v ->
            hideStationControlsToggle.setChecked(!hideStationControlsToggle.isChecked())
        );

        CompoundButton.OnCheckedChangeListener hideStationControlsToggleListener = (compoundButton, isChecked) -> {
            mViewModel.setHideStationControls(isChecked);

            if(isChecked) {
                MainActivity.fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                MainActivity.fragmentManager.beginTransaction()
                        .replace(R.id.main, SettingsPageFragment.class, null)
                        .addToBackStack("menu:settings")
                        .commit();

                MainActivity.fragmentManager.executePendingTransactions();
            }
        };
        hideStationControlsToggle.setOnCheckedChangeListener(hideStationControlsToggleListener);

        //The toggle for turning analytics on and off
        FlexboxLayout enableAnalyticsLayout = view.findViewById(R.id.enable_analytical_collection);
        SwitchCompat enableAnalyticsToggle = view.findViewById(R.id.enable_analytical_collection_toggle);
        enableAnalyticsToggle.setChecked(mViewModel.getAnalyticsEnabled().getValue());
        enableAnalyticsLayout.setOnClickListener(v ->
                enableAnalyticsToggle.setChecked(!enableAnalyticsToggle.isChecked())
        );

        enableAnalyticsToggle.setOnCheckedChangeListener((compoundButton, isChecked) ->
                mViewModel.setAnalyticsEnabled(isChecked)
        );

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
                Log.e("Activity Not found", e.getLocalizedMessage());
            }
        }));

        instance = this;
    }
}