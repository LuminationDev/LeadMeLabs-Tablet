package com.lumination.leadmelabs.ui.sidemenu.submenu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentSubMenuBinding;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.pages.subpages.AppliancePageFragment;

public class SubMenuFragment extends Fragment {

    public static SubMenuViewModel mViewModel;
    private View view;
    private FragmentSubMenuBinding binding;
    private static String currentType;

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

        binding.setLifecycleOwner(this);
        binding.setSubMenu(mViewModel);

        //load the default fragment
        mViewModel.setSelectedPage("lighting");
        currentType = "lighting";
    }

    //Really easy to set animations
    //.setCustomAnimations(android.R.anim.slide_out_right, android.R.anim.slide_in_left)
    private void setupButtons() {
        MainActivity.feedback(view.findViewById(R.id.light_button));
        view.findViewById(R.id.light_button).setOnClickListener(v -> loadFragment("Lighting Control", "lighting"));

        MainActivity.feedback(view.findViewById(R.id.blind_button));
        view.findViewById(R.id.blind_button).setOnClickListener(v -> loadFragment("Blind Controls", "blinds"));

        MainActivity.feedback(view.findViewById(R.id.projector_button));
        view.findViewById(R.id.projector_button).setOnClickListener(v -> loadFragment("Projector Controls", "projectors"));

        MainActivity.feedback(view.findViewById(R.id.ring_button));
        view.findViewById(R.id.ring_button).setOnClickListener(v -> loadFragment("LED Ring Controls", "rings"));

        MainActivity.feedback(view.findViewById(R.id.source_button));
        view.findViewById(R.id.source_button).setOnClickListener(v -> loadFragment("Source Controls", "sources"));
    }

    /**
     * Load in the Appliance page fragment, the bundle passed to the fragment manager details
     * what should be loaded onto the page.
     * @param title A string that is displayed as the title of the fragment.
     * @param type A string representing what appliances to load from the viewModel
     */
    private void loadFragment(String title, String type) {
        if(currentType.equals(type)) {
            return;
        }
        currentType = type;

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("type", type);

        ControlPageFragment.childManager.beginTransaction()
            .replace(R.id.subpage, AppliancePageFragment.class, args)
            .addToBackStack("submenu:" + type)
            .commit();

        ControlPageFragment.childManager.executePendingTransactions();
        mViewModel.setSelectedPage(type);
    }
}
