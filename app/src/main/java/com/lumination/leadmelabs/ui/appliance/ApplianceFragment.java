package com.lumination.leadmelabs.ui.appliance;

import android.os.Bundle;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentApplianceBinding;
import com.lumination.leadmelabs.models.Appliance;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.utilities.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ApplianceFragment extends Fragment {
    public static ApplianceViewModel mViewModel;
    private String title;

    public static MutableLiveData<String> type = new MutableLiveData<>();
    public static MutableLiveData<Integer> applianceCount = new MutableLiveData<>(0);
    public static ApplianceParentAdapter applianceParentAdapter;
    public static BaseAdapter applianceAdapter;

    public static ArrayMap<String, ArrayList<Appliance>> rooms = new ArrayMap<>();
    public static String overrideRoom = null;

    public FragmentApplianceBinding binding;

    public static ApplianceFragment instance;
    public static ApplianceFragment getInstance() { return instance; }

    public static LayoutInflater fragmentInflater = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        fragmentInflater = inflater;
        View view = fragmentInflater.inflate(R.layout.fragment_appliance, container, false);
        binding = DataBindingUtil.bind(view);

        Bundle bundle = getArguments();
        title = bundle != null ? bundle.getString("title") : null;
        type.setValue(bundle != null ? bundle.getString("type") : null);

        view.findViewById(R.id.switch_to_all).setOnClickListener(v -> {
            androidx.appcompat.widget.AppCompatButton btn = RoomFragment.getInstance().view.get().findViewById(R.id.all);
            btn.performClick();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.setLifecycleOwner(getViewLifecycleOwner());

        TextView titleView = view.findViewById(R.id.appliance_title);
        titleView.setText(title);

        String roomType = RoomFragment.currentType.getValue();
        //If all rooms are selected break the recycler views into children
        //Or if there is nothing present in other rooms display the single
        if(Objects.equals(roomType, "All") && checkForEmptyRooms(roomType)) {
            loadMultiRecycler(view);
        } else {
            loadSingleRecycler(view, type.getValue());
        }

        //Only add objects that are of the same sub type as the supplied argument
        mViewModel.getAppliances().observe(getViewLifecycleOwner(), active -> reloadData(active, view));

        mViewModel.getActiveAppliances().observe(getViewLifecycleOwner(), active -> {
            ApplianceViewModel.activeApplianceList = active;
            new filler(active);
        });

        instance = this;
    }

    /**
     * Load a nested RecyclerView, each row holds the cards from an individual room in a horizontal
     * list.
     */
    public static void loadMultiRecycler(View view) {
        RecyclerView parentRecyclerView = view.findViewById(R.id.multi_recyclerView);
        parentRecyclerView.setVisibility(View.VISIBLE);

        rooms = new ArrayMap<>();

        if(RoomFragment.mViewModel.getRooms().getValue() != null) {
            for (String room : RoomFragment.mViewModel.getRooms().getValue()) {
                //Load a recycler view with the individual components
                if (!room.equals("All")) { //do not want to double up
                    rooms.put(room, new ArrayList<>());
                }
            }
        }

        parentRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.getInstance().getApplicationContext()));
        applianceParentAdapter = new ApplianceParentAdapter(rooms);
        parentRecyclerView.setAdapter(applianceParentAdapter);
    }

    /**
     * Load a single RecyclerView that presents the appliance cards in a list that expands vertically
     * once the boundaries are reached (Grid-Like).
     */
    private void loadSingleRecycler(View view, String roomType) {
        RecyclerView recyclerView = view.findViewById(R.id.appliance_list);
        recyclerView.setVisibility(View.VISIBLE);

        int numberOfColumns = 4;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), numberOfColumns));
        if (roomType.equals(Constants.LED_WALLS)) {
            applianceAdapter = new RadioAdapter();
        } else {
            applianceAdapter = new ApplianceAdapter();
        }
        applianceAdapter.setApplianceList(new ArrayList<>());
        recyclerView.setAdapter(applianceAdapter);

        applianceCount.setValue(applianceAdapter.getApplianceList().size());
    }

    /**
     * Cycle through the appliances and check if there are any other objects in the room.
     * @return True; if there is more than 1 room with the same object type.
     */
    public static boolean checkForEmptyRooms(String roomType) {
        Set<String> rooms = new HashSet<>();

        //If the room is not all, return
        if(!Objects.equals(roomType, "All")) {
            return false;
        }

        List<Appliance> allAppliances = mViewModel.getAppliances().getValue();

        if(allAppliances == null) {
            return true;
        }

        for (Appliance appliance: allAppliances) {
            if(appliance.matchesDisplayCategory(type.getValue())) {
                rooms.add(appliance.room);
            }
        }

        //Change the room type selected to the only room that exists
        if(rooms.size() == 0) {
            overrideRoom = "No Items";
            return true;
        }
        if(rooms.size() == 1) {
            Object[] room = rooms.toArray();
            overrideRoom = room[0].toString();
        } else {
            overrideRoom = null;
        }

        return rooms.size() > 1;
    }

    /**
     * If the lambda from observer does not access anything then it compiles as a singleton and
     * does not allow for movement between subpages. Instantiating a class bypasses this base
     * behaviour.
     */
    class filler {
        /*
         * Update all available ApplianceAdapters, depending if all rooms are visible or just a single
         * one is active.
         * @param active A list containing the IDs of any appliance card that needs to be refreshed due
         *             to information changing.
         */
        public filler(HashMap<String, String> active) {
            String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();

            boolean roomCheck = checkForEmptyRooms(roomType);
            if((!Objects.equals(roomType, "All") || overrideRoom != null) && !roomCheck) {
                for (String cards : active.keySet()) {
                    ApplianceAdapter.getInstance().updateIfVisible(cards);
                }
            } else {
                for (String cards : active.keySet()) {
                    ApplianceParentAdapter.getInstance().updateIfVisible(cards);
                }
            }
        }
    }

    /**
     * Reload the current appliance fragment when a room is changed. Reloading will trigger the view
     * creation redraw, switching between different layouts depending on what rooms are selected.
     */
    public void notifyDataChange() {
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("type", type.getValue());

        ControlPageFragment.childManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.subpage, ApplianceFragment.class, args)
                .commitNow();
    }

    private void reloadData(List<Appliance> appliances, View view) {
        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();
        if(overrideRoom != null) {
            roomType = overrideRoom;
        }

        if(roomType == null) {
            roomType = "All";
        }

        if(roomType.equals("All") && checkForEmptyRooms(roomType)) {
            loadAllRoomData(appliances);
        } else {
            loadSingleRoomData(appliances, roomType, view);
        }
    }

    /**
     * Load the data required for the ApplianceParentAdapter
     * @param appliances A list of all appliances which may be in the room.
     */
    private void loadAllRoomData(List<Appliance> appliances) {
        rooms = new ArrayMap<>(); //reset the rooms as to no double up on the appliances

        for(Appliance appliance : appliances) {
            if(appliance.matchesDisplayCategory(type.getValue())) {
                if(rooms.containsKey(appliance.room)) {
                    rooms.get(appliance.room).add(appliance);
                } else {
                    rooms.put(appliance.room, new ArrayList<>(Collections.singleton(appliance)));
                }
                applianceCount.setValue(1);
            }
        }

        applianceParentAdapter.parentModelArrayList = rooms;
    }

    /**
     * Load the data required for the Single ApplianceAdapter and set the new list.
     * @param appliances A list of all appliances which may be in the room.
     * @param roomType A string representing what room is being loaded.
     */
    private void loadSingleRoomData(List<Appliance> appliances, String roomType, View view) {
        if(applianceAdapter == null) {
            loadSingleRecycler(view, appliances.get(0).type);
        }

        ArrayList<Appliance> subtype = new ArrayList<>();

        for(Appliance appliance : appliances) {
            if(appliance.matchesDisplayCategory(type.getValue()) &&
                    appliance.room.equals(roomType)) {
                subtype.add(appliance);
            }
        }

        applianceCount.setValue(subtype.size());
        applianceAdapter.setApplianceList(subtype);
    }
}
