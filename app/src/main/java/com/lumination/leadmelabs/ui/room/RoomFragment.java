package com.lumination.leadmelabs.ui.room;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
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
    private RelativeLayout highlight;

    public static MutableLiveData<String> currentType = new MutableLiveData<>("All");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_rooms, container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

            highlight = view.findViewById(R.id.highlight);
            setupButtons(rooms);
            setupAllButton();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupButtons(HashSet<String> rooms) {
        LinearLayout layout = view.findViewById(R.id.room_fragment);

        //Rooms have already been loaded, do not double up
        if(layout.getChildAt(2) != null) {
            return;
        }

        ArrayList<String> roomSet = new ArrayList<>(rooms);

        //Hashset does not always maintain the order, always make sure the All is last
        roomSet.remove("All");

        //For each room in the list create an on click listener
        for (int i = 0; i<roomSet.size(); i++) {
            androidx.appcompat.widget.AppCompatButton btn = new androidx.appcompat.widget.AppCompatButton(MainActivity.getInstance());
            btn.setTextSize(13);
            btn.setAllCaps(false);
            btn.setText(roomSet.get(i));
            btn.setStateListAnimator(null);
            btn.setId(View.generateViewId());

            btn.setBackground(ResourcesCompat.getDrawable(MainActivity.getInstance().getResources(), R.drawable.card_ripple_white_room_button, null));

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

                moveHighlight(btn.getId(), roomSet.get(finalI));
            });

            layout.addView(btn);
        }
    }

    /**
     * Setup the only hardcoded button (All) with the on click listener.
     */
    private void setupAllButton() {
        androidx.appcompat.widget.AppCompatButton btn = view.findViewById(R.id.all);
        btn.setOnClickListener(v -> {
            if(Objects.equals(currentType.getValue(), "All")) {
                return;
            }

            mViewModel.setSelectedRoom("All");
            currentType.setValue("All");

            if(SideMenuFragment.currentType.equals("dashboard")) {
                StationsFragment.getInstance().notifyDataChange();
            } else if (SideMenuFragment.currentType.equals("controls")) {
                ApplianceFragment.getInstance().notifyDataChange();
            } else {
                StationSelectionFragment.getInstance().notifyDataChange();
            }

            moveHighlight(btn.getId(), "All");
        });
    }

    /**
     * Move the highlight to the required coordinates based on the element id passed to the function.
     * @param id An in representing the ID of the element that the highlight should move to.
     */
    private void moveHighlight(int id, String name) {
        androidx.appcompat.widget.AppCompatButton btn = view.findViewById(id);
        highlight.animate().x(btn.getX()).y(btn.getY());

        TextView text = view.findViewById(R.id.highlight_title);
        text.setText(name);
    }
}
