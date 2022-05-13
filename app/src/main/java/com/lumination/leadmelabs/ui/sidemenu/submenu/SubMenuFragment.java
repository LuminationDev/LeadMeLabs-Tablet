package com.lumination.leadmelabs.ui.sidemenu.submenu;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentSubMenuBinding;
import com.lumination.leadmelabs.ui.pages.subpages.BlindPageFragment;
import com.lumination.leadmelabs.ui.pages.subpages.LightPageFragment;
import com.lumination.leadmelabs.ui.pages.subpages.ScriptPageFragment;

public class SubMenuFragment extends Fragment {

    private SubMenuViewModel mViewModel;
    private View view;
    private FragmentSubMenuBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sub_menu, container, false);
        binding = DataBindingUtil.bind(view);

        setupButtons();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(SubMenuViewModel.class);
        binding.setLifecycleOwner(this);
        binding.setSubMenu(mViewModel);
        mViewModel.setSelectedPage("light");

        mViewModel.getInfo().observe(getViewLifecycleOwner(), info -> {
            // update UI elements
        });

        mViewModel.getSelectedPage().observe(getViewLifecycleOwner(), selectedPage -> {

        });
    }

    //Really easy to set animations
    //.setCustomAnimations(android.R.anim.slide_out_right, android.R.anim.slide_in_left)
    private void setupButtons() {
        view.findViewById(R.id.light_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.subpage, LightPageFragment.class, null)
                    .commitNow();

            mViewModel.setSelectedPage("light");
        });

        view.findViewById(R.id.blind_button).setOnClickListener(v -> {
            MainActivity.fragmentManager.beginTransaction()
                    .replace(R.id.subpage, BlindPageFragment.class, null)
                    .commitNow();

            mViewModel.setSelectedPage("blind");
        });

        view.findViewById(R.id.projector_button).setOnClickListener(v -> {
//            MainActivity.fragmentManager.beginTransaction()
//                    .replace(R.id.subpage, ProjectorPageFragment.class, null)
//                    .commitNow();

            mViewModel.setSelectedPage("projector");
        });

        view.findViewById(R.id.ring_button).setOnClickListener(v -> {
//            MainActivity.fragmentManager.beginTransaction()
//                    .replace(R.id.subpage, RingPageFragment.class, null)
//                    .commitNow();

            mViewModel.setSelectedPage("ring");
        });

        view.findViewById(R.id.source_button).setOnClickListener(v -> {
//            MainActivity.fragmentManager.beginTransaction()
//                    .replace(R.id.subpage, SourcePageFragment.class, null)
//                    .commitNow();

            mViewModel.setSelectedPage("source");
        });
    }
}
