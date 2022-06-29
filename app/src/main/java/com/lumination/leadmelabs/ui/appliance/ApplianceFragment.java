package com.lumination.leadmelabs.ui.appliance;

import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ApplianceFragment extends Fragment {
    public static ApplianceViewModel mViewModel;
    private String title;

    public static MutableLiveData<String> type = new MutableLiveData<>();
    public static MutableLiveData<Integer> applianceCount = new MutableLiveData<>(0);
    public static ApplianceParentAdapter applianceParentAdapter;
    public static ApplianceAdapter applianceAdapter;

    public static ArrayMap<String, ArrayList<Appliance>> rooms = new ArrayMap<>();

    public FragmentApplianceBinding binding;

    public static ApplianceFragment instance;
    public static ApplianceFragment getInstance() { return instance; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appliance, container, false);
        binding = DataBindingUtil.bind(view);

        Bundle bundle = getArguments();
        title = bundle != null ? bundle.getString("title") : null;
        type.setValue(bundle != null ? bundle.getString("type") : null);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.setLifecycleOwner(this);

        TextView titleView = view.findViewById(R.id.appliance_title);
        titleView.setText(title);

        //If all rooms are selected break the recycler views into children
        if(Objects.equals(RoomFragment.currentType.getValue(), "All")) {
            loadMultiRecycler(view);
        } else {
            loadSingleRecycler(view);
        }

        //Only add objects that are of the same sub type as the supplied argument
        mViewModel.getAppliances().observe(getViewLifecycleOwner(), this::reloadData);

        mViewModel.getActiveAppliances().observe(getViewLifecycleOwner(), active -> {
            ApplianceViewModel.activeApplianceList = active;
            new filler(active);
        });

        instance = this;
    }

    //TODO breaking when loading before there are rooms available
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

    private void loadSingleRecycler(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.appliance_list);
        recyclerView.setVisibility(View.VISIBLE);

        int numberOfColumns = 4;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), numberOfColumns));
        applianceAdapter = new ApplianceAdapter();
        applianceAdapter.applianceList = new ArrayList<>();
        recyclerView.setAdapter(applianceAdapter);

        applianceCount.setValue(applianceAdapter.applianceList.size());
    }

    /**
     * If the lambda from observer does not access anything then it compiles as a singleton and
     * does not allow for movement between subpages. Instantiating a class bypasses this base
     * behaviour.
     */
    class filler {
        public filler(HashSet<String> active) {
            if(!Objects.equals(RoomFragment.mViewModel.getSelectedRoom().getValue(), "All")) {
                for (String cards : active) {
                    ApplianceAdapter.getInstance().updateIfVisible(cards);
                }
            } else {
                for (String cards : active) {
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
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.subpage, ApplianceFragment.class, args)
                .commitNow();
    }

    private void reloadData(List<Appliance> appliances) {
        String roomType = RoomFragment.mViewModel.getSelectedRoom().getValue();
        if(roomType == null) {
            roomType = "All";
        }

        if(roomType.equals("All")) {
            loadAllRoomData(appliances);
        } else {
            loadSingleRoomData(appliances, roomType);
        }
    }

    /**
     * Load the data required for the ApplianceParentAdapter
     * @param appliances A list of all appliances which may be in the room.
     */
    private void loadAllRoomData(List<Appliance> appliances) {
        rooms = new ArrayMap<>(); //reset the rooms as to no double up on the appliances

        for(Appliance appliance : appliances) {
            if(appliance.type.equals(type.getValue())) {
                if(rooms.containsKey(appliance.room)) {
                    rooms.get(appliance.room).add(appliance);
                    applianceCount.setValue(1);
                } else {
                    rooms.put(appliance.room, new ArrayList<>(Collections.singleton(appliance)));
                }
            }
        }

        applianceParentAdapter.parentModelArrayList = rooms;
    }

    /**
     * Load the data required for the Single ApplianceAdapter and set the new list.
     * @param appliances A list of all appliances which may be in the room.
     * @param roomType A string representing what room is being loaded.
     */
    private void loadSingleRoomData(List<Appliance> appliances, String roomType) {
        ArrayList<Appliance> subtype = new ArrayList<>();

        for(Appliance appliance : appliances) {
            if(appliance.type.equals(type.getValue()) && appliance.room.equals(roomType)) {
                subtype.add(appliance);
            }
        }

        applianceCount.setValue(subtype.size());
        applianceAdapter.applianceList = subtype;
    }
}
