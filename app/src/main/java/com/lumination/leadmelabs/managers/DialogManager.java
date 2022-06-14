package com.lumination.leadmelabs.managers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.SteamApplicationAdapter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for handling alert dialogs.
 */
public class DialogManager {
    public static androidx.appcompat.app.AlertDialog gameLaunchDialog;
    public static List<Integer> gameLaunchStationIds;
    public static AlertDialog reconnectDialog;
    public static List<Integer> endSessionStationIds;
    public static AlertDialog endSessionDialog;

    private static int pinCodeAttempts = 0;

    /**
     * Build and show the URL dialog box. The input from the edit text field is send to the stations
     * and launched on the default web browser.
     */
    public static void buildURLDialog(Context context, FragmentStationSingleBinding binding) {
        View view = View.inflate(context, R.layout.dialog_enter_url, null);
        AlertDialog urlDialog = new androidx.appcompat.app.AlertDialog.Builder(context).setView(view).create();

        EditText url = view.findViewById(R.id.url_input);
        url.requestFocus();
        TextView errorText = view.findViewById(R.id.error_text);

        Button submit = view.findViewById(R.id.submit_button);
        submit.setOnClickListener(v -> {
            errorText.setVisibility(View.GONE);
            String input = url.getText().toString();

            if (Patterns.WEB_URL.matcher(input).matches()) {
                Station selectedStation = binding.getSelectedStation();
                selectedStation.gameName = input;
                NetworkService.sendMessage("Station," + binding.getSelectedStation().id, "CommandLine", "URL:" + input);
                StationSingleFragment.mViewModel.updateStationById(selectedStation.id, selectedStation);
                urlDialog.dismiss();
            } else {
                errorText.setText(R.string.invalid_url);
                errorText.setVisibility(View.VISIBLE);
            }
        });

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> urlDialog.dismiss());

