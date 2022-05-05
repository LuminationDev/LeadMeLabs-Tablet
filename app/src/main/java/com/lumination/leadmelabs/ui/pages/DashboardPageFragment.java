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

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.zones.ZonesFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import java.time.LocalDate;
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
        dateMessage += (now.getDayOfMonth() + " ");
        String monthName = now.getMonth().name();
        monthName = monthName.substring(0, 1) + monthName.substring(1).toLowerCase(Locale.ROOT);
        dateMessage += (monthName + " ");
        dateMessage += (now.getYear() + " ");
        TextView dateMessageView = view.findViewById(R.id.date_message);
        dateMessageView.setText(dateMessage);
    }

    /**
     * Load in the initial fragments for the main view.
     */
    private void loadFragments() {
        childManager.beginTransaction()
                .replace(R.id.stations, StationsFragment.class, null)
                .replace(R.id.zones, ZonesFragment.class, null)
                .replace(R.id.logo, LogoFragment.class, null)
                .commitNow();
    }
}
