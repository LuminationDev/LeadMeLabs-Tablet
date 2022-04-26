package com.lumination.leadmelabs.ui.sidemenu.submenu;

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
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.pages.subpages.BlindPageFragment;
import com.lumination.leadmelabs.ui.pages.subpages.LightPageFragment;
import com.lumination.leadmelabs.ui.pages.subpages.ScriptPageFragment;

public class SubMenuFragment extends Fragment {

    private SubMenuViewModel mViewModel;
    private View view;
    private ImageView lights, blinds, scripts, dashboard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sub_menu, container, false);
        setupButtons();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(SubMenuViewModel.class);
        mViewModel.getInfo().observe(getViewLifecycleOwner(), info -> {
            // update UI elements
        });

        mViewModel.getSelectedIcon().observe(getViewLifecycleOwner(), selectedIcon -> {

        });
    }

    //Really easy to set animations
    //.setCustomAnimations(android.R.anim.slide_out_right, android.R.anim.slide_in_left)
    private void setupButtons() {
        lights = view.findViewById(R.id.lights_button_underline);
        view.findViewById(R.id.lights_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.subpage, LightPageFragment.class, null)
                    .commitNow();

            changeSelectedIcon("lights");
        });

        blinds = view.findViewById(R.id.blinds_button_underline);
        view.findViewById(R.id.blinds_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.subpage, BlindPageFragment.class, null)
                    .commitNow();

            changeSelectedIcon("blinds");
        });

        scripts = view.findViewById(R.id.scripts_button_underline);
        view.findViewById(R.id.scripts_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.subpage, ScriptPageFragment.class, null)
                    .commitNow();

            changeSelectedIcon("scripts");
        });

        dashboard = view.findViewById(R.id.dashboard_button_underline);
        view.findViewById(R.id.dashboard_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.side_menu, SideMenuFragment.class, null)
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
            case "lights":
                lights.setVisibility(View.VISIBLE);
                break;
            case "blinds":
                blinds.setVisibility(View.VISIBLE);
                break;
            case "scripts":
                scripts.setVisibility(View.VISIBLE);
                break;
            default:
                dashboard.setVisibility(View.VISIBLE);
        }

        if (!type.equals("lights")) {
            lights.setVisibility(View.INVISIBLE);
        }
        if (!type.equals("blinds")) {
            blinds.setVisibility(View.INVISIBLE);
        }
        if (!type.equals("scripts")) {
            scripts.setVisibility(View.INVISIBLE);
        }
        if (!type.equals("dashboard")) {
            dashboard.setVisibility(View.INVISIBLE);
        }
    }
}
