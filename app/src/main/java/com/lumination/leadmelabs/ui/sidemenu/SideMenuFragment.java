package com.lumination.leadmelabs.ui.sidemenu;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentMenuSideBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.pages.SettingsPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.settings.SettingsViewModel;
import com.lumination.leadmelabs.ui.sidemenu.submenu.SubMenuFragment;
import com.lumination.leadmelabs.ui.application.ApplicationSelectionFragment;

import java.util.Objects;

public class SideMenuFragment extends Fragment {

    public static SideMenuViewModel mViewModel;
    private View view;
    private FragmentMenuSideBinding binding;
    private ViewGroup.LayoutParams layout;
    public static String currentType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_menu_side, container, false);

        Bundle bundle = getArguments();
        String menuSize = bundle != null ? bundle.getString("menuSize") : null;
        binding = DataBindingUtil.bind(view);
        layout = view.getLayoutParams();
        if(menuSize != null) {
            changeViewParams(150, 22);
        }

        setupButtons();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SettingsViewModel settingsViewModel = ViewModelProviders.of(requireActivity()).get(SettingsViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setSideMenu(mViewModel);
        binding.setSettings(SettingsFragment.mViewModel);

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
        View dashBtn = view.findViewById(R.id.dashboard_button);
        buttonFeedback(dashBtn, "dashboard");
        dashBtn.setOnClickListener(v -> {
            removeSubMenu();
            loadFragment(DashboardPageFragment.class, "dashboard");
        });

        View sessionBtn = view.findViewById(R.id.session_button);
        buttonFeedback(sessionBtn, "session");
        sessionBtn.setOnClickListener(v -> {
            removeSubMenu();
            loadFragment(ApplicationSelectionFragment.class, "session");
            ApplicationSelectionFragment.setStationId(0);
        });

        View controlsBtn = view.findViewById(R.id.controls_button);
        buttonFeedback(controlsBtn, "controls");
        controlsBtn.setOnClickListener(v -> {
            if(!currentType.equals("controls")) { addSubMenu(); }
            loadFragment(ControlPageFragment.class, "controls");
        });

        View settingsBtn = view.findViewById(R.id.settings_button);
        buttonFeedback(settingsBtn, "settings");
        settingsBtn.setOnClickListener(v ->
                DialogManager.confirmPinCode(this, "replace")
        );

        view.findViewById(R.id.back_button).setOnClickListener(v ->
                handleBackState()
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    private void buttonFeedback(View view, String type) {
        view.setOnTouchListener((v, event) -> {
            if(currentType.equals(type)) {
                return false;
            }

            switch(event.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setBackgroundResource(0);
                    break;

                case MotionEvent.ACTION_DOWN:
                    v.setBackground(ResourcesCompat.getDrawable(
                            MainActivity.getInstance().getResources(),
                            R.drawable.card_ripple_trans_active,
                            null)
                    );
                    break;
            }

            return false;
        });
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
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
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
            FragmentManager.BackStackEntry backStackEntry = MainActivity.fragmentManager.getBackStackEntryAt(MainActivity.fragmentManager.getBackStackEntryCount() - 2);
            if (Objects.equals(backStackEntry.getName(), "menu:settings")) {
                DialogManager.confirmPinCode(this, "back");
            } else {
                MainActivity.fragmentManager.popBackStackImmediate();
            }
        }

        setSideMenuIcon();
        handleSubMenuOnNavigate();
    }

    private String getMenuName() {
        String name = MainActivity.fragmentManager
                .getBackStackEntryAt(MainActivity.fragmentManager.getBackStackEntryCount() - 1)
                .getName();

        if (name == null) {
            return null;
        }

        if(!name.startsWith("menu")) {
            return null;
        }

        return name;
    }

    private void setSideMenuIcon() {
        String name = getMenuName();
        if (name != null) {
            String[] split = name.split(":");

            mViewModel.setSelectedIcon(split[1]);
        }

    }

    private void handleSubMenuOnNavigate() {
        String name = getMenuName();
        if (name != null) {
            String[] split = name.split(":");

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
                .setCustomAnimations(android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right)
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
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        FlexboxLayout flexLayout = view.findViewById(R.id.side_menu_fragment);
        flexLayout.setLayoutTransition(layoutTransition);

        final float scale = requireContext().getResources().getDisplayMetrics().density;
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

    public void navigateToSettingsPage(String navigationType) {
        switch (navigationType) {
            case "back":
                MainActivity.fragmentManager.popBackStackImmediate();
                break;
            case "replace":
                loadFragment(SettingsPageFragment.class, "settings");
                break;
        }

        setSideMenuIcon();
        handleSubMenuOnNavigate();
    }
}
