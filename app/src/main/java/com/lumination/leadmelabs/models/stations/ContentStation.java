package com.lumination.leadmelabs.models.stations;

import android.view.View;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;

public class ContentStation extends Station {
    public ContentStation(String name, Object applications, int id, String status, String state, String room, String macAddress) {
        super(name, applications, id, status, state, room, macAddress);
    }

    //region Station State & GameName Binding
    /**
     * Data binding to update the Station content flexbox background.
     */
    @BindingAdapter("stationState")
    public static void setStationStateBackground(FlexboxLayout flexbox, ContentStation selectedStation) {
        if (selectedStation == null) return;

        boolean isStatusOn = selectedStation.status != null && (!selectedStation.status.equals("Off") && !selectedStation.status.equals("Turning On") && !selectedStation.status.equals("Restarting"));
        boolean hasState = selectedStation.state != null && selectedStation.state.length() != 0;
        boolean hasGame = selectedStation.gameName != null && selectedStation.gameName.length() != 0 && !selectedStation.gameName.equals("null");

        //Station is On and has either a State or a Game running
        if(isStatusOn && (hasState || hasGame)) {
            flexbox.setBackgroundResource(R.drawable.card_station_ripple_white);
        } else {
            flexbox.setBackgroundResource(R.drawable.card_station_ripple_empty);
        }
    }

    /**
     * Data binding to update the Station content text (Status or Game name)
     */
    @BindingAdapter("stationState")
    public static void setStationStateTextAndVisibility(TextView textView, ContentStation selectedStation) {
        if (selectedStation == null) return;

        //Set the visibility value
        boolean isStatusOn = selectedStation.status != null && (!selectedStation.status.equals("Off") && !selectedStation.status.equals("Turning On") && !selectedStation.status.equals("Restarting"));
        boolean hasState = selectedStation.state != null && selectedStation.state.length() != 0;
        boolean hasGame = selectedStation.gameName != null && selectedStation.gameName.length() != 0 && !selectedStation.gameName.equals("null");
        int visibility = isStatusOn && (hasState || hasGame) ? View.VISIBLE : View.INVISIBLE;
        textView.setVisibility(visibility);

        //Set the text value ('Not set' - backwards compatibility, default state when it is not sent across)
        if(selectedStation.state != null && (!selectedStation.state.equals("Ready to go") || !hasGame) && !selectedStation.state.equals("Not set")) {
            //Show the state if the state is anything but Ready to go
            textView.setText(selectedStation.state);
        } else {
            textView.setText(selectedStation.gameName);
        }
    }
}
