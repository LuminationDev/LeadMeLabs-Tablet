package com.lumination.leadmelabs.ui.library.application;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentStationShareCodeBinding;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.information.TagUtils;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.segment.analytics.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class ApplicationShareCodeFragment extends Fragment {

    public static FragmentManager childManager;
    public static StationsViewModel mViewModel;

    private FragmentStationShareCodeBinding binding;

    private EditText[] editTexts;

    private static final String segmentClassification = "Experience Code";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_station_share_code, container, false);
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
        SetupEditText(view);

        Button backButton = view.findViewById(R.id.cancel_button);
        backButton.setOnClickListener(v -> {
            mViewModel.selectSelectedApplication("");
            // Go back to the application adapter page
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            if (fragmentManager.getBackStackEntryCount() > 1) {
                fragmentManager.popBackStackImmediate();
            }
            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", segmentClassification);
            segmentProperties.put("stationId", LibrarySelectionFragment.getStationId());
            segmentProperties.put("name", selectedApplication.getName());
            segmentProperties.put("id", selectedApplication.getId());
            segmentProperties.put("type", selectedApplication.getType());
            Segment.trackEvent(SegmentConstants.Cancel_Share_Code, segmentProperties);
        });

        Button playButton = view.findViewById(R.id.launch_experience);
        playButton.setOnClickListener(v -> {
            if (selectedApplication != null) {
                confirmLaunchGame(selectedApplication);
            }
        });

        view.setOnClickListener(v -> dismissKeyboard());
    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
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
        String parameters = generateParameters(selectedApplication.name, editTexts);
        if (parameters.equals(Constants.Invalid)) {
            return;
        }

        //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
        if (MainActivity.isNucJsonEnabled) {
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "Launch");
                message.put("ExperienceId", selectedApplication.id);
                message.put("Parameters", parameters);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            NetworkService.sendMessage("Station," + LibrarySelectionFragment.getStationId(), "Experience", message.toString());
        }
        else {
            String additionalData = "Launch:" + selectedApplication.id;
            additionalData += ":Parameters:" + parameters;

            NetworkService.sendMessage("Station," + LibrarySelectionFragment.getStationId(), "Experience", additionalData);
        }

        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", segmentClassification);
        segmentProperties.put("stationId", LibrarySelectionFragment.getStationId());
        segmentProperties.put("name", selectedApplication.getName());
        segmentProperties.put("id", selectedApplication.getId());
        segmentProperties.put("type", selectedApplication.getType());
        Segment.trackEvent(SegmentConstants.Event_Experience_Launch, segmentProperties);

        SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
        if (fragment != null) {
            fragment.loadFragment(DashboardPageFragment.class, "dashboard", null);
        }
        DialogManager.awaitStationApplicationLaunch(new int[] { LibrarySelectionFragment.getStationId() }, StationsFragment.mViewModel.getSelectedApplicationName(selectedApplication.id), false);
    }

    /**
     * Sets up a pin-like input mechanism for a group of EditText fields.
     *
     * @param view The view containing the EditText fields.
     */
    private void SetupEditText(View view) {
        editTexts = new EditText[]{
                view.findViewById(R.id.et1),
                view.findViewById(R.id.et2),
                view.findViewById(R.id.et3),
                view.findViewById(R.id.et4),
                view.findViewById(R.id.et5),
                view.findViewById(R.id.et6)
        };

        for (int i = 0; i < editTexts.length - 1; i++) {
            int finalI = i;
            editTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        editTexts[finalI + 1].requestFocus();
                    }
                }
            });
        }

        for (int i = 1; i < editTexts.length; i++) {
            int finalI = i;
            editTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (editTexts[finalI].getText().toString().isEmpty()) {
                        editTexts[finalI - 1].requestFocus();
                        return true;
                    }
                }
                return false;
            });
        }
    }

    /**
     * Based on the supplied application name, create a set of parameters that will be inputted as
     * a command line argument on a Station.
     *
     * @param editTexts An array of editText elements that contain the code
     * @param applicationName A string of the application to be launched
     * @return A string to be passed as command line parameters on a Station.
     */
    public static String generateParameters(String applicationName, EditText[] editTexts) {
        StringBuilder combinedText = new StringBuilder();

        if (applicationName.equals("ThingLink")) {
            for (EditText editText : editTexts) {
                combinedText.append(editText.getText().toString());
            }

            //TODO put in validation for share code length of Thinglink etc...

            return combinedText.toString();
        }
        else if (applicationName.equals("CoSpaces")) {
            for (int i = 0; i < editTexts.length; i++) {
                combinedText.append(editTexts[i].getText().toString().trim());
                if (i == 2) { // After appending the third character
                    combinedText.append('-');
                }
            }

            //Check that there is 6 characters (and the '-') for a total of 7
            if (combinedText.length() != 7) {
                Toast.makeText(MainActivity.getInstance().getApplicationContext(), "Invalid Share Code", Toast.LENGTH_SHORT).show();
                return Constants.Invalid;
            }

            return combinedText.toString();
        }

        return "";
    }
}
