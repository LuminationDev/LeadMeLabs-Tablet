package com.lumination.leadmelabs.ui.room;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentRoomsBinding;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class RoomFragment extends Fragment {
    public static RoomViewModel mViewModel;
    private View view;
    private FragmentRoomsBinding binding;

    public static MutableLiveData<String> currentType = new MutableLiveData<>("All");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_rooms, container, false);
        binding = DataBindingUtil.bind(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.setLifecycleOwner(this);

        if(mViewModel.getSelectedRoom().getValue() == null) {
            mViewModel.setSelectedRoom("All");
            currentType.setValue("All");
        }

        mViewModel.getRooms().observe(getViewLifecycleOwner(), rooms -> {
            if(rooms.size() > 0) {
                view.findViewById(R.id.room_fragment).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.room_fragment).setVisibility(View.INVISIBLE);
            }

            setupButtons(rooms);
            updateRooms();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupButtons(HashSet<String> rooms) {
        LinearLayout layout = view.findViewById(R.id.room_fragment);

        //Rooms have already been loaded, do not double up
        if(layout.getChildAt(0) != null) {
            return;
        }

        ArrayList<String> roomSet = new ArrayList<>(rooms);

        //Hashset does not always maintain the order, always make sure the All is last
        if(roomSet.contains("All")) {
            roomSet.remove("All");
            roomSet.add("All");
        }

        //For each room in the list create an on click listener
        for (int i = 0; i<roomSet.size(); i++) {
            androidx.appcompat.widget.AppCompatButton btn = new androidx.appcompat.widget.AppCompatButton(MainActivity.getInstance());
            btn.setTextSize(13);
            btn.setAllCaps(false);
            btn.setText(roomSet.get(i));
            btn.setStateListAnimator(null);
            btn.setId(i);

            if(roomSet.get(i).equals("All")) {
                btn.setTextColor(ResourcesCompat.getColor(MainActivity.getInstance().getResources(), R.color.white, null));
                btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.card_blue_room_rounded, null));
            } else {
                btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.card_ripple_white_room_button, null));
            }

            btn.setPadding(16, 4, 16, 0);
            btn.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 60);
            params.setMargins(0,0,38,0);

            btn.setLayoutParams(params);

            int finalI = i;
            btn.setOnClickListener(v -> {
                if(Objects.equals(currentType.getValue(), roomSet.get(finalI))) {
                    return;
                }

                mViewModel.setSelectedRoom(roomSet.get(finalI));
                currentType.setValue(roomSet.get(finalI));

                if(SideMenuFragment.currentType.equals("dashboard")) {
                    StationsFragment.getInstance().notifyDataChange();
                } else if (SideMenuFragment.currentType.equals("controls")) {
                    ApplianceFragment.getInstance().notifyDataChange();
                } else {
                    StationSelectionFragment.getInstance().notifyDataChange();
                }

                updateRooms();
            });

            layout.addView(btn);
        }
    }

    private void updateRooms() {
        if(mViewModel.getRooms().getValue() == null) {
            return;
        }

        HashSet<String> rooms = mViewModel.getRooms().getValue();
        ArrayList<String> roomSet = new ArrayList<>(rooms);

        //Hashset does not always maintain the order, always make sure the All is last
        if(roomSet.contains("All")) {
            roomSet.remove("All");
            roomSet.add("All");
        }

        LinearLayout v;

        for (int i = 0; i < roomSet.size(); i++) {
            v = view.findViewById(R.id.room_fragment);
            androidx.appcompat.widget.AppCompatButton btn = (androidx.appcompat.widget.AppCompatButton) v.getChildAt(i);

            if(roomSet.get(i).equals(mViewModel.getSelectedRoom().getValue())) {
                btn.setTextColor(ResourcesCompat.getColor(MainActivity.getInstance().getResources(), R.color.white, null));
                btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.card_blue_room_rounded, null));
            } else {
                btn.setTextColor(ResourcesCompat.getColor(MainActivity.getInstance().getResources(), R.color.black, null));
                btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.card_ripple_white_room_button, null));
            }
        }
    }
}
