package com.lumination.leadmelabs.ui.sidemenu.submenu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentMenuSubBinding;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.pages.subpages.AppliancePageFragment;

public class SubMenuFragment extends Fragment {

    public static SubMenuViewModel mViewModel;
    private View view;
    private FragmentMenuSubBinding binding;
    private static String currentType;
    public static MutableLiveData<Integer> currentIcon = new MutableLiveData<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_menu_sub, container, false);
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
        mViewModel.setSelectedPage("scenes");
        currentType = "scenes";
        currentIcon.setValue(R.drawable.icon_empty_scenes);
    }

    private void setupButtons() {
        MainActivity.feedback(view.findViewById(R.id.scene_button));
        view.findViewById(R.id.scene_button).setOnClickListener(v -> loadFragment("Scenes", "scenes"));

        MainActivity.feedback(view.findViewById(R.id.light_button));
        view.findViewById(R.id.light_button).setOnClickListener(v -> loadFragment("Lighting Control", "lights"));

        MainActivity.feedback(view.findViewById(R.id.blind_button));
        view.findViewById(R.id.blind_button).setOnClickListener(v -> loadFragment("Blind Controls", "blinds"));

        MainActivity.feedback(view.findViewById(R.id.projector_button));
        view.findViewById(R.id.projector_button).setOnClickListener(v -> loadFragment("Projector Controls", "projectors"));

        MainActivity.feedback(view.findViewById(R.id.ring_button));
        view.findViewById(R.id.ring_button).setOnClickListener(v -> loadFragment("LED Ring Controls", "LED rings"));

        MainActivity.feedback(view.findViewById(R.id.source_button));
        view.findViewById(R.id.source_button).setOnClickListener(v -> loadFragment("Source Controls", "sources"));
    }

    /**
     * Load in the Appliance page fragment, the bundle passed to the fragment manager details
     * what should be loaded onto the page.
     * @param title A string that is displayed as the title of the fragment.
     * @param type A string representing what appliances to load from the viewModel
     */
    public static void loadFragment(String title, String type) {
        if(currentType.equals(type)) {
            return;
        }
        currentType = type;

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("type", type);

        ControlPageFragment.childManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in,
                    R.anim.fade_out)
            .replace(R.id.subpage, AppliancePageFragment.class, args)
            .addToBackStack("submenu:" + type)
            .commit();

        ControlPageFragment.childManager.executePendingTransactions();
        changeIcon(type);
        mViewModel.setSelectedPage(type);
    }

    /**
     * Change the room icon that is displayed on the no appliances available card.
     */
    private static void changeIcon(String type) {
        switch (type) {
            case "scenes":
                currentIcon.setValue(R.drawable.icon_empty_scenes);
                break;
            case "LED rings":
                currentIcon.setValue(R.drawable.icon_empty_led);
                break;
            case "lights":
                currentIcon.setValue(R.drawable.icon_empty_lights);
                break;
            case "blinds":
                currentIcon.setValue(R.drawable.icon_empty_blinds);
                break;
            case "projectors":
                currentIcon.setValue(R.drawable.icon_empty_projector);
                break;
            case "sources":
                currentIcon.setValue(R.drawable.icon_empty_scenes);
                break;
            default:
                currentIcon.setValue(R.drawable.icon_appliance_light_bulb_off);
        }
    }
}
