package com.lumination.leadmelabs.ui.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.services.NetworkService;

public class SettingsFragment extends Fragment {

    public static SettingsViewModel mViewModel;
    private AlertDialog nucDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
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

        FlexboxLayout setPinCodeButton = view.findViewById(R.id.set_pin_code);
        setPinCodeButton.setOnClickListener(v -> {
            buildSetPINCodeDialog();
        });

        FlexboxLayout setEncryptionKeyButton = view.findViewById(R.id.set_encryption_key);
        setEncryptionKeyButton.setOnClickListener(v -> {
            buildSetEncryptionKeyDialog();
        });

        FlexboxLayout howToButton = view.findViewById(R.id.how_to_button);
        howToButton.setOnClickListener(v -> {
            View webViewDialogView = View.inflate(getContext(), R.layout.dialog_webview, null);
            Button closeButton = webViewDialogView.findViewById(R.id.close_button);
            Dialog webViewDialog = new androidx.appcompat.app.AlertDialog.Builder(getContext()).setView(webViewDialogView).create();
            closeButton.setOnClickListener(w -> webViewDialog.dismiss());
            webViewDialog.show();
            webViewDialog.getWindow().setLayout(1200, 900);
            WebView webView = webViewDialogView.findViewById(R.id.dialog_webview);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("https://drive.google.com/file/d/1OSnrUnQwggod2IwialnfbJ32nT-1q9mQ/view?usp=sharing");
        });

        FlexboxLayout hideStationControlsLayout = view.findViewById(R.id.hide_station_controls);
        SwitchCompat hideStationControlsToggle = view.findViewById(R.id.hide_station_controls_toggle);
        hideStationControlsToggle.setChecked(mViewModel.getHideStationControls().getValue().booleanValue());
        hideStationControlsLayout.setOnClickListener(v -> {
            hideStationControlsToggle.setChecked(!hideStationControlsToggle.isChecked());
        });

        CompoundButton.OnCheckedChangeListener hideStationControlsToggleListener = (compoundButton, isChecked) -> {
            mViewModel.setHideStationControls(isChecked);
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

    private void buildSetPINCodeDialog() {
        View view = View.inflate(getContext(), R.layout.dialog_set_pin, null);
        EditText newPin = view.findViewById(R.id.pin_code_input);
        Button pinConfirmButton = view.findViewById(R.id.pin_confirm_button);
        AlertDialog pinDialog = new AlertDialog.Builder(getContext()).setView(view).create();
        pinConfirmButton.setOnClickListener(v -> {
            mViewModel.setPinCode(newPin.getText().toString());
            pinDialog.dismiss();
        });
        pinDialog.show();
    }

    private void buildSetEncryptionKeyDialog() {
        View view = View.inflate(getContext(), R.layout.dialog_set_encryption_key, null);
        EditText newKey = view.findViewById(R.id.encryption_key_input);
        Button encryptionKeyConfirmButton = view.findViewById(R.id.encryption_key_confirm);
        AlertDialog encryptionDialog = new AlertDialog.Builder(getContext()).setView(view).create();
        encryptionKeyConfirmButton.setOnClickListener(v -> {
            mViewModel.setEncryptionKey(newKey.getText().toString());
            encryptionDialog.dismiss();
        });
        encryptionDialog.show();
    }
}