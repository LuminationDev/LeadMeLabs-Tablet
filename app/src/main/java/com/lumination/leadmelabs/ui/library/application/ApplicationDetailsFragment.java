package com.lumination.leadmelabs.ui.library.application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationApplicationDetailsBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.information.TagUtils;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.classes.SegmentExperienceEvent;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.Interlinking;

import org.json.JSONException;
import org.json.JSONObject;

public class ApplicationDetailsFragment extends Fragment {

    public static FragmentManager childManager;
    public static StationsViewModel mViewModel;

    private FragmentStationApplicationDetailsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_station_application_details, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);

        mViewModel = new ViewModelProvider(requireActivity()).get(StationsViewModel.class);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        Station station = StationsFragment.mViewModel.getStationById(LibrarySelectionFragment.getStationId());
        binding.setSelectedStation(station);

        //Specifically set the selected application
        Application selectedApplication = mViewModel.getSelectedApplication().getValue();
        binding.setSelectedApplication(selectedApplication);
        if (selectedApplication != null) {
            loadAdditionalInformation(view, selectedApplication);
        }

        Button backButton = view.findViewById(R.id.cancel_button);
        backButton.setOnClickListener(v -> {
            mViewModel.selectSelectedApplication("");
            // Go back to the application adapter page
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            if (fragmentManager.getBackStackEntryCount() > 1) {
                fragmentManager.popBackStackImmediate();
            }
        });

        Button playButton = view.findViewById(R.id.launch_experience);
        playButton.setOnClickListener(v -> {
            if (selectedApplication != null) {
                confirmLaunchGame(selectedApplication);
            }
        });
    }

    /**
     * Loads additional information for the given application and updates the UI.
     *
     * @param view               The parent view where the information will be displayed.
     * @param currentApplication The application object containing information to be displayed.
     */
    private void loadAdditionalInformation(View view, Application currentApplication) {
        Helpers.setExperienceImage(currentApplication.type, currentApplication.name, currentApplication.id, view);

        // Set up tags
        LinearLayout tagsContainer = binding.getRoot().findViewById(R.id.tagsContainer);
        TextView subtagsTextView = binding.getRoot().findViewById(R.id.subTags);
        TextView yearLevelTextView = binding.getRoot().findViewById(R.id.yearLevel);
        TagUtils.setupTags(getContext(), tagsContainer, subtagsTextView, yearLevelTextView, currentApplication);
    }

    public void confirmLaunchGame(Application selectedApplication) {
        Station station = StationsFragment.mViewModel.getStationById(LibrarySelectionFragment.getStationId());
        if (station == null) {
            return;
        }

        String joinedStations = Interlinking.joinStations(station, LibrarySelectionFragment.getStationId(), selectedApplication.getName());
        //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
        if (MainActivity.isNucJsonEnabled) {
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "Launch");
                message.put("ExperienceId", selectedApplication.id);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            NetworkService.sendMessage("Station," + joinedStations, "Experience", message.toString());
        }
        else {
            NetworkService.sendMessage("Station," + joinedStations, "Experience", "Launch:" + selectedApplication.id);
        }

        // Send data to Segment
        SegmentExperienceEvent event = new SegmentExperienceEvent(
                SegmentConstants.Event_Experience_Launch,
                LibrarySelectionFragment.getStationId(),
                selectedApplication.getName(),
                selectedApplication.getId(),
                selectedApplication.getType());
        Segment.trackAction(event);

        SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
        if (fragment != null) {
            fragment.loadFragment(DashboardPageFragment.class, "dashboard", null);
        }

        int[] ids = new int[]{station.id};
        if (Interlinking.multiLaunch(selectedApplication.getName())) {
            ids = Interlinking.collectNestedStations(station, int[].class);
        }

        DialogManager.awaitStationApplicationLaunch(ids, selectedApplication.name, false);
    }
}