        urlDialog.show();
    }

    /**
     * Build and launch the shutdown dialog box for stations. Each time this is build a timer is
     * started to allow the users to cancel the shutdown operation.
     */
    public static void buildShutdownDialog(Context context, FragmentStationSingleBinding binding, CompoundButton compoundButton) {
        View view = View.inflate(context, R.layout.dialog_template, null);
        AlertDialog confirmDialog = new androidx.appcompat.app.AlertDialog.Builder(context).setView(view).create();
        confirmDialog.setCancelable(false);
        confirmDialog.setCanceledOnTouchOutside(false);

        TextView title = view.findViewById(R.id.title);
        TextView contentText = view.findViewById(R.id.content_text);
        title.setText(R.string.shutting_down);
        contentText.setText(R.string.cancel_shutdown);

        Station selectedStation = binding.getSelectedStation();
        NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "Shutdown");

        Button confirmButton = view.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> confirmDialog.dismiss());
        confirmButton.setText(R.string._continue);

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setText(R.string.cancel_10);

        confirmDialog.show();
        confirmDialog.getWindow().setLayout(1200, 380);

        CountDownTimer timer = new CountDownTimer(9000, 1000) {
            @Override
            public void onTick(long l) {
                cancelButton.setText(MessageFormat.format("Cancel ({0})", (l + 1000) / 1000));
            }

            @Override
            public void onFinish() {
                confirmDialog.dismiss();
            }
        }.start();

        cancelButton.setOnClickListener(x -> {
            NetworkService.sendMessage("Station," + selectedStation.id, "CommandLine", "CancelShutdown");
            compoundButton.setChecked(true);
            timer.cancel();
            confirmDialog.dismiss();
        });
    }

    /**
     * Build the set NUC dialog. A user is able to auto scan for nearby NUC's or input the IP address
     * manually.
     */
    public static void buildSetNucDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_set_nuc, null);
        AlertDialog nucDialog = new AlertDialog.Builder(context).setView(view).create();

        SettingsFragment.mViewModel.getNuc().observe(SettingsFragment.getInstance().getViewLifecycleOwner(), nucAddress -> {
            TextView textView = view.findViewById(R.id.nuc_address);
            textView.setText(nucAddress);
        });

        Button scan_button = view.findViewById(R.id.scan_nuc_address);
        scan_button.setOnClickListener(v -> NetworkService.broadcast("Android"));

        EditText newAddress = view.findViewById(R.id.nuc_address_input);
        Button setAddress = view.findViewById(R.id.set_nuc_button);
        setAddress.setOnClickListener(v -> {
            SettingsFragment.mViewModel.setNucAddress(newAddress.getText().toString());
            nucDialog.dismiss();
        });

        nucDialog.show();
    }

    /**
     * Build the set PIN dialog. Users can set a custom pin to lock the settings menu with.
     */
    public static void buildSetPINCodeDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_set_pin, null);
        AlertDialog pinDialog = new AlertDialog.Builder(context).setView(view).create();

        EditText newPin = view.findViewById(R.id.pin_code_input);

        Button pinConfirmButton = view.findViewById(R.id.pin_confirm_button);
        pinConfirmButton.setOnClickListener(v -> {
            SettingsFragment.mViewModel.setPinCode(newPin.getText().toString());
            pinDialog.dismiss();
        });

        pinDialog.show();
    }

    public static void confirmPinCode(SideMenuFragment sideMenuFragment, String navigationType) {
        pinCodeAttempts = 0;
        View view = View.inflate(sideMenuFragment.getContext(), R.layout.dialog_pin, null);
        AlertDialog pinDialog = new AlertDialog.Builder(sideMenuFragment.getContext()).setView(view).create();

        pinDialog.show();
        EditText pinEditText = view.findViewById(R.id.pin_code_input);
        pinEditText.requestFocus();
        view.findViewById(R.id.pin_confirm_button).setOnClickListener(w -> {
            View errorMessage = view.findViewById(R.id.pin_error);
            errorMessage.setVisibility(View.GONE);

            String pinCode = SettingsFragment.mViewModel.getPinCode().getValue();

            if (pinCode != null) {
                String pinInput = pinEditText.getText().toString();
                String luminationOverridePin = "5864628466"; // workaround for Lumination tech support, put in PIN for l-u-m-i-n-a-t-i-o-n and press 5 times

                if (pinInput.equals(luminationOverridePin)) {
                    pinCodeAttempts++;
                } else {
                    pinCodeAttempts = 0;
                }

                if (pinCode.equals(pinInput) || (pinCodeAttempts >= 5 && pinInput.equals(luminationOverridePin))) {
                    sideMenuFragment.navigateToSettingsPage(navigationType);
                    pinDialog.dismiss();
                } else {
                    errorMessage.setVisibility(View.VISIBLE);
                }
            }
            //TODO delete after testing - lets user through without inputting a pin
            else {
                sideMenuFragment.navigateToSettingsPage(navigationType);
                pinDialog.dismiss();
            }
        });

        view.findViewById(R.id.pin_cancel_button).setOnClickListener(w -> {
            pinDialog.dismiss();
        });
    }

    /**
     * Build the set encryption key dialog. The encryption keys is what encodes and decodes messages
     * between the tablet, NUC & stations.
     */
    public static void buildSetEncryptionKeyDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_set_encryption_key, null);
        AlertDialog encryptionDialog = new AlertDialog.Builder(context).setView(view).create();

        EditText newKey = view.findViewById(R.id.encryption_key_input);

        Button encryptionKeyConfirmButton = view.findViewById(R.id.encryption_key_confirm);
        encryptionKeyConfirmButton.setOnClickListener(v -> {
            SettingsFragment.mViewModel.setEncryptionKey(newKey.getText().toString());
            encryptionDialog.dismiss();
        });

        encryptionDialog.show();
    }

    /**
     * Build a custom WebView dialog. The view will load the supplied URL.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void buildWebViewDialog(Context context, String URL) {
        View webViewDialogView = View.inflate(context, R.layout.dialog_webview, null);
        Dialog webViewDialog = new androidx.appcompat.app.AlertDialog.Builder(context).setView(webViewDialogView).create();

        Button closeButton = webViewDialogView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(w -> webViewDialog.dismiss());

        WebView webView = webViewDialogView.findViewById(R.id.dialog_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(URL);

        webViewDialog.show();
        webViewDialog.getWindow().setLayout(1200, 900);
    }

    /**
     * Build and display the reconnection dialog. Can either be dismissed through the close button
     * or a user can select reconnect, sending a UDP broadcast out from the network service looking
     * for active NUCs.
     */
    public static void buildReconnectDialog() {
        View reconnectDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_reconnect, null);
        reconnectDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.getInstance()).setView(reconnectDialogView).create();
        reconnectDialog.setCancelable(false);
        reconnectDialog.setCanceledOnTouchOutside(false);
        reconnectDialogView.findViewById(R.id.reconnect_failed).setVisibility(View.GONE);

        Button reconnectButton = reconnectDialogView.findViewById(R.id.reconnect_button);
        reconnectButton.setOnClickListener(w -> {
            reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.VISIBLE);
            reconnectDialogView.findViewById(R.id.reconnect_failed).setVisibility(View.GONE);
            NetworkService.broadcast("Android");
            new java.util.Timer().schedule( // turn animations back on after the scenes have updated
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            MainActivity.runOnUI(() -> {
                                reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
                                reconnectDialogView.findViewById(R.id.reconnect_failed).setVisibility(View.VISIBLE);
                            });
                        }
                    },
                    10000
            );
        });

        Button closeReconnectDialogButton = reconnectDialogView.findViewById(R.id.close_reconnect_dialog);
        closeReconnectDialogButton.setOnClickListener(w -> {
            reconnectDialogView.findViewById(R.id.reconnect_failed).setVisibility(View.GONE);
            reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
            reconnectDialog.dismiss();
        });

        reconnectDialog.show();
        reconnectDialog.getWindow().setLayout(1200, 450);
    }

    /**
     * Build the launch confirmation dialog when launching a new steam experience from the station
     * selection screen.
     */
    public static void buildSelectionLaunch(Context context, ArrayList<String> theatreStations, int steamGameId, int[] selectedIds)  {
        View confirmDialogView = View.inflate(context, R.layout.dialog_confirm, null);
        AlertDialog confirmDialog = new AlertDialog.Builder(context).setView(confirmDialogView).create();

        TextView headingText = confirmDialogView.findViewById(R.id.heading_text);
        headingText.setText(R.string.exit_theatre);

        TextView contentText = confirmDialogView.findViewById(R.id.content_text);
        contentText.setText(MessageFormat.format("{0}{1} currently in theatre mode. Are you sure you want to exit theatre mode?", String.join(", ", theatreStations), theatreStations.size() > 1 ? " are" : " is"));

        Button confirmButton = confirmDialogView.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> StationSelectionFragment.getInstance().confirmLaunchGame(selectedIds, steamGameId, confirmDialog));

        Button cancelButton = confirmDialogView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(x -> confirmDialog.dismiss());
        confirmDialog.show();
    }

    /**
     * Build the launch confirmation dialog when launching a new steam experience.
     */
    public static void buildLaunchExperienceDialog(Context context, SteamApplication steamApplication, Station station) {
        View confirmDialogView = View.inflate(context, R.layout.dialog_confirm, null);
        AlertDialog confirmDialog = new AlertDialog.Builder(context).setView(confirmDialogView).create();

        TextView headingText = confirmDialogView.findViewById(R.id.heading_text);
        headingText.setText(R.string.exit_theatre);

        TextView contentText = confirmDialogView.findViewById(R.id.content_text);
        contentText.setText(MessageFormat.format("{0}{1}", station.name, R.string.exit_current_theatre_mode));

        Button confirmButton = confirmDialogView.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> {
            NetworkService.sendMessage("Station," + SteamApplicationAdapter.stationId, "Steam", "Launch:" + steamApplication.id);
            SideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard");
            confirmDialog.dismiss();
            awaitStationGameLaunch(new int[] { station.id }, steamApplication.name);
        });

        Button cancelButton = confirmDialogView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(x -> confirmDialog.dismiss());

        confirmDialog.show();
    }

    /**
     * Displays a dialog to inform users that a game is currently launching on a number of stations.
     * This is dismisses manually by the user or automatically when the NUC sends back confirmation
     * from the stations.
     */
    public static void awaitStationGameLaunch(int[] stationIds, String gameName)
    {
        View gameLaunchDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_template, null);
        gameLaunchDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.getInstance()).setView(gameLaunchDialogView).create();

        TextView title = gameLaunchDialogView.findViewById(R.id.title);
        title.setText(R.string.launching_game);

        TextView contentText = gameLaunchDialogView.findViewById(R.id.content_text);
        contentText.setText(MessageFormat.format("Launching {0} on {1}", gameName, String.join(", ", StationsFragment.mViewModel.getStationNames(stationIds))));

        gameLaunchStationIds = new ArrayList<>(stationIds.length);
        for (int i : stationIds)
        {
            gameLaunchStationIds.add(i);
        }

        Button confirmButton = gameLaunchDialogView.findViewById(R.id.confirm_button);
        confirmButton.setText(R.string.dismiss);
        confirmButton.setOnClickListener(w -> gameLaunchDialog.dismiss());

        Button cancelButton = gameLaunchDialogView.findViewById(R.id.cancel_button);
        cancelButton.setVisibility(View.GONE);

        gameLaunchDialog.show();
        gameLaunchDialog.getWindow().setLayout(1200, 380);
    }

    /**
     * A game has launched on all selected stations so the game launch dialog is dismissed.
     */
    public static void gameLaunchedOnStation(int stationId) {
        if (gameLaunchStationIds != null) {
            gameLaunchStationIds.removeIf(id -> id == stationId);
            if (gameLaunchStationIds.size() == 0) {
                if (gameLaunchDialog != null) {
                    gameLaunchDialog.dismiss();
                }
            }
        }
    }

    public static void awaitStationEndSession(int[] stationIds)
    {
        View endSessionDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_template, null);
        endSessionDialog = new AlertDialog.Builder(MainActivity.getInstance()).setView(endSessionDialogView).create();

        TextView title = endSessionDialogView.findViewById(R.id.title);
        title.setText(R.string.ending_session);

        TextView contentText = endSessionDialogView.findViewById(R.id.content_text);
        contentText.setText(MessageFormat.format("Ending session on {0}", String.join(", ", StationsFragment.mViewModel.getStationNames(stationIds))));

        endSessionStationIds =  new ArrayList<Integer>(stationIds.length);
        for (int i : stationIds)
        {
            endSessionStationIds.add(i);
        }

        Button confirmButton = endSessionDialogView.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> endSessionDialog.dismiss());
        confirmButton.setText(R.string.dismiss);

        Button cancelButton = endSessionDialogView.findViewById(R.id.cancel_button);
        cancelButton.setVisibility(View.GONE);

        endSessionDialog.show();
        endSessionDialog.getWindow().setLayout(1200, 380);
    }

    public static void sessionEndedOnStation(int stationId) {
        if (endSessionStationIds != null) {
            endSessionStationIds.removeIf(id -> id == stationId);
            if (endSessionStationIds.size() == 0) {
                if (endSessionDialog != null) {
                    endSessionDialog.dismiss();
                }
            }
        }
    }
}
