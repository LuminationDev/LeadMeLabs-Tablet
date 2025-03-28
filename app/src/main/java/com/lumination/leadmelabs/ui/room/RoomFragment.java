package com.lumination.leadmelabs.ui.room;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.ui.appliance.ApplianceFragment;
import com.lumination.leadmelabs.ui.settings.SettingsConstants;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionFragment;
import com.lumination.leadmelabs.unique.snowHydro.stations.SnowyHydroStationsFragment;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Properties;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

public class RoomFragment extends Fragment {
    public static RoomViewModel mViewModel;
    public SoftReference<View> view;
    private SoftReference<RelativeLayout> highlight;

    public static RoomFragment instance;
    public static RoomFragment getInstance() { return instance; }

    //Dynamic dimensions
    private int spacing, marginEnd, width, height;

    public static MutableLiveData<String> currentType = new MutableLiveData<>("All");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        spacing = Helpers.convertDpToPx(158); //158dp - the current size of a single room button + margin
        marginEnd = Helpers.convertDpToPx(25);
        width = Helpers.convertDpToPx(133);
        height = Helpers.convertDpToPx(40);

        view = new SoftReference<>(inflater.inflate(R.layout.fragment_rooms, container, false));
        return view.get();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(mViewModel.getSelectedRoom().getValue() == null) {
            mViewModel.setSelectedRoom("All");
            currentType.setValue("All");
        }

        mViewModel.getRooms().observe(getViewLifecycleOwner(), rooms -> {
            if(rooms.size() > 1) {
                view.findViewById(R.id.room_fragment).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.room_fragment).setVisibility(View.INVISIBLE);
            }

            highlight = new SoftReference<>(view.findViewById(R.id.highlight));
            setupButtons(rooms);
            setupAllButton();
        });

        instance = this;
    }

    /**
     * If the lifecycle has not resumed when the tablet receives the rooms from the NUC the rooms
     * are not displayed until the page is swapped. Here we can manually trigger the rooms to
     * appear.
     */
    public static void ManualRoomTrigger() {
        HashSet<String> rooms = mViewModel.getRooms().getValue();
        if(rooms == null || instance == null) return;

        if(rooms.size() > 1) {
            instance.view.get().findViewById(R.id.room_fragment).setVisibility(View.VISIBLE);
        } else {
            instance.view.get().findViewById(R.id.room_fragment).setVisibility(View.INVISIBLE);
        }

        instance.highlight = new SoftReference<>(instance.view.get().findViewById(R.id.highlight));
        instance.setupButtons(rooms);
        instance.setupAllButton();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupButtons(HashSet<String> rooms) {
        RelativeLayout layout = view.get().findViewById(R.id.room_fragment_container);

        //Rooms have already been loaded, do not double up
        //There are two base elements that are always there
        if(layout.getChildAt(2) != null) {
            return;
        }

        // Copy the HashSet into an iterable list and sort into Alphabetical order
        ArrayList<String> roomSet = new ArrayList<>(rooms);
        Collections.sort(roomSet);

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

            btn.setPadding(Helpers.convertDpToPx(10), Helpers.convertDpToPx(2), Helpers.convertDpToPx(10), 0);
            btn.setGravity(Gravity.CENTER);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
            params.setMargins((spacing * (i + 1)),0, marginEnd,0);

            btn.setLayoutParams(params);

            int finalI = i;
            btn.setOnClickListener(v -> onRoomClick(roomSet.get(finalI), btn.getId()));

            layout.addView(btn, 0);

            positionHighlight(i, roomSet.get(i));
        }
    }

    /**
     * If the room is not set to All then the highlight needs to move to the currently selected
     * room.
     * @param idx An int representing the position in the room set, this translates to how much the
     *            highlight needs to move.
     * @param name A string representing the name of the room that is active.
     */
    private void positionHighlight(int idx, String name) {
        if(!Objects.equals(currentType.getValue(), "All") && Objects.equals(currentType.getValue(), name)) {
            RelativeLayout.LayoutParams allParams = new RelativeLayout.LayoutParams(width, height);
            allParams.setMargins((-spacing + (spacing * (idx + 2))),0,0,0);
            highlight.get().setLayoutParams(allParams);
        }
    }

    /**
     * Setup the only hardcoded button (All) with the on click listener.
     */
    private void setupAllButton() {
        androidx.appcompat.widget.AppCompatButton btn = view.get().findViewById(R.id.all);
        btn.setOnClickListener(v -> onRoomClick("All", btn.getId()));
    }

    private void onRoomClick(String roomName, int id) {
        if(Objects.equals(currentType.getValue(), roomName)) {
            return;
        }

        //Reset any overrides from the appliance adapters
        ApplianceFragment.overrideRoom = null;

        mViewModel.setSelectedRoom(roomName);
        currentType.setValue(roomName);

        if(SideMenuFragment.currentType.equals("dashboard")) {
            String layout = SettingsFragment.mViewModel.getTabletLayoutScheme().getValue();
            if (layout == null || layout.equals(SettingsConstants.DEFAULT_LAYOUT)) {
                StationsFragment.getInstance().notifyDataChange();
            } else {
                SnowyHydroStationsFragment.getInstance().notifyDataChange();
            }
        } else if (SideMenuFragment.currentType.equals("controls")) {
            ApplianceFragment.getInstance().notifyDataChange();
        } else {
            StationSelectionFragment.getInstance().notifyDataChange();
        }

        moveHighlight(id);
        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", "Room Selection");
        segmentProperties.put("name", roomName);
        Segment.trackEvent(SegmentConstants.Room_Selected, segmentProperties);
    }

    /**
     * Move the highlight to the required coordinates based on the element id passed to the function.
     * @param id An in representing the ID of the element that the highlight should move to.
     */
    private void moveHighlight(int id) {
        androidx.appcompat.widget.AppCompatButton btn = view.get().findViewById(id);
        highlight.get().animate().x(btn.getX()).y(btn.getY());
    }
}
