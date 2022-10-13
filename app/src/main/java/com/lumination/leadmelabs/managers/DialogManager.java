package com.lumination.leadmelabs.managers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.BasicStationSelectionAdapter;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.SteamApplicationAdapter;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.WakeOnLan;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Responsible for handling alert dialogs.
 */
public class DialogManager {
    private static final HashMap<String, AlertDialog> openDialogs = new HashMap<>();

    public static androidx.appcompat.app.AlertDialog gameLaunchDialog;
    public static List<Integer> gameLaunchStationIds;
    public static AlertDialog reconnectDialog;
    public static List<Integer> endSessionStationIds;
    public static AlertDialog endSessionDialog;
    public static List<Integer> restartSessionStationIds;
    public static AlertDialog restartSessionDialog;

    public static CountDownTimer shutdownTimer;

    private static int pinCodeAttempts = 0;

    /**
     * Dismiss an open dialog that is no longer relevant. Basic dialogs are kept track of within a
     * hashmap with a combination of the dialog title and station name used as a key.
     * @param titleKey A string representing the title of the dialog
     * @param stationName A string representing which station the dialog belongs to
     */
    public static void closeOpenDialog(String titleKey, String stationName) {
        String key = titleKey + ":" + stationName;
        AlertDialog toBeClosed = openDialogs.remove(key);
        if(toBeClosed != null) {
            MainActivity.runOnUI(toBeClosed::dismiss);
        }
    }

    /**
     * Dismiss an open dialog that is no longer relevant. Basic dialogs are kept track of within a
     * hashmap with a combination of the dialog title and station name used as a key.
     * @param titleKey A string representing the title of the dialog
     * @param stationName A string representing which station the dialog belongs to
     * @param dialog An alert dialog that is to be tracked
     */
    public static void trackOpenDialog(String titleKey, String stationName, AlertDialog dialog) {
        String key = titleKey + ":" + stationName;
        openDialogs.put(key, dialog);
    }

    /**
     * Create a basic dialog box with a custom title and content based on the strings that are
     * passed in.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     */
    public static void createBasicDialog(String titleText, String contentText) {
        createBasicDialog(titleText, contentText, null);
    }

    /**
     * Create a basic dialog box with a custom title and content based on the strings that are
     * passed in.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     * @param stationName A string representing if the dialog box reflects a stations status.
     */
    public static void createBasicDialog(String titleText, String contentText, String stationName) {
        View basicDialogView = View.inflate(MainActivity.getInstance(), R.layout.alert_dialog_basic_vern, null);
        AlertDialog basicDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(basicDialogView).create();

        if(stationName != null) {
            trackOpenDialog(titleText, stationName, basicDialog);
            basicDialog.setOnDismissListener(v -> closeOpenDialog(titleText, stationName));
        }

        TextView title = basicDialogView.findViewById(R.id.title);
        title.setText(titleText);

        TextView contentView = basicDialogView.findViewById(R.id.content_text);
        contentView.setText(contentText);

        Button cancelButton = basicDialogView.findViewById(R.id.close_dialog);
        cancelButton.setOnClickListener(w -> {
            basicDialog.dismiss();
            closeOpenDialog(titleText, stationName);
        });

        basicDialog.show();
        basicDialog.getWindow().setLayout(680, 680);
    }

    /**
     * Create a basic dialog box with a custom title and content based on the strings that are
     * passed in.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     */
    public static void createConfirmationDialog(String titleText, String contentText) {
        BooleanCallbackInterface booleanCallbackInterface = new BooleanCallbackInterface() {
            @Override
            public void callback(boolean result) { }
        };
        createConfirmationDialog(titleText, contentText, booleanCallbackInterface, "Cancel", "Confirm");
    }

    /**
     * Create a basic dialog box with a custom title and content based on the strings that are
     * passed in.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     * @param booleanCallbackInterface A callback to be called on cancel or confirm. Will call the callback with true on confirm and false on cancel
     */
    public static void createConfirmationDialog(String titleText, String contentText, BooleanCallbackInterface booleanCallbackInterface) {
        createConfirmationDialog(titleText, contentText, booleanCallbackInterface, "Cancel", "Confirm");
    }

