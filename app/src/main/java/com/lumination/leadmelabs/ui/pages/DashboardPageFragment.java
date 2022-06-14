package com.lumination.leadmelabs.ui.pages;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.ui.stations.SteamSelectionFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class DashboardPageFragment extends Fragment {
    private View view;
    private FragmentManager childManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.page_dashboard, container, false);
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
        dayName = dayName.substring(0, 1) + dayName.substring(1).toLowerCase(Locale.ROOT);
        dateMessage += (dayName + " ");
        dateMessage += (now.getDayOfMonth() + getDayOfMonthSuffix(now.getDayOfMonth()) +" ");
        String monthName = now.getMonth().name();
        monthName = monthName.substring(0, 1) + monthName.substring(1).toLowerCase(Locale.ROOT);
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
            int[] selectedIds = new ViewModelProvider(requireActivity()).get(StationsViewModel.class).getAllStationIds();
            String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

            NetworkService.sendMessage("Station," + stationIds, "CommandLine", "EndVR");
<<<<<<< HEAD
            DialogManager.awaitStationEndSession(selectedIds);
=======
            MainActivity.awaitStationEndSession(selectedIds);
>>>>>>> main
        });

        FlexboxLayout identify = view.findViewById(R.id.identify_button);
        identify.setOnClickListener(v -> {
            int[] selectedIds = new ViewModelProvider(requireActivity()).get(StationsViewModel.class).getAllStationIds();
            String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));
            NetworkService.sendMessage("Station," + stationIds, "CommandLine", "IdentifyStation");
            Toast.makeText(getContext(), "Stations located successfully", Toast.LENGTH_SHORT).show();
        });

        FlexboxLayout shutdown = view.findViewById(R.id.shutdown_button);
        shutdown.setOnClickListener(v -> {
            int[] selectedIds = new ViewModelProvider(requireActivity()).get(StationsViewModel.class).getAllStationIds();
            String stationIds = String.join(", ", Arrays.stream(selectedIds).mapToObj(String::valueOf).toArray(String[]::new));

            NetworkService.sendMessage("Station," + stationIds, "CommandLine", "Shutdown");

            View shutdownDialogView = View.inflate(getContext(), R.layout.dialog_template, null);
            Button confirmButton = shutdownDialogView.findViewById(R.id.confirm_button);
            Button cancelButton = shutdownDialogView.findViewById(R.id.cancel_button);
            TextView title = shutdownDialogView.findViewById(R.id.title);
            TextView contentText = shutdownDialogView.findViewById(R.id.content_text);
            title.setText("Shutting Down");
            contentText.setText("Cancel shutdown?");
            androidx.appcompat.app.AlertDialog confirmDialog = new androidx.appcompat.app.AlertDialog.Builder(getContext()).setView(shutdownDialogView).create();
            confirmDialog.setCancelable(false);
            confirmDialog.setCanceledOnTouchOutside(false);
            confirmButton.setOnClickListener(w -> confirmDialog.dismiss());
            cancelButton.setOnClickListener(x -> {
                NetworkService.sendMessage("Station," + stationIds, "CommandLine", "CancelShutdown");
                confirmDialog.dismiss();
            });
            confirmButton.setText("Continue");
            cancelButton.setText("Cancel (10)");
            confirmDialog.show();
            confirmDialog.getWindow().setLayout(1200, 380);

            CountDownTimer timer = new CountDownTimer(9000, 1000) {
                @Override
                public void onTick(long l) {
                    cancelButton.setText("Cancel (" + (l + 1000) / 1000 + ")");
                }

                @Override
                public void onFinish() {
                    confirmDialog.dismiss();
                }
            }.start();
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
