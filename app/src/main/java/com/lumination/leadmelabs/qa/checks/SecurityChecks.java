package com.lumination.leadmelabs.qa.checks;

import android.content.Context;
import android.content.SharedPreferences;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.qa.QaCheck;
import com.lumination.leadmelabs.services.jobServices.UpdateJobService;

import org.json.JSONArray;

public class SecurityChecks {
    public JSONArray runSecurityChecks() {
        JSONArray qaChecks = new JSONArray();
        qaChecks.put(isAppUpToDate().toJson());
        qaChecks.put(pinIsNotDefault().toJson());
        return qaChecks;
    }

    public QaCheck isAppUpToDate() {
        QaCheck qaCheck = new QaCheck("app_up_to_date");
        boolean result = UpdateJobService.checkForUpdateSync();
        if (result) {
            qaCheck.setFailed("New version available on play store or app may be sideloaded");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }

    public QaCheck pinIsNotDefault() {
        QaCheck qaCheck = new QaCheck("pin_is_not_default");
        SharedPreferences sharedPreferences = MainActivity.getInstance().getSharedPreferences("pin_code", Context.MODE_PRIVATE);
        String pinCode = sharedPreferences.getString("pin_code", null);
        if (pinCode == null || pinCode.equals("")) {
            qaCheck.setFailed("Pin code has not been set");
        } else if (pinCode.equals("1990") || pinCode.equals("1234")) {
            qaCheck.setFailed("Pin code must not be a commonly used code");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }
}