    /**
     * Create a basic dialog box with a custom title and content based on the strings that are
     * passed in.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     * @param booleanCallbackInterface A callback to be called on cancel or confirm. Will call the callback with true on confirm and false on cancel
     */
    public static void createConfirmationDialog(String titleText, String contentText, BooleanCallbackInterface booleanCallbackInterface, String cancelButtonText, String confirmButtonText) {
        View confirmationDialogView = View.inflate(MainActivity.getInstance(), R.layout.alert_dialog_warning_vern, null);
        AlertDialog confirmationDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(confirmationDialogView).create();

        TextView title = confirmationDialogView.findViewById(R.id.title);
        title.setText(titleText);

        TextView contentView = confirmationDialogView.findViewById(R.id.content_text);
        contentView.setText(contentText);

        ImageView vernImage = confirmationDialogView.findViewById(R.id.icon_vern);
        vernImage.setBackgroundResource(R.drawable.vern_warning);

        Button confirmButton = confirmationDialogView.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> {
            booleanCallbackInterface.callback(true);
            confirmationDialog.dismiss();
        });
        confirmButton.setText(confirmButtonText);

        Button cancelButton = confirmationDialogView.findViewById(R.id.close_dialog);
        cancelButton.setOnClickListener(w -> {
            booleanCallbackInterface.callback(false);
            confirmationDialog.dismiss();
        });
        cancelButton.setText(cancelButtonText);

