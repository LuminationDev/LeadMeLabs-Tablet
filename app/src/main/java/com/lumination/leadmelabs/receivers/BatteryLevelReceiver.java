package com.lumination.leadmelabs.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.classes.SegmentTabletEvent;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;

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

                int drawableId;
                String eventToTrack = null;

                if (batteryPct > 60) {
                    drawableId = R.drawable.ic_battery_level_green;
                } else if (batteryPct > 15) {
                    drawableId = R.drawable.ic_battery_level_yellow;
                } else if (batteryPct > 1) {
                    drawableId = R.drawable.ic_battery_level_red;
                    eventToTrack = SegmentConstants.Event_Tablet_Low_Battery;
                } else {
                    drawableId = R.drawable.ic_battery_level_red;
                    eventToTrack = SegmentConstants.Event_Tablet_Flat_Battery;
                }

                batteryTextView.setCompoundDrawablesWithIntrinsicBounds(0, drawableId, 0, 0);

                // Track a low battery event (below 15%) or flat battery event (1% left)
                if (eventToTrack != null) {
                    boolean hideStationControls = Boolean.TRUE.equals(ViewModelProviders.of(MainActivity.getInstance())
                            .get(SettingsViewModel.class).getHideStationControls().getValue());
                    SegmentTabletEvent event = new SegmentTabletEvent(eventToTrack, hideStationControls);
                    Segment.trackAction(event);
                }
            }
        }
    }
}
