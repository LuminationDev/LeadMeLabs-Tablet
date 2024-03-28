package com.lumination.leadmelabs.qa;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.qa.checks.NetworkChecks;
import com.lumination.leadmelabs.qa.checks.SecurityChecks;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.sentry.Sentry;

public class QaManager {
    /**
     * Handles the update for QA action namespace.
     * This method processes updates related to quality assurance tasks.
     *
     * @param additionalData The additional data associated with the update.
     */
    public static void handleQaUpdate(String additionalData) throws JSONException {
        JSONObject request = new JSONObject(additionalData);

        String action = request.getString("action");
        switch (action) {
            case "Connect":
                QaManager.Connect();
                break;

            case "RunGroup":
                QaManager.RunGroup(request.getJSONObject("actionData").getString("group"));
                break;

            default:
                Sentry.captureMessage(
                        ViewModelProviders.of(MainActivity.getInstance()).get(SettingsViewModel.class).getLabLocation()
                                + ": handleQaUpdate, not a valid action - " + action);
        }
    }

    /**
     * A NUC has asked to check the connection for the purpose of a QA check. Send a message back to
     * acknowledge that the connection is valid.
     */
    public static void Connect() throws JSONException {
        JSONObject response = new JSONObject();
        response.put("response", "TabletConnected");
        response.put("responseData", new JSONObject());
        response.put("ipAddress", NetworkService.getIPAddress());
        NetworkService.sendMessage("NUC", "QA", response.toString());
    }

    /**
     * Run a group of QA checks, sending the details back to the NUC.
     * @param group A string of the specific QA group to run.
     */
    public static void RunGroup(String group) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("response", "RunTabletGroup");
        response.put("ipAddress", NetworkService.getIPAddress());
        JSONObject responseData = new JSONObject();
        responseData.put("group", group);
        switch (group) {
            case "network_checks": {
                NetworkChecks qaChecks = new NetworkChecks();
                JSONArray qaCheckList = qaChecks.runNetworkChecks();
                responseData.put("data", qaCheckList.toString());
                break;
            }
            case "security_checks": {
                SecurityChecks qaChecks = new SecurityChecks();
                JSONArray qaCheckList = qaChecks.runSecurityChecks();
                responseData.put("data", qaCheckList.toString());
                break;
            }
        }
        response.put("responseData", responseData);

        NetworkService.sendMessage("NUC", "QA", response.toString());
    }
}