        confirmationDialog.setCancelable(false);
        confirmationDialog.show();
        confirmationDialog.getWindow().setLayout(680, 680);
    }

    public static void createEndSessionDialog(ArrayList<Station> stations) {
        View view = View.inflate(MainActivity.getInstance(), R.layout.dialog_select_stations, null);
        AlertDialog endSessionDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.getInstance()).setView(view).create();

        TextView title = view.findViewById(R.id.title);
        title.setText(R.string.select_stations);

        TextView contentView = view.findViewById(R.id.content_text);
        contentView.setText(R.string.end_session_on);

        RecyclerView recyclerView = view.findViewById(R.id.stations_list);
        recyclerView.setLayoutManager(new GridLayoutManager(endSessionDialog.getContext(), 3));

        BasicStationSelectionAdapter stationAdapter = new BasicStationSelectionAdapter();
        stations = Helpers.cloneStationList(stations);
        stationAdapter.stationList = stations;
        recyclerView.setAdapter(stationAdapter);

        Button confirmButton = view.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> {
            int[] selectedIds = Helpers.cloneStationList(stationAdapter.stationList).stream().filter(station -> station.selected).mapToInt(station -> station.id).toArray();
            String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

            NetworkService.sendMessage("Station," + stationIds, "CommandLine", "StopGame");
            endSessionDialog.dismiss();
        });

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(w -> endSessionDialog.dismiss());

        endSessionDialog.show();
    }

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
     * Build and show the rename station dialog box.
     */
    public static void buildRenameStationDialog(Context context, FragmentStationSingleBinding binding) {
        View view = View.inflate(context, R.layout.dialog_rename_station, null);
        AlertDialog renameStationDialog = new androidx.appcompat.app.AlertDialog.Builder(context).setView(view).create();

        EditText nameInput = view.findViewById(R.id.name_input);
        nameInput.requestFocus();
        TextView errorText = view.findViewById(R.id.error_text);

        Button submit = view.findViewById(R.id.submit_button);
        submit.setOnClickListener(v -> {
            errorText.setVisibility(View.GONE);
            String input = nameInput.getText().toString();

            if (Pattern.matches("([A-Za-z0-9 ])+", input)) {
                Station selectedStation = binding.getSelectedStation();
                selectedStation.setName(input);
                StationSingleFragment.mViewModel.updateStationById(selectedStation.id, selectedStation);
                NetworkService.sendMessage("NUC", "UpdateStation", selectedStation.id + ":SetValue:name:" + input);

                renameStationDialog.dismiss();
            } else {
                errorText.setText("Station name can only contains letters, numbers and spaces");
                errorText.setVisibility(View.VISIBLE);
            }
        });

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> renameStationDialog.dismiss());

        renameStationDialog.show();
    }

    /**
     * Build and launch the shutdown dialog box for stations. Each time this is build a timer is
     * started to allow the users to cancel the shutdown operation.
     */
    public static void buildShutdownDialog(Context context, int[] stationIds) {
        CountdownCallbackInterface countdownCallbackInterface = result -> { };
        buildShutdownDialog(context, stationIds, countdownCallbackInterface);
    }

    /**
     * Build and launch the shutdown dialog box for stations. Each time this is build a timer is
     * started to allow the users to cancel the shutdown operation.
     */
    public static void buildShutdownDialog(Context context, int[] stationIds, CountdownCallbackInterface countdownCallbackInterface) {
        View view = View.inflate(context, R.layout.dialog_template, null);
        AlertDialog confirmDialog = new androidx.appcompat.app.AlertDialog.Builder(context, R.style.AlertDialogTheme).setView(view).create();
        confirmDialog.setCancelable(false);
        confirmDialog.setCanceledOnTouchOutside(false);
        //confirmDialog.getWindow().setBackgroundDrawable(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.bg_white_alertdialog_curved, null));

        TextView title = view.findViewById(R.id.title);
        TextView contentText = view.findViewById(R.id.content_text);
        title.setText(R.string.shutting_down);
        contentText.setText(R.string.cancel_shutdown);

        String stationIdsString = String.join(", ", Arrays.stream(stationIds).mapToObj(String::valueOf).toArray(String[]::new));

        NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", "Shutdown");

        Button confirmButton = view.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> confirmDialog.dismiss());
        confirmButton.setText(R.string._continue);

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setText(R.string.cancel_10);

        confirmDialog.show();
        confirmDialog.getWindow().setLayout(1200, 380);

        shutdownTimer = new CountDownTimer(9000, 1000) {
            @Override
            public void onTick(long l) {
                cancelButton.setText(MessageFormat.format("Cancel ({0})", (l + 1000) / 1000));
                countdownCallbackInterface.callback((int) (l + 1000) / 1000);
            }

            @Override
            public void onFinish() {
                confirmDialog.dismiss();
                countdownCallbackInterface.callback(0);
            }
        }.start();

        cancelButton.setOnClickListener(x -> {
            NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", "CancelShutdown");
            countdownCallbackInterface.callback(0);
            shutdownTimer.cancel();
            confirmDialog.dismiss();

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_ids", stationIdsString);
            }};
            FirebaseManager.logAnalyticEvent("shutdown_cancelled", analyticsAttributes);
        });
    }

    /**
     * Build the set NUC dialog. A user is able to auto scan for nearby NUC's or input the IP address
     * manually.
     */
    public static void buildNucDetailsDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_nuc_details, null);
        AlertDialog nucDetailsDialog = new AlertDialog.Builder(context).setView(view).create();

        SettingsFragment.mViewModel.getNuc().observe(SettingsFragment.getInstance().getViewLifecycleOwner(), nucAddress -> {
            TextView textView = view.findViewById(R.id.nuc_ip_address);
            textView.setText(nucAddress);
        });

        SettingsFragment.mViewModel.getNucMac().observe(SettingsFragment.getInstance().getViewLifecycleOwner(), nucMacAddress -> {
            TextView textView = view.findViewById(R.id.nuc_mac_address);
            textView.setText(nucMacAddress);
        });

        EditText newMacAddress = view.findViewById(R.id.nuc_address_input);
        Button setAddress = view.findViewById(R.id.set_nuc_mac_button);
        setAddress.setOnClickListener(v -> {
            SettingsFragment.mViewModel.setNucMacAddress(newMacAddress.getText().toString());
        });

        Button wakeNuc = view.findViewById(R.id.wake_nuc_button);
        wakeNuc.setOnClickListener(v -> {
            if(SettingsFragment.mViewModel.getNucMac().getValue() != null) {
                WakeOnLan.WakeNUCOnLan();
            } else {
                Toast.makeText(context, "NUC MAC address needs to be set.", Toast.LENGTH_LONG).show();
            }
        });

        nucDetailsDialog.show();
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
        scan_button.setOnClickListener(v -> {
            NetworkService.broadcast("Android");
            nucDialog.dismiss();
        });

        EditText newAddress = view.findViewById(R.id.nuc_address_input);
        Button setAddress = view.findViewById(R.id.set_nuc_button);
        setAddress.setOnClickListener(v -> {
            SettingsFragment.mViewModel.setNucAddress(newAddress.getText().toString());
            nucDialog.dismiss();
        });

        Button refreshAddress = view.findViewById(R.id.refresh_nuc_button);
        refreshAddress.setOnClickListener(v -> {
            if(NetworkService.getNUCAddress() != null) {
                NetworkService.refreshNUCAddress();
                nucDialog.dismiss();
            } else {
                Toast.makeText(context, "NUC address needs to be set.", Toast.LENGTH_LONG).show();
            }
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

        view.findViewById(R.id.pin_cancel_button).setOnClickListener(w -> pinDialog.dismiss());
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
     * Build the set encryption key dialog. The encryption keys is what encodes and decodes messages
     * between the tablet, NUC & stations.
     */
    public static void buildSetLabLocationDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_set_lab_location, null);
        AlertDialog labLocationDialog = new AlertDialog.Builder(context).setView(view).create();

        EditText newLabLocation = view.findViewById(R.id.lab_location_input);

        Button confirmButton = view.findViewById(R.id.lab_location_confirm);
        confirmButton.setOnClickListener(v -> {
            SettingsFragment.mViewModel.setLabLocation(newLabLocation.getText().toString());
            labLocationDialog.dismiss();
        });

        labLocationDialog.show();
    }

    /**
     * Build the set license key dialog. The license key is what determines if the program is running
     * with a valid license.
     */
    public static void buildSetLicenseKeyDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_set_license_key, null);
        AlertDialog licenseDialog = new AlertDialog.Builder(context).setView(view).create();

        EditText newKey = view.findViewById(R.id.license_key_input);

        Button encryptionKeyConfirmButton = view.findViewById(R.id.license_key_confirm);
        encryptionKeyConfirmButton.setOnClickListener(v -> {
            SettingsFragment.mViewModel.setLicenseKey(newKey.getText().toString());
            licenseDialog.dismiss();
        });

        licenseDialog.show();
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
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(URL);
        webView.getSettings().setJavaScriptEnabled(true);

        webViewDialog.show();
        webViewDialog.getWindow().setLayout(1200, 900);
    }

    /**
     * Build and display the reconnection dialog. Can either be dismissed through the close button
     * or a user can select reconnect, sending a UDP broadcast out from the network service looking
     * for active NUCs.
     */
    public static void buildReconnectDialog() {
        View reconnectDialogView = View.inflate(MainActivity.getInstance(), R.layout.alert_dialog_lost_server, null);
        reconnectDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(reconnectDialogView).create();
        reconnectDialog.setCancelable(false);
        reconnectDialog.setCanceledOnTouchOutside(false);

        //Configure the text title/content
        TextView title = reconnectDialogView.findViewById(R.id.title);
        title.setText(R.string.lost_server_connection);

        ImageView vernImage = reconnectDialogView.findViewById(R.id.icon_vern);
        vernImage.setBackgroundResource(R.drawable.vern_lost_server);

        TextView content = reconnectDialogView.findViewById(R.id.content_text);
        content.setText(R.string.lost_server_message_content);

        Button reconnectButton = reconnectDialogView.findViewById(R.id.confirm_button);
        reconnectButton.setVisibility(View.VISIBLE);
        reconnectButton.setText(R.string.reconnect);
        reconnectButton.setOnClickListener(w -> {
            reconnectButton.setVisibility(View.GONE);
            reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
            //NetworkService.broadcast("Android");

            if(NetworkService.getNUCAddress() != null) {
                NetworkService.refreshNUCAddress();
            }

            new java.util.Timer().schedule( // turn animations back on after the scenes have updated
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            MainActivity.runOnUI(() -> {
                                reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
                                content.setText(R.string.reconnection_failed);
                                content.setVisibility(View.VISIBLE);
                                reconnectButton.setVisibility(View.VISIBLE);
                            });
                        }
                    },
                    10000
            );
        });

        Button closeReconnectDialogButton = reconnectDialogView.findViewById(R.id.close_dialog);
        closeReconnectDialogButton.setOnClickListener(w -> {
            content.setText(R.string.lost_server_message_content);
            reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
            reconnectDialog.dismiss();
            MainActivity.hasNotReceivedPing = 0;
        });

        reconnectDialog.setOnDismissListener(v -> {
            MainActivity.startNucPingMonitor();
        });

        reconnectDialog.show();
        reconnectDialog.getWindow().setLayout(680, 680);
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
        confirmButton.setOnClickListener(w -> StationSelectionPageFragment.getInstance().confirmLaunchGame(selectedIds, steamGameId, confirmDialog));

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
            awaitStationGameLaunch(new int[] { station.id }, steamApplication.name, false);
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
    public static void awaitStationGameLaunch(int[] stationIds, String gameName, boolean restarting)
    {
        View gameLaunchDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_template, null);
        gameLaunchDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.getInstance()).setView(gameLaunchDialogView).create();

        TextView title = gameLaunchDialogView.findViewById(R.id.title);
        title.setText(restarting ? "Restarting Experience" : "Launching Experience");

        TextView contentText = gameLaunchDialogView.findViewById(R.id.content_text);
        contentText.setText(MessageFormat.format("{0} {1} on {2}", restarting ? "Restarting" : "Launching", gameName, String.join(", ", StationsFragment.mViewModel.getStationNames(stationIds))));

        gameLaunchStationIds = new ArrayList<>(stationIds.length);
        for (int i : stationIds)
        {
            gameLaunchStationIds.add(i);

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("experience_name", gameName);
                put("station_id", String.valueOf(i));
            }};
            FirebaseManager.logAnalyticEvent(restarting ? "experience_restarted" : "experience_launched", analyticsAttributes);
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

        endSessionStationIds =  new ArrayList<>(stationIds.length);
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

    public static void awaitStationRestartSession(int[] stationIds)
    {
        View restartSessionDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_template, null);
        restartSessionDialog = new AlertDialog.Builder(MainActivity.getInstance()).setView(restartSessionDialogView).create();

        TextView title = restartSessionDialogView.findViewById(R.id.title);
        title.setText(R.string.restarting_system);

        TextView contentText = restartSessionDialogView.findViewById(R.id.content_text);
        contentText.setText(MessageFormat.format("Restarting system on {0}", String.join(", ", StationsFragment.mViewModel.getStationNames(stationIds))));

        restartSessionStationIds =  new ArrayList<>(stationIds.length);
        for (int i : stationIds)
        {
            restartSessionStationIds.add(i);
        }

        Button confirmButton = restartSessionDialogView.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> restartSessionDialog.dismiss());
        confirmButton.setText(R.string.dismiss);

        Button cancelButton = restartSessionDialogView.findViewById(R.id.cancel_button);
        cancelButton.setVisibility(View.GONE);

        restartSessionDialog.show();
        restartSessionDialog.getWindow().setLayout(1200, 380);
    }

    public static void sessionRestartedOnStation(int stationId) {
        if (restartSessionStationIds != null) {
            restartSessionStationIds.removeIf(id -> id == stationId);
            if (restartSessionStationIds.size() == 0) {
                if (restartSessionDialog != null) {
                    restartSessionDialog.dismiss();
                }
            }
        }
    }
}
