package com.lumination.leadmelabs.ui.sidemenu;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.pages.SessionPageFragment;
import com.lumination.leadmelabs.ui.pages.SettingsPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuFragment;

public class SideMenuFragment extends Fragment {

    private SideMenuViewModel mViewModel;
    private View view;
    private ImageView session, controls, navigation, dashboard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_side_menu, container, false);
        setupButtons();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(SideMenuViewModel.class);
        mViewModel.getInfo().observe(getViewLifecycleOwner(), info -> {
            // update UI elements
        });

        mViewModel.getSelectedIcon().observe(getViewLifecycleOwner(), selectedIcon -> {

        });
    }

    //Really easy to set animations
    //.setCustomAnimations(android.R.anim.slide_out_right, android.R.anim.slide_in_left)
    private void setupButtons() {
        session = view.findViewById(R.id.session_button_underline);
        view.findViewById(R.id.session_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SessionPageFragment.class, null)
                    .commitNow();

            changeSelectedIcon("session");
        });

        controls = view.findViewById(R.id.controls_button_underline);
        view.findViewById(R.id.controls_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.side_menu, SubMenuFragment.class, null)
                    .replace(R.id.main, ControlPageFragment.class, null)
                    .commitNow();

            changeSelectedIcon("controls");
        });

        navigation = view.findViewById(R.id.navigation_button_settings_underline);
        view.findViewById(R.id.navigation_button_settings).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SettingsPageFragment.class, null)
                    .commitNow();

            changeSelectedIcon("navigation");
        });

        dashboard = view.findViewById(R.id.dashboard_button_underline);
        view.findViewById(R.id.dashboard_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, DashboardPageFragment.class, null)
                    .commitNow();

            changeSelectedIcon("dashboard");
        });
    }

    /**
     * Have not found a way for data binding to work with visibility
     */
    private void changeSelectedIcon(String type) {
        mViewModel.setSelectedIcon(type);

        Log.e("TAG", type);
        switch(type) {
            case "session":
                session.setVisibility(View.VISIBLE);
                break;
            case "controls":
                controls.setVisibility(View.VISIBLE);
                break;
            case "navigation":
                navigation.setVisibility(View.VISIBLE);
                break;
            default:
                dashboard.setVisibility(View.VISIBLE);
        }

        if (!type.equals("session")) {
            session.setVisibility(View.INVISIBLE);
        }
        if (!type.equals("controls")) {
            controls.setVisibility(View.INVISIBLE);
        }
        if (!type.equals("navigation")) {
            navigation.setVisibility(View.INVISIBLE);
        }
        if (!type.equals("dashboard")) {
            dashboard.setVisibility(View.INVISIBLE);
        }
    }
}
