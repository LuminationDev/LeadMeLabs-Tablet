package com.lumination.leadmelabs.ui.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.BooleanCallbackInterface;
import com.lumination.leadmelabs.CountdownCallbackInterface;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.SteamSelectionFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.utilities.Identifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardPageFragment extends Fragment {
    public static FragmentManager childManager;
    private static boolean cancelledShutdown = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page_dashboard, container, false);
        childManager = getChildFragmentManager();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            loadFragments();
        }

        int currentDate = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String message = "Welcome!";
        if (currentDate >= 0 && currentDate < 12) {
            message = "Good Morning!";
        } else if (currentDate >= 12 && currentDate < 17) {
            message = "Good Afternoon!";
        } else if (currentDate >= 17 && currentDate <= 23) {
            message = "Good Evening!";
        }
        TextView welcomeMessage = view.findViewById(R.id.welcome_message);
        welcomeMessage.setText(message);

        LocalDate now = LocalDate.now();
        String dateMessage = "";
        String dayName = now.getDayOfWeek().name();
        dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase(Locale.ROOT);
        dateMessage += (dayName + " ");
        dateMessage += (now.getDayOfMonth() + getDayOfMonthSuffix(now.getDayOfMonth()) +" ");
        String monthName = now.getMonth().name();
        monthName = monthName.charAt(0) + monthName.substring(1).toLowerCase(Locale.ROOT);
        dateMessage += (monthName + " ");
        dateMessage += (now.getYear() + " ");
        TextView dateMessageView = view.findViewById(R.id.date_message);
        dateMessageView.setText(dateMessage);

        FlexboxLayout newSession = view.findViewById(R.id.new_session_button);
        newSession.setOnClickListener(v -> {
            SideMenuFragment.loadFragment(SteamSelectionFragment.class, "session");
            SteamSelectionFragment.setStationId(0);
        });

        FlexboxLayout endSession = view.findViewById(R.id.end_session_button);
        endSession.setOnClickListener(v -> {
            BooleanCallbackInterface selectStationsCallback = new BooleanCallbackInterface() {
                @Override
                public void callback(boolean confirmationResult) {
                    if (confirmationResult) {
                        int[] selectedIds = StationsFragment.getInstance().getRoomStations().stream().mapToInt(station -> station.id).toArray();
                        String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

                        NetworkService.sendMessage("Station," + stationIds, "CommandLine", "StopGame");
                    } else {
                        ArrayList<Station> stationsToSelectFrom = (ArrayList<Station>) StationsFragment.getInstance().getRoomStations().clone();
                        DialogManager.createEndSessionDialog(stationsToSelectFrom);
                    }
                }
            };
            DialogManager.createConfirmationDialog("End session on all stations?", "This will stop any running experiences", selectStationsCallback, "End on select", "End on all");
        });

        FlexboxLayout identify = view.findViewById(R.id.identify_button);
        identify.setOnClickListener(v -> {
            List<Station> stations = StationsFragment.getInstance().getRoomStations();
            Identifier.identifyStations(stations);
        });

        FlexboxLayout shutdown = view.findViewById(R.id.shutdown_button);
        TextView shutdownHeading = view.findViewById(R.id.shutdown_heading);
        TextView shutdownContent = view.findViewById(R.id.shutdown_content);
        shutdown.setOnClickListener(v -> {
            CountdownCallbackInterface shutdownCountDownCallback = seconds -> {
                if (seconds <= 0) {
                    shutdownHeading.setText("Shut Down");
                    shutdownContent.setText("Shut down stations");
                } else {
                    if (!cancelledShutdown) {
                        shutdownHeading.setText("Cancel (" + seconds + ")");
                        shutdownContent.setText("Cancel shut down");
                    }
                }
            };
            int[] stationIds = StationsFragment.getInstance().getRoomStations().stream().mapToInt(station -> station.id).toArray();
            if (shutdownHeading.getText().toString().startsWith("Shut Down")) {
                cancelledShutdown = false;
                DialogManager.buildShutdownDialog(getContext(), stationIds, shutdownCountDownCallback);
            } else {
                cancelledShutdown = true;
                String stationIdsString = String.join(", ", Arrays.stream(stationIds).mapToObj(String::valueOf).toArray(String[]::new));
                NetworkService.sendMessage("Station," + stationIdsString, "CommandLine", "CancelShutdown");
                shutdownHeading.setText("Shut Down");
                shutdownContent.setText("Shut down stations");
            }
        });


        SettingsViewModel settingsViewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel.class);
        settingsViewModel.getHideStationControls().observe(getViewLifecycleOwner(), hideStationControls -> {
            View stationControls = view.findViewById(R.id.station_controls);
            stationControls.setVisibility(hideStationControls ? View.GONE : View.VISIBLE);
            View stations = view.findViewById(R.id.stations);
            stations.setVisibility(hideStationControls ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * Load in the initial fragments for the main view.
     */
    private void loadFragments() {
        childManager.beginTransaction()
                .replace(R.id.stations, StationsFragment.class, null)
                .replace(R.id.logo, LogoFragment.class, null)
                .replace(R.id.rooms, RoomFragment.class, null)
                .commitNow();
    }

    String getDayOfMonthSuffix(final int n) {
        if (n < 1 || n > 31) {
            return "";
        }
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }
}
