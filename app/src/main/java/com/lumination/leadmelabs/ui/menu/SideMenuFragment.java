package com.lumination.leadmelabs.ui.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.ui.pages.ControlsFragment;
import com.lumination.leadmelabs.ui.pages.DashboardFragment;
import com.lumination.leadmelabs.ui.pages.SessionFragment;
import com.lumination.leadmelabs.ui.pages.SettingsFragment;

public class SideMenuFragment extends Fragment {

    private SideMenuViewModel mViewModel;
    private View view;

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
    }

    //Really easy to set animations
    //.setCustomAnimations(android.R.anim.slide_out_right, android.R.anim.slide_in_left)
    private void setupButtons() {
        view.findViewById(R.id.navigation_button_settings).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SettingsFragment.class, null)
                    .commitNow();
        });

        view.findViewById(R.id.session_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SessionFragment.class, null)
                    .commitNow();
        });

        view.findViewById(R.id.controls_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, ControlsFragment.class, null)
                    .commitNow();
        });

        view.findViewById(R.id.dashboard_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, DashboardFragment.class, null)
                    .commitNow();
        });
    }
}
