package com.lumination.leadmelabs.ui.settings;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;

public class SettingsFragment extends Fragment {

    public static SettingsViewModel mViewModel;
    private View view;
    private AlertDialog nucDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settings, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        buildSetNucDialog();

        FlexboxLayout setNucAddressButton = view.findViewById(R.id.set_nuc_address);
        setNucAddressButton.setOnClickListener(v -> {
            nucDialog.show();
        });

        FlexboxLayout howToButton = view.findViewById(R.id.how_to_button);
        howToButton.setOnClickListener(v -> {
            // todo - open the guide
        });

        FlexboxLayout hideStationControlsLayout = view.findViewById(R.id.hide_station_controls);
        SwitchCompat hideStationControlsToggle = view.findViewById(R.id.hide_station_controls_toggle);
        SettingsViewModel settingsViewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel.class);
        hideStationControlsToggle.setChecked(settingsViewModel.getHideStationControls().getValue().booleanValue());
        hideStationControlsLayout.setOnClickListener(v -> {
            hideStationControlsToggle.setChecked(!hideStationControlsToggle.isChecked());
        });
        CompoundButton.OnCheckedChangeListener hideStationControlsToggleListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                settingsViewModel.setHideStationControls(new Boolean(isChecked));
            }
        };
        hideStationControlsToggle.setOnCheckedChangeListener(hideStationControlsToggleListener);
    }

    private void buildSetNucDialog() {
        View view = View.inflate(getContext(), R.layout.dialog_set_nuc, null);
        mViewModel.getNuc().observe(getViewLifecycleOwner(), nucAddress -> {
            TextView textView = view.findViewById(R.id.nuc_address);
            textView.setText(nucAddress);
        });
        Button scan_button = view.findViewById(R.id.scan_nuc_address);
        scan_button.setOnClickListener(v -> {
            NetworkService.broadcast("Android");
        });
        EditText newAddress = view.findViewById(R.id.nuc_address_input);
        Button setAddress = view.findViewById(R.id.set_nuc_button);
        setAddress.setOnClickListener(v -> {
            mViewModel.setNucAddress(newAddress.getText().toString());
            nucDialog.dismiss();
        });
        nucDialog = new AlertDialog.Builder(getContext()).setView(view).create();
    }
}