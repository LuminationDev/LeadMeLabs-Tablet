package com.lumination.leadmelabs.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;

import java.util.Locale;

/**
 * BroadcastReceiver to monitor battery level changes and update UI accordingly.
 * It sets the battery percentage in a TextView and displays a charging icon based on the charging status.
 */
public class BatteryLevelReceiver extends BroadcastReceiver {
    private TextView batteryTextView;
    private ImageView batteryChargingIcon;

    /**
     * Sets the TextView and ImageView to display battery information.
     *
     * @param batteryTextView     The TextView to display battery percentage.
     * @param batteryChargingIcon The ImageView to display charging icon.
     */
    public void setBatteryTextView(TextView batteryTextView, ImageView batteryChargingIcon) {
        this.batteryTextView = batteryTextView;
        this.batteryChargingIcon = batteryChargingIcon;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;

        if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
            DialogManager.createBasicDialog("Low battery", "Warning! 15% battery remaining. Please place tablet on charge after use.");
        }

        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) (level * 100 / (float) scale);

            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;

            if (batteryTextView != null && batteryChargingIcon != null) {
                batteryChargingIcon.setVisibility(isCharging ? View.VISIBLE : View.GONE);
                batteryTextView.setText(isCharging ? "" : String.format(Locale.ENGLISH, "%d", batteryPct));

                if (batteryPct > 60) {
                    batteryTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_battery_level_green, 0, 0);
                } else if (batteryPct > 15) {
                    batteryTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_battery_level_yellow, 0, 0);
                } else {
                    batteryTextView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_battery_level_red, 0, 0);
                }
            }
        }
    }
}