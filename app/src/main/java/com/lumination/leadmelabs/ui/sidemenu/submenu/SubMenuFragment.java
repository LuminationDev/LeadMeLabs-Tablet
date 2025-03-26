package com.lumination.leadmelabs.ui.sidemenu.submenu;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentMenuSubBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.utilities.Helpers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SubMenuFragment extends Fragment {

    public static SubMenuViewModel mViewModel;
    private View view;
    private FragmentMenuSubBinding binding;
    private static String currentType;
    public static MutableLiveData<Integer> currentIcon = new MutableLiveData<>();
    public static HashSet<String> checkedApplianceTypes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_menu_sub, container, false);
        binding = DataBindingUtil.bind(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setSubMenu(mViewModel);

        mViewModel.getSubObjects().observe(getViewLifecycleOwner(), this::createSubObjects);

        mViewModel.setSelectedPage("scenes");
        currentType = "scenes";
        currentIcon.setValue(R.drawable.icon_empty_scenes);
    }

    /**
     * For each appliance type that is sent through, create a Submenu option for it, appending it
     * to the menu with functionality.
     * @param applianceTypes A HashSet containing the different appliance types.
     */
    private void createSubObjects(HashSet<String> applianceTypes) {
        if(applianceTypes.size() > 0) {
            checkedApplianceTypes = new HashSet<>();
            HashMap<String, TextView> options = new HashMap<>();
            List<TextView> orderedOptions = new ArrayList<>();

            //Loop through the stored appliances and check if there is any pages without a linked
            //appliance. This would occur when there are locked rooms set.
            HashSet<String> lockedRooms = SettingsFragment.mViewModel.getLockedIfEnabled().getValue();
            if (lockedRooms == null) { //need to perform a null check first
                checkedApplianceTypes = applianceTypes;
            } else if (lockedRooms.size() == 0) {
                checkedApplianceTypes = applianceTypes;
            } else {
                List<Appliance> appliances = ApplianceFragment.mViewModel.getAppliances().getValue();

                if (appliances != null) {
                    for (Appliance appliance : appliances) {
                        if (SettingsFragment.mViewModel.getLockedIfEnabled().getValue().contains(appliance.room)) {
                            if (appliance.displayType != null) {
                                checkedApplianceTypes.add(appliance.displayType);
                            } else {
                                checkedApplianceTypes.add(appliance.type);
                            }
                        }
                    }
                }
            }

            for (String type : checkedApplianceTypes) {
                options.put(type, createObject(type));
            }

            //Organise the list into typical order
            orderedOptions.add(options.remove("scenes"));
            orderedOptions.add(options.remove("lights"));

            //Add the rest
            for(Map.Entry<String, TextView> entry : options.entrySet()) {
                orderedOptions.add(entry.getValue());
            }

            //Append the options
            FlexboxLayout menu = view.findViewById(R.id.side_menu_options);
            menu.removeAllViews();
            for(TextView option: orderedOptions) {
                if(option != null) {
                    menu.addView(option);
                }
            }
        }
    }

    /**
     * Create a TextView layout that acts as the SubMenu option.
     * @param type A string describing the type of object that requires a page.
     * @return A TextView layout.
     */
    private TextView createObject(String type) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Helpers.convertDpToPx(40));

        TextView tv = new TextView(MainActivity.getInstance());
        tv.setLayoutParams(params);
        //Capitalise the first letter of the type
        tv.setText(MessageFormat.format("{0}{1}", type.substring(0, 1).toUpperCase(), type.substring(1)));

        //Scenes is always the first item selected
        tv.setTextColor(ContextCompat.getColor(MainActivity.getInstance(), type.equals("scenes") ? R.color.blue : R.color.black));

        tv.setId(View.generateViewId());
        tv.setPadding(Helpers.convertDpToPx(15), 0 , Helpers.convertDpToPx(15), 0);
        tv.setGravity(Gravity.CENTER_VERTICAL);

        tv.setOnClickListener(v -> {
            loadFragment(getTitle(type), type);
            changeHighlight(type);
        });
        feedback(tv);

        return tv;
    }

    /**
     * Change the current highlighted option
     */
    private void changeHighlight(String type) {
        FlexboxLayout layout = view.findViewById(R.id.side_menu_options);
        int count = layout.getChildCount();
        TextView tv;
        for(int i=0; i<count; i++) {
            tv = (TextView) layout.getChildAt(i);

            if(tv.getText().toString().equalsIgnoreCase(type)) {
                tv.setTextColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.blue));
            } else {
                tv.setTextColor(ContextCompat.getColor(MainActivity.getInstance(), R.color.black));
            }
        }
    }

    /**
     * Determine the title of the fragment page that will result from a selected appliance type
     * being selected for preview.
     * @param type A string representing a type of appliance.
     * @return A string representing the title of the subpage fragment.
     */
    public static String getTitle(String type) {
        switch (type) {
            case "scenes":
                return "Scenes";
            case "lights":
                return "Lighting Controls";
            case "blinds":
                return "Blind Controls";
            case "computers":
                return "Computer Controls";
            case "projectors":
                return "Projector Controls";
            case "LED rings":
                return "LED Ring Controls";
            case "LED walls":
                return "LED Wall Controls";
            case "sources":
                return "Source Controls";
            case "splicers":
                return "Splicer Controls";
            default:
                return null;
        }
    }

    /**
     * Change the background of the selected view while a user is touching it.
     * @param view A view which has an OnTouchListener added.
     */
    @SuppressLint("ClickableViewAccessibility")
    public void feedback(View view) {
        view.setOnTouchListener((v, event) -> {
            switch(event.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setBackgroundResource(0);
                    break;

                case MotionEvent.ACTION_DOWN:
                    v.setBackground(ResourcesCompat.getDrawable(
                            MainActivity.getInstance().getResources(),
                            R.drawable.icon_touch_event,
                            null)
                    );
                    break;
            }

            return false;
        });
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
                .replace(R.id.subpage, ApplianceFragment.class, args)
                .addToBackStack("submenu:" + type)
                .commit();
        Segment.trackScreen("submenu:" + type);

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
            case "splicers":
            case "sources":
                currentIcon.setValue(R.drawable.icon_empty_scenes);
                break;
            case "LED rings":
            case "LED walls":
                currentIcon.setValue(R.drawable.icon_empty_led);
                break;
            case "lights":
                currentIcon.setValue(R.drawable.icon_empty_lights);
                break;
            case "blinds":
                currentIcon.setValue(R.drawable.icon_empty_blinds);
                break;
            case "computers":
            case "projectors":
                currentIcon.setValue(R.drawable.icon_empty_projector);
                break;
            default:
                currentIcon.setValue(R.drawable.icon_appliance_light_bulb_off);
        }
    }
}
