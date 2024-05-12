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

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.dashboard.DashboardFragment;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsConstants;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.unique.snowHydro.SnowyHydroConstants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.Identifier;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardPageFragment extends Fragment {
    private FragmentManager childManager;

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

        //Run the identify flow
        FlexboxLayout identify = view.findViewById(R.id.identify_button);
        identify.setOnClickListener(v -> {
            List<Station> stations = Helpers.getRoomStations();
            Identifier.identifyStations(stations);
        });

        //Open the help page
        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(HelpPageFragment.class, "help", null);
        });

        if (savedInstanceState == null) {
            loadFragments();
        }

        setupDashboardTitleLayout(view);
    }

    private void setupDashboardTitleLayout(View view) {
        String layout = SettingsFragment.mViewModel.getTabletLayoutScheme().getValue();

        //This happens regardless of the layout type
        setupTimeDateDisplay(view);

        if (layout == null) {
            setupWelcomeTitle(view);
            return;
        }

        switch (layout) {
            case SettingsConstants.SNOWY_HYDRO_LAYOUT:
                setupSnowyTitle(view);
                break;

            case SettingsConstants.DEFAULT_LAYOUT:
            default:
                setupWelcomeTitle(view);
                break;
        }
    }

    //region Welcome Text
    /**
     * Setup the welcome text based on the welcome constant saved in the Snowy Hydro folder.
     * @param view The parent view where the information will be displayed.
     */
    private void setupSnowyTitle(View view) {
        TextView welcomeMessage = view.findViewById(R.id.welcome_message);
        welcomeMessage.setText(SnowyHydroConstants.WELCOME);
    }

    /**
     * Setup the welcome text for the user that changes depending on the time of day.
     * selected tablet layout.
     * @param view The parent view where the information will be displayed.
     */
    private void setupWelcomeTitle(View view) {
        int currentDate = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String message = "Welcome!";
        if (currentDate < 12) {
            message = "Good Morning!";
        } else if (currentDate < 17) {
            message = "Good Afternoon!";
        } else {
            message = "Good Evening!";
        }
        TextView welcomeMessage = view.findViewById(R.id.welcome_message);
        welcomeMessage.setText(message);
    }

    /**
     * Setup the time and date display that appears under the welcome text.
     * @param view The parent view where the information will be displayed.
     */
    private void setupTimeDateDisplay(View view) {
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
    //endregion

    /**
     * Load in the initial fragments for the main view.
     */
    private void loadFragments() {
        childManager.beginTransaction()
                .replace(R.id.dashboard_area, DashboardFragment.class, null)
                .replace(R.id.logo, LogoFragment.class, null)
                .replace(R.id.rooms, RoomFragment.class, null)
                .commitNow();
    }
}
