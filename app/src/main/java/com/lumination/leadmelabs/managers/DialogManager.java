package com.lumination.leadmelabs.managers;

import static com.lumination.leadmelabs.utilities.WakeOnLan.WakeById;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alimuzaffar.lib.pin.PinEntryEditText;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.lumination.leadmelabs.databinding.FragmentStationSingleBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.interfaces.CountdownCallbackInterface;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.models.applications.details.Details;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.dashboard.DashboardFragment;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.library.application.Adapters.GlobalAdapter;
import com.lumination.leadmelabs.ui.library.application.Adapters.LevelAdapter;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.RoomAdapter;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.BasicStationSelectionAdapter;
import com.lumination.leadmelabs.ui.stations.StationSingleFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.WakeOnLan;
import com.segment.analytics.Properties;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.sentry.Sentry;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Responsible for handling alert dialogs.
 */
public class DialogManager {
    private static final HashMap<String, AlertDialog> openDialogs = new HashMap<>();

    public static AlertDialog steamGuardEntryDialog;
    public static AlertDialog gameLaunchDialog;
    public static List<Integer> gameLaunchStationIds;
    public static AlertDialog reconnectDialog;
    public static List<Integer> endSessionStationIds;
    public static AlertDialog endSessionDialog;
    public static List<Integer> restartVRSystemStationIds;
    public static AlertDialog restartVRSystemDialog;

    public static CountDownTimer shutdownTimer;

    private static int pinCodeAttempts = 0;
    private static boolean missingEncryptionAlerted = false;

    private static long submitModalLastSubmitClick = 0;

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
     * Create a basic dialog box that displays the lack of encryption key. This is a separate function
     * so that we can stop it from stacking up by monitoring if it is open.
     */
    public static void createMissingEncryptionDialog(String titleText, String contentText) {
        if(missingEncryptionAlerted) {
            return;
        }

        View basicDialogView = View.inflate(MainActivity.getInstance(), R.layout.alert_dialog_basic_vern, null);
        AlertDialog basicDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(basicDialogView).create();

        TextView title = basicDialogView.findViewById(R.id.title);
        title.setText(titleText);

        TextView contentView = basicDialogView.findViewById(R.id.content_text);
        contentView.setText(contentText);

        Button cancelButton = basicDialogView.findViewById(R.id.close_dialog);
        cancelButton.setOnClickListener(w -> {
            basicDialog.dismiss();
            missingEncryptionAlerted = false;
            trackDialogDismissed("Missing Encryption Key");
        });

        missingEncryptionAlerted = true;
        basicDialog.show();
        if (basicDialog.getWindow() != null) {
            basicDialog.getWindow().setLayout(680, 680);
        }
        trackDialogShown("Missing Encryption Key");
    }

    /**
     * Create a basic dialog box that displays the lack of encryption key. This is a separate function
     * so that we can stop it from stacking up by monitoring if it is open.
     */
    public static void createSubmitTicketDialog() {
        View dialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_submit_ticket, null);
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(dialogView).create();

        TextView errorText = dialogView.findViewById(R.id.error_text);
        TextView successText = dialogView.findViewById(R.id.success_text);

        MainActivity.runOnUI(() -> {
            successText.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
        });

