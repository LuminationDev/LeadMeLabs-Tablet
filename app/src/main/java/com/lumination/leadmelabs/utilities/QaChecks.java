package com.lumination.leadmelabs.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.jobServices.UpdateJobService;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class QaChecks {
    public class QaCheck {
        public String passedStatus = null;
        public String message = null;
        public String id;

        public QaCheck(String id) {
            this.id = id;
        }

        public void setFailed(String message)
        {
            this.passedStatus = "failed";
            this.message = message;
        }

        public void setPassed()
        {
            this.passedStatus = "passed";
        }

        public void setPassed(String message)
        {
            this.setPassed();
            this.message = message;
        }
    }

    private class QaDetail {
        public String value = null;
        public String message = null;
        public String id;

        public QaDetail(String id) {
            this.id = id;
        }

        public QaDetail(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public QaDetail(String id, String value, String message) {
            this.id = id;
            this.value = value;
            this.message = message;
        }
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

    public QaDetail getLabLocation() {
        return new QaDetail("lab_location", SettingsFragment.mViewModel.getLabLocation().getValue());
    }

    public QaDetail getHideStationControls() {
        return new QaDetail("hide_station_controls", String.valueOf(SettingsFragment.mViewModel.getHideStationControls().getValue()));
    }

    public QaDetail getCurrentUnixTime() {
        return new QaDetail("current_unix_time", String.valueOf(System.currentTimeMillis() / 1000L));
    }

    public QaCheck canReachPlayStore() {
        QaCheck qaCheck = new QaCheck("can_reach_play_store");
        boolean result = isAvailable("https://play.google.com/");
        if (!result) {
            qaCheck.setFailed("Could not reach https://play.google.com/");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }

    public QaCheck canReachAnalytics() {
        QaCheck qaCheck = new QaCheck("can_reach_analytics");
        boolean result = isAvailable("https://analytics.google.com/");
        if (!result) {
            qaCheck.setFailed("Could not reach https://analytics.google.com/");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }

    public QaCheck canReachSentry() {
        QaCheck qaCheck = new QaCheck("can_reach_sentry");
        boolean result = isAvailable("https://sentry.io/");
        if (!result) {
            qaCheck.setFailed("Could not reach https://sentry.io/");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }

    public QaCheck canReachSteamStatic() {
        QaCheck qaCheck = new QaCheck("can_reach_steam_static");
        boolean result = isAvailable("https://cdn.cloudflare.steamstatic.com/steam/apps/238010/header.jpg");
        if (!result) {
            qaCheck.setFailed("Could not reach cloudflare.steamstatic.com");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }


    public ArrayList<QaCheck> runQa() {
        ArrayList<QaCheck> qaChecks = new ArrayList<QaCheck>();
        qaChecks.add(isAppUpToDate());
        qaChecks.add(pinIsNotDefault());
        qaChecks.add(canReachPlayStore());
        qaChecks.add(canReachAnalytics());
        qaChecks.add(canReachSentry());
        qaChecks.add(canReachSteamStatic());
        return qaChecks;
    }

    private boolean isAvailable(String url){
        try {
            URL urlObject = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
            urlConnection.connect();
            urlConnection.disconnect();
            return true;
        } catch (IOException e) {
            Log.e("QaChecks", "Exception", e);
        }
        return false;
    }
}
