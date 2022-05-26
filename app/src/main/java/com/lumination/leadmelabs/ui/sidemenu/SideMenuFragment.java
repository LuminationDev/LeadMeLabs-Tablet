package com.lumination.leadmelabs.ui.sidemenu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentSideMenuBinding;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.pages.QuickStartPageFragment;
import com.lumination.leadmelabs.ui.pages.SettingsPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuFragment;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuViewModel;
import com.lumination.leadmelabs.ui.stations.SteamSelectionFragment;

import java.util.Objects;

public class SideMenuFragment extends Fragment {

    public static SideMenuViewModel mViewModel;
    private View view;
    private FragmentSideMenuBinding binding;

    private ViewGroup.LayoutParams layout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_side_menu, container, false);
        binding = DataBindingUtil.bind(view);
        layout = view.getLayoutParams();
        setupButtons();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.setLifecycleOwner(this);
        binding.setSideMenu(mViewModel);
        mViewModel.setSelectedIcon("quickstart");

        mViewModel.getInfo().observe(getViewLifecycleOwner(), info -> {
            // update UI elements
        });

        mViewModel.getSelectedIcon().observe(getViewLifecycleOwner(), selectedIcon -> {

        });
    }

    //Really easy to set animations
    //.setCustomAnimations(android.R.anim.slide_out_right, android.R.anim.slide_in_left)
    private void setupButtons() {
        view.findViewById(R.id.quickstart_button).setOnClickListener(v -> {
            removeSubMenu();
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, QuickStartPageFragment.class, null)
                    .commitNow();

            mViewModel.setSelectedIcon("quickstart");
        });

        view.findViewById(R.id.session_button).setOnClickListener(v -> {
            removeSubMenu();
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SteamSelectionFragment.class, null)
                    .commitNow();

            mViewModel.setSelectedIcon("session");
        });

        view.findViewById(R.id.controls_button).setOnClickListener(v -> {
            changeViewParams(150, 45);

            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.sub_menu, SubMenuFragment.class, null, "sub")
                    .replace(R.id.main, ControlPageFragment.class, null)
                    .commitNow();

            mViewModel.setSelectedIcon("controls");
        });

        view.findViewById(R.id.navigation_button_settings).setOnClickListener(v -> {
            removeSubMenu();
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, SettingsPageFragment.class, null)
                    .commitNow();

            mViewModel.setSelectedIcon("navigation");
        });

        view.findViewById(R.id.dashboard_button).setOnClickListener(v -> {
            removeSubMenu();
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.main, DashboardPageFragment.class, null)
                    .commitNow();

            mViewModel.setSelectedIcon("dashboard");
        });
    }

    /**
     * Remove the sub menu from the view.
     */
    private void removeSubMenu() {
        Fragment fragment = MainActivity.fragmentManager.findFragmentByTag("sub");

        if(fragment != null) {
            MainActivity.fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNow();

            changeViewParams(200, 70);
        }
    }

    /**
     * Change the width and padding of the side menu. Automatically converts the supplied int to
     * the dp required based on the screen resolution.
     * @param newWidth A integer representing the new width.
     * @param newPadding A integer representing the new padding.
     */
    private void changeViewParams(int newWidth, int newPadding) {
        final float scale = Objects.requireNonNull(getContext()).getResources().getDisplayMetrics().density;
        layout.width = (int) (newWidth * scale + 0.5f);
        view.setLayoutParams(layout);

        int padding = (int) (newPadding * scale + 0.5f);
        view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
    }
}
