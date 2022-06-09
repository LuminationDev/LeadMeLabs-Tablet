package com.lumination.leadmelabs.ui.room;

import android.os.Bundle;
import android.util.Log;
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

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentRoomsBinding;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.appliance.ApplianceViewModel;
import com.lumination.leadmelabs.ui.pages.ControlPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import java.util.ArrayList;
import java.util.List;

public class RoomFragment extends Fragment {

    public static RoomViewModel mViewModel;
    private View view;
    private FragmentRoomsBinding binding;
    private static String currentType;

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

        if(mViewModel.getSelectedRoom().getValue() == null) {
            mViewModel.setSelectedRoom("All");
            currentType = "All";
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

    private void setupButtons(List<String> rooms) {
        LinearLayout layout = view.findViewById(R.id.room_fragment);
        //For each room in the list create an on click listener
        for (int i = 0; i<rooms.size(); i++) {
            androidx.appcompat.widget.AppCompatButton btn = new androidx.appcompat.widget.AppCompatButton(MainActivity.getInstance());
            btn.setTextSize(13);
            btn.setText(rooms.get(i));
            btn.setStateListAnimator(null);
            btn.setId(i);

            if(rooms.get(i).equals("All")) {
                btn.setTextColor(ResourcesCompat.getColor(MainActivity.getInstance().getResources(), R.color.white, null));
                btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.card_ripple_blue_room_button, null));
            } else {
                btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.bg_white, null));
            }

            btn.setPadding(16, 0, 16, 0);
            btn.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 60);
            params.setMargins(0,0,38,0);

            btn.setLayoutParams(params);

            int finalI = i;
            btn.setOnClickListener(v -> {
                mViewModel.setSelectedRoom(rooms.get(finalI));
                currentType = rooms.get(finalI);

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

        List<String> rooms = mViewModel.getRooms().getValue();
        LinearLayout v;

        for (int i = 0; i < rooms.size(); i++) {
            v = view.findViewById(R.id.room_fragment);
            androidx.appcompat.widget.AppCompatButton btn = (androidx.appcompat.widget.AppCompatButton) v.getChildAt(i);

            if(rooms.get(i).equals(mViewModel.getSelectedRoom().getValue())) {
                btn.setTextColor(ResourcesCompat.getColor(MainActivity.getInstance().getResources(), R.color.white, null));
                btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.card_ripple_blue_room_button, null));
            } else {
                btn.setTextColor(ResourcesCompat.getColor(MainActivity.getInstance().getResources(), R.color.black, null));
                btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.bg_white, null));
            }
        }
    }
}
