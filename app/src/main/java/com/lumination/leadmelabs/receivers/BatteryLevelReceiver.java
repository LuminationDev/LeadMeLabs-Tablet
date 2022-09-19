package com.lumination.leadmelabs.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.widget.TextView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;

public class BatteryLevelReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryLevelReceiver";
    private TextView batteryTextView;

    public void setBatteryTextView(TextView batteryTextView)
    {
        this.batteryTextView = batteryTextView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
            DialogManager.createBasicDialog("Low battery", "Warning! 15% battery remaining. Please place tablet on charge after use.");
        }
        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) (level * 100 / (float)scale);
            if (batteryTextView != null) {
                batteryTextView.setText(batteryPct + "");
                if (batteryPct > 60) {
                    batteryTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_battery_level_green, 0, 0);
                }
                if (batteryPct <= 60 && batteryPct > 15) {
                    batteryTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_battery_level_yellow, 0, 0);
                }
                if (batteryPct <= 15) {
                    batteryTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_battery_level_red, 0, 0);
                }
            }
        }
    }
}