        MaterialButton closeButton = dialogView.findViewById(R.id.close_dialog);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        MaterialButton submitButton = dialogView.findViewById(R.id.submit_ticket);
        submitButton.setOnClickListener(w -> {
            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - DialogManager.submitModalLastSubmitClick < 1000){
                return;
            }
             DialogManager.submitModalLastSubmitClick = SystemClock.elapsedRealtime();

            MainActivity.runOnUI(() -> successText.setVisibility(View.GONE));

            String subject = ((EditText) dialogView.findViewById(R.id.submit_ticket_subject)).getText().toString();
            String email = ((EditText) dialogView.findViewById(R.id.submit_ticket_email)).getText().toString();
            String content = ((EditText) dialogView.findViewById(R.id.submit_ticket_content)).getText().toString().replace("\n", "\\n");

            if (subject.isEmpty() || email.isEmpty() || content.isEmpty()) {
                MainActivity.runOnUI(() -> {
                    errorText.setText("All fields must be filled out to submit a ticket");
                    errorText.setVisibility(View.VISIBLE);
                    successText.setVisibility(View.GONE);
                });
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                MainActivity.runOnUI(() -> {
                    errorText.setText("Email address is not a valid email address. Please check that you have entered it correctly and try again.");
                    errorText.setVisibility(View.VISIBLE);
                    successText.setVisibility(View.GONE);
                });
                return;
            }

            MainActivity.runOnUI(() -> {
                successText.setText("Submitting, please wait...");
                errorText.setVisibility(View.GONE);
                successText.setVisibility(View.VISIBLE);
            });

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            OkHttpClient client = new OkHttpClient().newBuilder().callTimeout(20, TimeUnit.SECONDS).build();
            String bodyText = "{\n" +
                    "    \"subject\": \"" + subject + " - " + ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getLabLocation().getValue() + "\",\n" +
                    "    \"email\": \"" + email + "\",\n" +
                    "    \"content\": \"" + content + "\"\n" +
                    "}";
            Thread thread = new Thread(() -> {
                RequestBody body = RequestBody.create(bodyText, JSON);
                Request request = new Request.Builder()
                        .url("https://us-central1-leadme-labs.cloudfunctions.net/submitTicket")
                        .post(body)
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        MainActivity.runOnUI(() -> {
                            successText.setText("Ticket successfully submitted. This dialog will close in two seconds.");
                            successText.setVisibility(View.VISIBLE);
                        });
                        NetworkService.sendMessage("Station,All", "CommandLine", "UploadLogFile");

                        Properties segmentProperties = new Properties();
                        segmentProperties.put("classification", HelpPageFragment.segmentClassification);
                        Segment.trackEvent(SegmentConstants.Ticket_Submitted, segmentProperties);

                        HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                            put("content_type", "submit");
                            put("content_id", "submit_ticket");
                        }};
                        FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
                        Thread.sleep(2000);
                        MainActivity.runOnUI(dialog::dismiss);
                    } else {
                        MainActivity.runOnUI(() -> {
                            successText.setVisibility(View.GONE);
                            errorText.setText("Something went wrong, please try again or visit https://lumination.com.au/help-support/ to lodge a ticket.");
                            errorText.setVisibility(View.VISIBLE);
                        });

                        Properties segmentProperties = new Properties();
                        segmentProperties.put("classification", HelpPageFragment.segmentClassification);
                        Segment.trackEvent(SegmentConstants.Submit_Ticket_Failed, segmentProperties);
                    }

                } catch (IOException e) {
                    Sentry.captureException(e);
                    e.printStackTrace();
                    MainActivity.runOnUI(() -> {
                        successText.setVisibility(View.GONE);
                        errorText.setText("Something went wrong, please try again or visit https://lumination.com.au/help-support/ to lodge a ticket.");
                        errorText.setVisibility(View.VISIBLE);
                    });

                    Properties segmentProperties = new Properties();
                    segmentProperties.put("classification", HelpPageFragment.segmentClassification);
                    Segment.trackEvent(SegmentConstants.Submit_Ticket_Failed, segmentProperties);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Sentry.captureException(e);
                    MainActivity.runOnUI(() -> {
                        successText.setVisibility(View.GONE);
                        errorText.setText("Something went wrong, please try again or visit https://lumination.com.au/help-support/ to lodge a ticket.");
                        errorText.setVisibility(View.VISIBLE);
                    });

                    Properties segmentProperties = new Properties();
                    segmentProperties.put("classification", HelpPageFragment.segmentClassification);
                    Segment.trackEvent(SegmentConstants.Submit_Ticket_Failed, segmentProperties);
                }
            });
            thread.start();
        });

        dialog.setCancelable(false);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(1000, 760);
        }
    }

    /**
     * Create a basic dialog box that displays the lack of encryption key. This is a separate function
     * so that we can stop it from stacking up by monitoring if it is open.
     */
    public static void createUpdateDetailsDialog() {
        View dialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_update_details, null);
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(dialogView).create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(1000, 850);
        }
        trackDialogShown("Update Details");
    }

    /**
     * Create a basic dialog box that displays the lack of encryption key. This is a separate function
     * so that we can stop it from stacking up by monitoring if it is open.
     */
    public static void createTroubleshootingTextDialog(String titleText, String bodyText) {
        View dialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_troubleshooting_text_only, null);
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(dialogView).create();

        TextView title = dialogView.findViewById(R.id.title);
        title.setText(titleText);

        TextView body = dialogView.findViewById(R.id.body_text);
        body.setText(bodyText);

        TextView submitText = dialogView.findViewById(R.id.submit_text);
        TextView preSubmitText = dialogView.findViewById(R.id.pre_submit_text);

        Thread thread = new Thread(() -> {
            boolean isOnline = Helpers.urlIsAvailable("https://us-central1-leadme-labs.cloudfunctions.net/status");
            MainActivity.runOnUI(() -> {
                if (isOnline) {
                    preSubmitText.setText("If the issue persists, please ");
                    SpannableString content = new SpannableString("submit a support ticket.");
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    submitText.setText(content);
                    submitText.setOnClickListener(w -> {
                        dialog.dismiss();
                        createSubmitTicketDialog();
                        Properties segmentProperties = new Properties();
                        segmentProperties.put("classification", HelpPageFragment.segmentClassification);
                        segmentProperties.put("name", "Troubleshooting Footer Text");
                        Segment.trackEvent(SegmentConstants.Submit_Ticket_Opened, segmentProperties);
                    });
                    submitText.setVisibility(View.VISIBLE);
                } else {
                    preSubmitText.setText("If the issue persists, please submit a support ticket at help.lumination.com.au");
                    submitText.setVisibility(View.GONE);
                }
            });
        });
        thread.start();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(800, 600);
        }
    }

    /**
     * Create a basic dialog box that displays the lack of encryption key. This is a separate function
     * so that we can stop it from stacking up by monitoring if it is open.
     */
    public static void createTextDialog(String titleText, String bodyText) {
        View dialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_troubleshooting_text_only, null);
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(dialogView).create();

        TextView title = dialogView.findViewById(R.id.title);
        title.setText(titleText);

        TextView body = dialogView.findViewById(R.id.body_text);
        body.setText(bodyText);

        FlexboxLayout submitText = dialogView.findViewById(R.id.support_text);
        submitText.setVisibility(View.GONE);

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(800, 600);
        }
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
     * passed in. The dialog is tracked against the openDialogs so it can be closed for other parts
     * of the application.
     * @param type A string representing what the dialog relates to.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     */
    public static void createBasicTrackedDialog(String type, String titleText, String contentText) {
        View basicDialogView = View.inflate(MainActivity.getInstance(), R.layout.alert_dialog_basic_vern, null);
        AlertDialog basicDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(basicDialogView).create();

        trackOpenDialog(type, titleText, basicDialog);

        TextView title = basicDialogView.findViewById(R.id.title);
        title.setText(titleText);

        TextView contentView = basicDialogView.findViewById(R.id.content_text);
        contentView.setText(contentText);

        Button cancelButton = basicDialogView.findViewById(R.id.close_dialog);
        cancelButton.setOnClickListener(w -> {
            basicDialog.dismiss();
            closeOpenDialog(type, titleText);
            trackDialogDismissed(titleText);
        });

        basicDialog.show();
        if (basicDialog.getWindow() != null) {
            basicDialog.getWindow().setLayout(680, 680);
        }
        trackDialogShown(titleText);
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
            trackDialogDismissed(titleText);
        });

        basicDialog.show();
        if (basicDialog.getWindow() != null) {
            basicDialog.getWindow().setLayout(680, 680);
        }
        trackDialogShown(titleText);
    }

    /**
     * Create a dialog box associated with a tablet update with a custom title and content based on
     * the strings that are passed in.
     */
    public static void createUpdateDialog(String titleText, String contentText) {
        View basicDialogView = View.inflate(MainActivity.getInstance(), R.layout.alert_dialog_update_vern, null);
        AlertDialog basicDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(basicDialogView).create();

        TextView title = basicDialogView.findViewById(R.id.title);
        title.setText(titleText);

        TextView contentView = basicDialogView.findViewById(R.id.content_text);
        contentView.setText(contentText);

        Button cancelButton = basicDialogView.findViewById(R.id.close_dialog);
        cancelButton.setOnClickListener(w -> {
            basicDialog.dismiss();
            trackDialogDismissed("Update");
        });

        basicDialog.show();
        if (basicDialog.getWindow() != null) {
            basicDialog.getWindow().setLayout(680, 680);
        }
        trackDialogShown("Update");
    }

    /**
     * Create a basic dialog box with a custom title and content based on the strings that are
     * passed in.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     */
    public static void createConfirmationDialog(String titleText, String contentText) {
        BooleanCallbackInterface booleanCallbackInterface = result -> { };
        createConfirmationDialog(titleText, contentText, booleanCallbackInterface, "Cancel", "Confirm", false);
    }

    /**
     * Create a basic dialog box with a custom title and content based on the strings that are
     * passed in.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     * @param booleanCallbackInterface A callback to be called on cancel or confirm. Will call the callback with true on confirm and false on cancel
     */
    public static void createConfirmationDialog(String titleText, String contentText, BooleanCallbackInterface booleanCallbackInterface) {
        createConfirmationDialog(titleText, contentText, booleanCallbackInterface, "Cancel", "Confirm", false);
    }

    /**
     * Create a basic dialog box with a custom title and content based on the strings that are
     * passed in.
     * @param titleText A string representing what the title shown to the user will be.
     * @param contentText A string representing what content is described within the dialog box.
     * @param booleanCallbackInterface A callback to be called on cancel or confirm. Will call the callback with true on confirm and false on cancel
     */
    public static void createConfirmationDialog(String titleText, String contentText, BooleanCallbackInterface booleanCallbackInterface, String cancelButtonText, String confirmButtonText, boolean cancelable) {
        View confirmationDialogView = View.inflate(MainActivity.getInstance(), R.layout.alert_dialog_warning_vern, null);
        AlertDialog confirmationDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(confirmationDialogView).create();

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
            trackDialogDismissed(titleText);
        });
        confirmButton.setText(confirmButtonText);

        Button cancelButton = confirmationDialogView.findViewById(R.id.close_dialog);
        cancelButton.setOnClickListener(w -> {
            booleanCallbackInterface.callback(false);
            confirmationDialog.dismiss();
            trackDialogDismissed(titleText);
        });
        cancelButton.setText(cancelButtonText);

        confirmationDialog.setCancelable(cancelable);
        confirmationDialog.setCanceledOnTouchOutside(cancelable);
        confirmationDialog.show();
        if (confirmationDialog.getWindow() != null) {
            confirmationDialog.getWindow().setLayout(680, 680);
        }
        trackDialogShown(titleText);
    }

    public static void createEndSessionDialog(ArrayList<Station> stations) {
        View view = View.inflate(MainActivity.getInstance(), R.layout.dialog_select_stations, null);
        AlertDialog endSessionDialog = new AlertDialog.Builder(MainActivity.getInstance()).setView(view).create();

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
            if(SettingsFragment.checkAdditionalExitPrompts()) {
                BooleanCallbackInterface confirmAppExitCallback = confirmationResult -> {
                    if (confirmationResult) {
                        endSession(stationAdapter.stationList);
                        Properties segmentProperties = new Properties();
                        segmentProperties.put("classification", DashboardFragment.segmentClassification);
                        Segment.trackEvent(SegmentConstants.End_Session_On_Select, segmentProperties);
                    }
                };

                DialogManager.createConfirmationDialog(
                        "Confirm experience exit",
                        "Are you sure you want to exit? Some users may require saving their progress. Please confirm this action.",
                        confirmAppExitCallback,
                        "Cancel",
                        "Confirm",
                        false);
            } else {
                endSession(stationAdapter.stationList);
                Properties segmentProperties = new Properties();
                segmentProperties.put("classification", DashboardFragment.segmentClassification);
                Segment.trackEvent(SegmentConstants.End_Session_On_Select, segmentProperties);
            }

            endSessionDialog.dismiss();
            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "session_management");
                put("content_id", "end_session_dialog_submit");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
        });

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(w -> endSessionDialog.dismiss());

        endSessionDialog.show();
    }

    /**
     * End the current session on the stations supplied in the station list.
     * @param stationList An Arraylist of Station objects.
     */
    private static void endSession(ArrayList<Station> stationList) {
        int[] selectedIds = Helpers.cloneStationList(stationList).stream().filter(station -> station.selected).mapToInt(station -> station.id).toArray();
        String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

        NetworkService.sendMessage("Station," + stationIds, "CommandLine", "StopGame");
    }

    /**
     * Build and show the URL dialog box. The input from the edit text field is send to the stations
     * and launched on the default web browser.
     */
    public static void buildURLDialog(Context context, FragmentStationSingleBinding binding) {
        View view = View.inflate(context, R.layout.dialog_enter_url, null);
        AlertDialog urlDialog = new AlertDialog.Builder(context).setView(view).create();

        EditText url = view.findViewById(R.id.url_input);
        url.requestFocus();
        TextView errorText = view.findViewById(R.id.error_text);

        Button submit = view.findViewById(R.id.submit_button);
        submit.setOnClickListener(v -> {
            errorText.setVisibility(View.GONE);
            String input = url.getText().toString();

            if (Patterns.WEB_URL.matcher(input).matches()) {
                Station selectedStation = binding.getSelectedStation();
                selectedStation.applicationController.setExperienceName(input);
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
        AlertDialog renameStationDialog = new AlertDialog.Builder(context).setView(view).create();

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
                errorText.setText(R.string.station_name_warning);
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
        buildShutdownOrRestartDialog(context, "Shutdown", stationIds, countdownCallbackInterface);
    }

    /**
     * Build and launch the shutdown dialog box for stations. Each time this is build a timer is
     * started to allow the users to cancel the shutdown operation.
     */
    public static void buildShutdownOrRestartDialog(Context context, String type, int[] stationIds, CountdownCallbackInterface countdownCallbackInterface) {
        View view = View.inflate(context, R.layout.dialog_template, null);
        AlertDialog confirmDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme).setView(view).create();
        confirmDialog.setCancelable(false);
        confirmDialog.setCanceledOnTouchOutside(false);

        TextView title = view.findViewById(R.id.title);
        TextView contentText = view.findViewById(R.id.content_text);

        int titleText = type.equals("Shutdown") ? R.string.shutting_down : R.string.restarting;
        title.setText(titleText);

        int contextText = type.equals("Shutdown") ? R.string.cancel_shutdown : R.string.cancel_restart;
        contentText.setText(contextText);

        String stationIdsString = String.join(", ", Arrays.stream(stationIds).mapToObj(String::valueOf).toArray(String[]::new));

        NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", type);

        Button confirmButton = view.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> {
            confirmDialog.dismiss();
            trackDialogDismissed(type.equals("Shutdown") ? "Shutdown" : "Restart");
        });
        confirmButton.setText(R.string._continue);

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setText(R.string.cancel_10);

        confirmDialog.show();
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setLayout(1200, 380);
        }
        trackDialogShown(type.equals("Shutdown") ? "Shutdown" : "Restart");

        shutdownTimer = new CountDownTimer(9000, 1000) {
            @Override
            public void onTick(long l) {
                cancelButton.setText(MessageFormat.format("Cancel ({0})", (l + 1000) / 1000));
                if(countdownCallbackInterface != null) {
                    countdownCallbackInterface.callback((int) (l + 1000) / 1000);
                }
            }

            @Override
            public void onFinish() {
                confirmDialog.dismiss();
                DashboardFragment.getInstance().dashboardModeManagement.changeModeButtonAvailability(
                        "",
                        type.equals("Shutdown") ? Constants.SHUTDOWN_MODE : Constants.RESTART_MODE
                );

                //Start a power check for all stations
                if (type.equals("Restart")) {
                    WakeById("Restarting", stationIds); //Wake any computers that are already off
                }
                if(countdownCallbackInterface != null) {
                    countdownCallbackInterface.callback(0);
                }
            }
        }.start();

        cancelButton.setOnClickListener(x -> {
            NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", "CancelShutdown");
            if(countdownCallbackInterface != null) {
                countdownCallbackInterface.callback(0);
            }
            shutdownTimer.cancel();
            confirmDialog.dismiss();

            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("station_ids", stationIdsString);
            }};
            FirebaseManager.logAnalyticEvent("shutdown_cancelled", analyticsAttributes);
            trackDialogDismissed(type.equals("Shutdown") ? "Shutdown" : "Restart");
        });
    }

    /**
     * Build the set NUC dialog. A user is able to input the MAC address manually.
     */
    public static void buildNucDetailsDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_nuc_details, null);
        AlertDialog nucDetailsDialog = new AlertDialog.Builder(context).setView(view).create();

        SettingsFragment.mViewModel.getNucAddress().observe(SettingsFragment.getInstance().getViewLifecycleOwner(), nucAddress -> {
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
            trackSettingChanged("NUC MAC Address");
        });

        Button wakeNuc = view.findViewById(R.id.wake_nuc_button);
        wakeNuc.setOnClickListener(v -> {
            if(SettingsFragment.mViewModel.getNucMac().getValue() != null) {
                WakeOnLan.WakeNUCOnLan();
                Properties segmentProperties = new Properties();
                segmentProperties.put("classification", SettingsFragment.segmentClassification);
                Segment.trackEvent(SegmentConstants.NUC_WOL, segmentProperties);
            } else {
                Toast.makeText(context, "NUC MAC address needs to be set.", Toast.LENGTH_LONG).show();
            }
        });

        nucDetailsDialog.show();
    }

    /**
     * Build the set NUC dialog. A user is able to input the IP address manually.
     */
    public static void buildSetNucDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_set_nuc, null);
        AlertDialog nucDialog = new AlertDialog.Builder(context).setView(view).create();

        SettingsFragment.mViewModel.getNucAddress().observe(SettingsFragment.getInstance().getViewLifecycleOwner(), nucAddress -> {
            TextView textView = view.findViewById(R.id.nuc_address);
            textView.setText(nucAddress);
        });

        EditText newAddress = view.findViewById(R.id.nuc_address_input);
        Button setAddress = view.findViewById(R.id.set_nuc_button);
        setAddress.setOnClickListener(v -> {
            if (!newAddress.getText().toString().trim().isEmpty()) {
                SettingsFragment.mViewModel.setNucAddress(newAddress.getText().toString().trim());
                nucDialog.dismiss();
                trackSettingChanged("NUC IP Address");
            } else {
                Toast.makeText(context, "NUC address cannot be empty.", Toast.LENGTH_LONG).show();
            }
        });

        Button refreshAddress = view.findViewById(R.id.refresh_nuc_button);
        refreshAddress.setOnClickListener(v -> {
            if(NetworkService.getNUCAddress() != null) {
                NetworkService.refreshNUCAddress();
                nucDialog.dismiss();
                Properties segmentProperties = new Properties();
                segmentProperties.put("classification", SettingsFragment.segmentClassification);
                Segment.trackEvent(SegmentConstants.NUC_Refreshed, segmentProperties);
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
            trackSettingChanged("Pin Code");
        });

        pinDialog.setCancelable(false);
        pinDialog.show();
    }

    public static void confirmPinCode(SideMenuFragment sideMenuFragment, String navigationType) {
        pinCodeAttempts = 0;
        View view = View.inflate(sideMenuFragment.getContext(), R.layout.dialog_pin, null);
        AlertDialog pinDialog = new AlertDialog.Builder(sideMenuFragment.getContext()).setView(view).create();

        pinDialog.setCancelable(false);
        pinDialog.show();
        trackDialogShown("Enter PIN Code");
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
                    trackDialogDismissed("Enter PIN Code");
                } else {
                    errorMessage.setVisibility(View.VISIBLE);
                }
            }
            else {
                sideMenuFragment.navigateToSettingsPage(navigationType);
                pinDialog.dismiss();
                trackDialogDismissed("Enter PIN Code");
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
            SettingsFragment.mViewModel.setEncryptionKey(newKey.getText().toString().trim());
            encryptionDialog.dismiss();
            trackSettingChanged("Encryption Key");
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
            trackSettingChanged("Lab Location");
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
            trackSettingChanged("License Key");
        });

        licenseDialog.show();
    }

    /**
     * Build a custom WebView dialog. The view will load the supplied URL.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void buildWebViewDialog(Context context, String URL) {
        View webViewDialogView = View.inflate(context, R.layout.dialog_webview, null);
        Dialog webViewDialog = new AlertDialog.Builder(context).setView(webViewDialogView).create();

        Button closeButton = webViewDialogView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(w -> webViewDialog.dismiss());

        WebView webView = webViewDialogView.findViewById(R.id.dialog_webview);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(URL);
        webView.getSettings().setJavaScriptEnabled(true);

        webViewDialog.show();
        if (webViewDialog.getWindow() != null) {
            webViewDialog.getWindow().setLayout(1200, 900);
        }
    }

    /**
     * Build the set locked room dialog. Users can set what room a tablet is locked to, once locked
     * the tablet will only display, interact and receive information about that particular room.
     */
    public static void buildLockedRoomDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_set_locked_room, null);
        AlertDialog lockedRoomDialog = new AlertDialog.Builder(context).setView(view).create();

        //Get the currently selected value for the locked room or 'None' if nothing has been selected
        TextView preview = view.findViewById(R.id.locked_room_preview);
        preview.setText(
                SettingsFragment.mViewModel.getLockedRooms().getValue() == null ||
                        SettingsFragment.mViewModel.getLockedRooms().getValue().isEmpty() ?
                        "None" :
                        String.join(", ", SettingsFragment.mViewModel.getLockedRooms().getValue()));

        HashSet<String> rooms = RoomFragment.mViewModel.getAllRooms().getValue();

        RecyclerView roomRecyclerView = view.findViewById(R.id.room_list);
        TextView roomStatus = view.findViewById(R.id.room_status_prompt);

        //Show the empty room list prompt or set the rooms the recycler view
        if((rooms != null ? rooms.size() : 0) == 0) {
            roomStatus.setText(R.string.no_rooms);
            roomStatus.setVisibility(View.VISIBLE);
            roomRecyclerView.setVisibility(View.GONE);
        }
        else if (rooms.size() == 1) {
            roomStatus.setText(R.string.one_room);
            roomStatus.setVisibility(View.VISIBLE);
            roomRecyclerView.setVisibility(View.GONE);
        } else {
            roomStatus.setVisibility(View.GONE);
            int numberOfColumns = 1;
            roomRecyclerView.setLayoutManager(new GridLayoutManager(MainActivity.getInstance().getApplicationContext(), numberOfColumns));
            RoomAdapter roomAdapter = new RoomAdapter(preview, rooms);
            roomRecyclerView.setAdapter(roomAdapter);
        }

        Button roomConfirmButton = view.findViewById(R.id.room_lock_confirm_button);
        roomConfirmButton.setOnClickListener(v -> {
            SettingsFragment.mViewModel.setLockedRooms(preview.getText().toString());
            lockedRoomDialog.dismiss();
            trackDialogDismissed("Select Locked Rooms");
        });

        lockedRoomDialog.show();
        trackDialogShown("Select Locked Rooms");
    }

    /**
     * Build and display the reconnection dialog. Can either be dismissed through the close button
     * or a user can select reconnect, sending a UDP broadcast out from the network service looking
     * for active NUCs.
     */
    public static void buildReconnectDialog() {
        if (!MainActivity.isActivityRunning) return; //The activity has been destroyed

        View reconnectDialogView = View.inflate(MainActivity.getInstance(), R.layout.alert_dialog_lost_server, null);
        if (reconnectDialog == null) {
            reconnectDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(reconnectDialogView).create();
            reconnectDialog.setCancelable(false);
            reconnectDialog.setCanceledOnTouchOutside(false);
        }

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

        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", "Reconnect");
        Segment.trackEvent(SegmentConstants.Reconnect_Dialog_Shown, segmentProperties);

        reconnectButton.setOnClickListener(w -> {
            reconnectButton.setVisibility(View.GONE);
            reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);

            if(NetworkService.getNUCAddress() != null) {
                NetworkService.refreshNUCAddress();
            }

            new Timer().schedule( // turn animations back on after the scenes have updated
                    new TimerTask() {
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
            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "reconnect_dialog");
                put("content_id", "reconnect_button");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
            Properties segmentPropertiesReconnect = new Properties();
            segmentPropertiesReconnect.put("classification", "Reconnect");
            Segment.trackEvent(SegmentConstants.Reconnect_Dialog_Reconnect, segmentPropertiesReconnect);
        });

        Button ignoreReconnectDialogButton = reconnectDialogView.findViewById(R.id.ignore_dialog);
        ignoreReconnectDialogButton.setOnClickListener(w -> {
            content.setText(R.string.lost_server_message_content);
            reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
            new Handler().postDelayed(() -> reconnectDialog.dismiss(), 200);
            MainActivity.reconnectionIgnored = true;
            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "reconnect_dialog");
                put("content_id", "ignore_button");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
            Properties segmentPropertiesIgnore = new Properties();
            segmentPropertiesIgnore.put("classification", "Reconnect");
            Segment.trackEvent(SegmentConstants.Reconnect_Dialog_Ignore, segmentPropertiesIgnore);
        });

        Button closeReconnectDialogButton = reconnectDialogView.findViewById(R.id.close_dialog);
        closeReconnectDialogButton.setOnClickListener(w -> {
            content.setText(R.string.lost_server_message_content);
            reconnectDialogView.findViewById(R.id.reconnect_loader).setVisibility(View.GONE);
            new Handler().postDelayed(() -> reconnectDialog.dismiss(), 200);
            MainActivity.hasNotReceivedPing = 0;
            MainActivity.attemptedRefresh = false;
            HashMap<String, String> analyticsAttributes = new HashMap<String, String>() {{
                put("content_type", "reconnect_dialog");
                put("content_id", "close_button");
            }};
            FirebaseManager.logAnalyticEvent("select_content", analyticsAttributes);
            Properties segmentPropertiesDismiss = new Properties();
            segmentPropertiesDismiss.put("classification", "Reconnect");
            Segment.trackEvent(SegmentConstants.Reconnect_Dialog_Dismiss, segmentPropertiesDismiss);
        });

        reconnectDialog.setOnDismissListener(v -> MainActivity.startNucPingMonitor());

        reconnectDialog.show();
        if (reconnectDialog.getWindow() != null) {
            reconnectDialog.getWindow().setLayout(680, 720);
        }
    }

    /**
     * Displays a dialog to inform users that a game is currently launching on a number of stations.
     * This is dismisses manually by the user or automatically when the NUC sends back confirmation
     * from the stations.
     */
    public static void awaitStationApplicationLaunch(int[] stationIds, String gameName, boolean restarting)
    {
        View gameLaunchDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_template, null);
        gameLaunchDialog = new AlertDialog.Builder(MainActivity.getInstance()).setView(gameLaunchDialogView).create();

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
        confirmButton.setOnClickListener(w -> {
            gameLaunchDialog.dismiss();
            trackDialogDismissed("Launching Experience");
        });

        Button cancelButton = gameLaunchDialogView.findViewById(R.id.cancel_button);
        cancelButton.setVisibility(View.GONE);

        gameLaunchDialog.show();
        if (gameLaunchDialog.getWindow() != null) {
            gameLaunchDialog.getWindow().setLayout(1200, 380);
        }
        trackDialogShown("Launching Experience");
    }

    /**
     * A game has launched on all selected stations so the game launch dialog is dismissed.
     */
    public static void gameLaunchedOnStation(int stationId) {
        if (gameLaunchStationIds != null) {
            gameLaunchStationIds.removeIf(id -> id == stationId);
            if (gameLaunchStationIds.isEmpty()) {
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
        if (endSessionDialog.getWindow() != null) {
            endSessionDialog.getWindow().setLayout(1200, 380);
        }
    }

    public static void sessionEndedOnStation(int stationId) {
        if (endSessionStationIds != null) {
            endSessionStationIds.removeIf(id -> id == stationId);
            if (endSessionStationIds.isEmpty()) {
                if (endSessionDialog != null) {
                    endSessionDialog.dismiss();
                }
            }
        }
    }

    public static void awaitStationRestartVRSystem(int[] stationIds)
    {
        View restartSessionDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_template, null);
        restartVRSystemDialog = new AlertDialog.Builder(MainActivity.getInstance()).setView(restartSessionDialogView).create();

        TextView title = restartSessionDialogView.findViewById(R.id.title);
        title.setText(R.string.restarting_system);

        TextView contentText = restartSessionDialogView.findViewById(R.id.content_text);
        contentText.setText(MessageFormat.format("Restarting system on {0}", String.join(", ", StationsFragment.mViewModel.getStationNames(stationIds))));

        restartVRSystemStationIds =  new ArrayList<>(stationIds.length);
        for (int i : stationIds)
        {
            restartVRSystemStationIds.add(i);
        }

        Button confirmButton = restartSessionDialogView.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> {
            restartVRSystemDialog.dismiss();
            trackDialogDismissed("Await Restart VR System");
        });
        confirmButton.setText(R.string.dismiss);

        Button cancelButton = restartSessionDialogView.findViewById(R.id.cancel_button);
        cancelButton.setVisibility(View.GONE);

        restartVRSystemDialog.show();
        if (restartVRSystemDialog.getWindow() != null) {
            restartVRSystemDialog.getWindow().setLayout(1200, 380);
        }
        trackDialogShown("Await Restart VR System");
    }

    /**
     * Dismiss the restart vr system loading screen as it has successfully restarted the applications
     * or an error has occurred and another popup is about to be shown.
     * @param stationId An integer of the ID of a station which has relaunched/encountered an error.
     */
    public static void vrSystemRestartedOnStation(int stationId) {
        if (restartVRSystemStationIds != null) {
            restartVRSystemStationIds.removeIf(id -> id == stationId);
            if (restartVRSystemStationIds.isEmpty()) {
                if (restartVRSystemDialog != null) {
                    restartVRSystemDialog.dismiss();
                }
            }
        }
    }

    /**
     * A popup that allows the entry of a steam guard key.
     * @param stationId An integer of the ID of a station which is to be configured.
     */
    public static void steamGuardKeyEntry(int stationId) {
        View view = View.inflate(MainActivity.getInstance(), R.layout.dialog_steam_guard_input, null);
        steamGuardEntryDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.AlertDialogVernTheme).setView(view).create();

        //Reset in case of pre-emptive closure
        FlexboxLayout entry = view.findViewById(R.id.steam_guard_entry);
        entry.setVisibility(View.VISIBLE);
        ProgressBar waiting = view.findViewById(R.id.waiting_for_result);
        waiting.setVisibility(View.GONE);

        final String[] steamGuard = {""};

        Button confirmButton = view.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(w -> {
            NetworkService.sendMessage("Station," + stationId, "Station", "SetValue:steamCMD:" + steamGuard[0]);

            //Present a loading bar until the output comes back
            entry.setVisibility(View.GONE);
            waiting.setVisibility(View.VISIBLE);
        });

        //Track as the code is input into the pin entry area
        PinEntryEditText text = view.findViewById(R.id.steam_guard_key);
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                steamGuard[0] = s.toString();

                if(steamGuard[0].length() == 5) {
                    confirmButton.setEnabled(true);
                    confirmButton.setBackgroundTintList(ColorStateList.valueOf(MainActivity.getInstance().getColor(R.color.blue)));
                } else {
                    confirmButton.setEnabled(false);
                    confirmButton.setBackgroundTintList(ColorStateList.valueOf(MainActivity.getInstance().getColor(R.color.grey)));
                }
            }
        });

        Button cancelButton = view.findViewById(R.id.close_dialog);
        cancelButton.setOnClickListener(w -> {
            steamGuardEntryDialog.dismiss();
            trackDialogDismissed("Steam Guard Entry");
        });

        steamGuardEntryDialog.setCancelable(false);
        steamGuardEntryDialog.show();
        if (steamGuardEntryDialog.getWindow() != null) {
            steamGuardEntryDialog.getWindow().setLayout(680, 680);
        }
        trackDialogShown("Steam Guard Entry");
    }

    /**
     * Display a list of buttons that represent the different options for the currently loaded
     * experience.
     */
    public static void showExperienceOptions(String gameName, Details details) {
        View basicDialogView = View.inflate(MainActivity.getInstance(), R.layout.dialog_experience_options, null);
        AlertDialog basicDialog = new AlertDialog.Builder(MainActivity.getInstance(), R.style.ExperienceDetailsDialogTheme).setView(basicDialogView).create();

        TextView title = basicDialogView.findViewById(R.id.title);
        title.setText(gameName);

        //Set the global actions
        RecyclerView globalRecyclerView = basicDialogView.findViewById(R.id.global_action_list);
        globalRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.getInstance().getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        GlobalAdapter globalAdapter = new GlobalAdapter(details.getGlobalActions());
        globalRecyclerView.setAdapter(globalAdapter);

        int numberOfColumns = 1;

        //Set the levels and their actions
        RecyclerView levelRecyclerView = basicDialogView.findViewById(R.id.level_list);
        levelRecyclerView.setLayoutManager(new GridLayoutManager(MainActivity.getInstance().getApplicationContext(), numberOfColumns));
        LevelAdapter levelAdapter = new LevelAdapter(details.getLevels());
        levelRecyclerView.setAdapter(levelAdapter);

        Button cancelButton = basicDialogView.findViewById(R.id.close_dialog);
        cancelButton.setOnClickListener(w -> basicDialog.dismiss());

        basicDialog.show();
        if (basicDialog.getWindow() != null) {
            basicDialog.getWindow().setLayout(1100, 875);
        }
    }

    //region Tracking
    private static final String segmentClassification = "Dialog";

    private static void trackSettingChanged(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", SettingsFragment.segmentClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Setting_Changed, segmentProperties);
    }

    private static void trackDialogShown(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Show_Dialog, segmentProperties);
    }

    private static void trackDialogDismissed(String name) {
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("name", name);
        Segment.trackEvent(SegmentConstants.Dismiss_Dialog, segmentProperties);
    }
    //endregion tracking
}
