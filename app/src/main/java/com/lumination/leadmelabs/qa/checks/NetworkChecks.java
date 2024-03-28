package com.lumination.leadmelabs.qa.checks;

import com.lumination.leadmelabs.qa.QaCheck;
import com.lumination.leadmelabs.qa.QaDetail;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.utilities.Helpers;

import org.json.JSONArray;

public class NetworkChecks {
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
        boolean result = Helpers.urlIsAvailable("https://play.google.com/");
        if (!result) {
            qaCheck.setFailed("Could not reach https://play.google.com/");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }

    public QaCheck canReachAnalytics() {
        QaCheck qaCheck = new QaCheck("can_reach_analytics");
        boolean result = Helpers.urlIsAvailable("https://analytics.google.com/");
        if (!result) {
            qaCheck.setFailed("Could not reach https://analytics.google.com/");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }

    public QaCheck canReachSentry() {
        QaCheck qaCheck = new QaCheck("can_reach_sentry");
        boolean result = Helpers.urlIsAvailable("https://sentry.io/");
        if (!result) {
            qaCheck.setFailed("Could not reach https://sentry.io/");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }

    public QaCheck canReachSteamStatic() {
        QaCheck qaCheck = new QaCheck("can_reach_steam_static");
        boolean result = Helpers.urlIsAvailable("https://cdn.cloudflare.steamstatic.com/steam/apps/238010/header.jpg");
        if (!result) {
            qaCheck.setFailed("Could not reach cloudflare.steamstatic.com");
        } else {
            qaCheck.setPassed();
        }
        return qaCheck;
    }

    public JSONArray runNetworkChecks() {
        JSONArray qaChecks = new JSONArray();
        qaChecks.put(canReachPlayStore().toJson());
        qaChecks.put(canReachAnalytics().toJson());
        qaChecks.put(canReachSentry().toJson());
        qaChecks.put(canReachSteamStatic().toJson());
        return qaChecks;
    }
}
