package com.lumination.leadmelabs.ui.sidemenu;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentSideMenuBinding;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.pages.SettingsPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuFragment;
import com.lumination.leadmelabs.ui.stations.SteamSelectionFragment;

import java.util.Arrays;
import java.util.Objects;

public class SideMenuFragment extends Fragment {

    public static SideMenuViewModel mViewModel;
    private View view;
    private FragmentSideMenuBinding binding;
    private ViewGroup.LayoutParams layout;
    public static String currentType;

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
        SettingsViewModel settingsViewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel.class);

        binding.setLifecycleOwner(this);
        binding.setSideMenu(mViewModel);
        mViewModel.setSelectedIcon(settingsViewModel.getHideStationControls().getValue() ? "controls" : "dashboard");
        currentType = settingsViewModel.getHideStationControls().getValue() ? "controls" : "dashboard";

        mViewModel.getInfo().observe(getViewLifecycleOwner(), info -> {
            // update UI elements
        });

        mViewModel.getSelectedIcon().observe(getViewLifecycleOwner(), selectedIcon -> {

        });

        settingsViewModel.getHideStationControls().observe(getViewLifecycleOwner(), hideStationControls -> {
            View sessionButton = view.findViewById(R.id.session_button);
            sessionButton.setVisibility(hideStationControls ? View.GONE : View.VISIBLE);
            View dashboardButton = view.findViewById(R.id.dashboard_button);
            dashboardButton.setVisibility(hideStationControls ? View.GONE : View.VISIBLE);
        });
    }

    //Really easy to set animations
    //.setCustomAnimations(android.R.anim.slide_out_right, android.R.anim.slide_in_left)
    private void setupButtons() {
        view.findViewById(R.id.dashboard_button).setOnClickListener(v -> {
            removeSubMenu();
            loadFragment(DashboardPageFragment.class, "dashboard");
        });

        view.findViewById(R.id.session_button).setOnClickListener(v -> {
            removeSubMenu();
            loadFragment(SteamSelectionFragment.class, "session");
            SteamSelectionFragment.setStationId(0);
        });

        view.findViewById(R.id.controls_button).setOnClickListener(v -> {
            addSubMenu();
            loadFragment(ControlPageFragment.class, "controls");
        });

        view.findViewById(R.id.navigation_button_settings).setOnClickListener(v -> {
            removeSubMenu();
            loadFragment(SettingsPageFragment.class, "navigation");
        });

        view.findViewById(R.id.back_button).setOnClickListener(v ->
            handleBackState()
        );
    }

    /**
     * Loading in the replacement for the main fragment area.
     * @param fragmentClass A Fragment to be loaded in.
     * @param type A string representing the type of fragment being loaded in, i.e. dashboard.
     */
    public static void loadFragment(Class<? extends androidx.fragment.app.Fragment> fragmentClass, String type) {
        if(currentType.equals(type)) {
            return;
        }
        currentType = type;
        
        if(type.equals("dashboard")) {
            clearBackStack();
        }

        MainActivity.fragmentManager.beginTransaction()
                .replace(R.id.main, fragmentClass, null)
                .addToBackStack("menu:" + type)
                .commit();

        MainActivity.fragmentManager.executePendingTransactions();

        //Only change the active icon if the type supplied is a menu icon.
        if(!type.equals("notMenu")) {
            mViewModel.setSelectedIcon(type);
        }
    }

    /**
     * Manage the current fragment back stack, replacing the current one with the previous if
     * the user is within a sub menu.
     */
    private void handleBackState() {
        if (MainActivity.fragmentManager.getBackStackEntryCount() > 1) {
            MainActivity.fragmentManager.popBackStackImmediate();
        }

        String name = MainActivity.fragmentManager
                .getBackStackEntryAt(MainActivity.fragmentManager.getBackStackEntryCount() - 1)
                .getName();

        if(name != null) {
            if(!name.startsWith("menu")) {
                return;
            }

            String[] split = name.split(":");

            mViewModel.setSelectedIcon(split[1]);

            Log.e("SIDE MENU", Arrays.toString(split));
            if(split.length > 2) {
                currentType = split[2];
            } else {
                currentType = split[1];
            }

            if(!name.contains("controls")) {
                removeSubMenu();
                return;
            }

            addSubMenu();
        }
    }

    /**
     * Add the sub menu to the view.
     */
    private void addSubMenu() {
        changeViewParams(150, 22);
        MainActivity.fragmentManager.beginTransaction()
                .replace(R.id.sub_menu, SubMenuFragment.class, null, "sub")
                .commitNow();
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

            changeViewParams(200, 45);
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

    /**
     * Clear the fragment managers back stack on moving to a new menu section.
     */
    private static void clearBackStack() {
        MainActivity.fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
