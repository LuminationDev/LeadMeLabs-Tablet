package com.lumination.leadmelabs.ui.library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.FragmentLibraryBinding;
import com.lumination.leadmelabs.interfaces.ILibraryInterface;
import com.lumination.leadmelabs.models.applications.information.TagConstants;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.classes.SegmentHelpEvent;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.library.application.ApplicationLibraryFragment;
import com.lumination.leadmelabs.ui.library.video.VideoLibraryFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;
import com.lumination.leadmelabs.utilities.Callback;
import com.lumination.leadmelabs.utilities.Debouncer;
import com.segment.analytics.Properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import io.sentry.Sentry;

public class LibrarySelectionFragment extends Fragment {
    private static int currentStationId = 0;
    public static int getStationId() {
        return currentStationId;
    }
    public static void setStationId(int id) {
        currentStationId = id;
    }

    public static LibraryViewModel mViewModel;

    private FragmentLibraryBinding binding;
    public static FragmentManager childManager;

    private ILibraryInterface libraryInterface;

    public static final String segmentClassification = "Library";

    public void setInterface(ILibraryInterface libraryInterface) {
        this.libraryInterface = libraryInterface;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setLibrary(mViewModel);
        binding.setStations(StationsFragment.mViewModel);

        Bundle bundle = getArguments();
        String stationName = bundle != null ? bundle.getString("station") : null;
        String onLoadType = bundle != null ? bundle.getString("library") : "vr_experiences";
        if(onLoadType == null){
            onLoadType = "vr_experiences";
        }

        if (savedInstanceState == null) {
            switchLibrary(onLoadType);

            childManager.beginTransaction()
                    .replace(R.id.logo, LogoFragment.class, null)
                    .commitNow();
        }

        TextView stationTitle = view.findViewById(R.id.selectedStation);
        stationTitle.setVisibility(stationName != null ? View.VISIBLE : View.GONE);
        stationTitle.setText(stationName != null ? MessageFormat.format(" - {0}", stationName) : "");

        setupFilter(view);
        setupButtons(view);

        mViewModel.getSubjectFilters().observe(getViewLifecycleOwner(), filters -> {
            String currentSearch = mViewModel.getCurrentSearch().getValue();
            libraryInterface.performSearch(currentSearch);
        });
    }

    private void setupFilter(View view) {
        // Setup the filter dropdown
        Spinner customSpinner = view.findViewById(R.id.subject_filter_spinner);
        List<String> data = new ArrayList<>(TagConstants.ALL_FILTERS);
        LibrarySubjectFilterAdapter adapter = new LibrarySubjectFilterAdapter(getContext(), data, getViewLifecycleOwner());
        customSpinner.setAdapter(adapter);

        // Setup the filter container
        FlexboxLayout container = view.findViewById(R.id.subject_filter_placeholder);
        container.setOnClickListener(v -> customSpinner.performClick());
    }

    /**
     * Setup the search input and other buttons that exist on the Library page.
     * @param view The view created.
     */
    private void setupButtons(View view) {
        EditText searchInput = view.findViewById(R.id.search_input);
        Properties segmentSearchProperties = new Properties();
        segmentSearchProperties.put("classification", segmentClassification);
        segmentSearchProperties.put("tab", mViewModel.getLibraryTitle());
        class ReportSearch implements Callback {
            @Override
            public void call(Object o) {
                Segment.trackEvent(SegmentConstants.Search_Library, segmentSearchProperties);
            }
        }
        ReportSearch reportSearch = new ReportSearch();
        Debouncer<ReportSearch> debouncer = new Debouncer<ReportSearch>(reportSearch, 2000);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchTerm = searchInput.getText().toString();
                mViewModel.setCurrentSearch(searchTerm);
                libraryInterface.performSearch(searchTerm);

                try {
                    segmentSearchProperties.put("search", searchTerm);
                    debouncer.call(reportSearch);
                } catch (Exception e) {
                    Sentry.captureException(e);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
        searchInput.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                String searchTerm = textView.getText().toString();
                libraryInterface.performSearch(searchTerm);
                dismissKeyboard(searchInput);
            }
            return false;
        });
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                dismissKeyboard(searchInput);
            }
        });

        // Dismiss the keyboard if selecting anywhere on the library page
        LinearLayout libraryArea = view.findViewById(R.id.libraryArea);
        libraryArea.setOnClickListener(v -> {
            dismissKeyboard(searchInput);
        });

        // Switch between the different sub libraries
        FlexboxLayout vrButton = view.findViewById(R.id.view_vr_button);
        vrButton.setOnClickListener(v -> {
            switchLibrary("vr_experiences");
            searchInput.setText("");

            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", segmentClassification);
            segmentProperties.put("name", "vr_experiences");
            Segment.trackEvent(SegmentConstants.Switch_Library_Tab, segmentProperties);
        });

        FlexboxLayout applicationsButton = view.findViewById(R.id.view_applications_button);
        applicationsButton.setOnClickListener(v -> {
            switchLibrary("applications");
            searchInput.setText("");
        });

        FlexboxLayout videoButton = view.findViewById(R.id.view_video_button);
        videoButton.setOnClickListener(v -> {
            switchLibrary("videos");
            searchInput.setText("");

            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", segmentClassification);
            segmentProperties.put("name", "videos");
            Segment.trackEvent(SegmentConstants.Switch_Library_Tab, segmentProperties);
        });

        // Check if there are any experiences running or if the selected Station has an experience running
        FlexboxLayout refresh_btn = view.findViewById(R.id.refresh_experiences_btn);
        refresh_btn.setOnClickListener(v -> libraryInterface.refreshList());

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(HelpPageFragment.class, "help", null);
            // Send data to Segment
            SegmentHelpEvent event = new SegmentHelpEvent(SegmentConstants.Event_Help_Page_Accessed, "Library");
            Segment.trackAction(event);
        });
    }

    /**
     * Switches the library based on the provided library type.
     * Refreshes the search, updates ViewModel properties, sets up the library view,
     * and commits the transaction.
     *
     * @param library The type of library to switch to.
     */
    private void switchLibrary(String library) {
        // Refresh the search
        mViewModel.setCurrentSearch("");

        // Begin a fragment transaction with fade animations
        FragmentTransaction transaction = childManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out);

        // Create a Bundle to pass data to the fragment
        Bundle bundle = new Bundle();
        switch (library) {
            case "vr_experiences":
                // Set up the Bundle for VR experiences
                bundle.putBoolean("isVr", true);
                setupLibrary("VR Library", new ApplicationLibraryFragment(), bundle, transaction);
                break;

            case "applications":
                // Set up the Bundle for applications
                bundle.putBoolean("isVr", false);
                setupLibrary("Application Library", new ApplicationLibraryFragment(), bundle, transaction);
                break;

            case "videos":
                // Set up the Video Library
                setupLibrary("Video Library", new VideoLibraryFragment(), null, transaction);
                break;
        }

        // Update the library type in the ViewModel
        mViewModel.setLibraryType(library);

        // Commit the transaction immediately
        transaction.commitNow();
    }

    /**
     * Sets up the library view with the provided page title, library title, subtitle,
     * fragment, and transaction. Updates ViewModel properties and sets the interface
     * if the fragment implements the ILibraryInterface interface.
     *
     * @param libraryTitle The title of the library.
     * @param fragment     The fragment representing the library.
     * @param bundle       A argument bundle to be passed to the fragment.
     * @param transaction  The FragmentTransaction used for the transaction.
     */
    private void setupLibrary(String libraryTitle, Fragment fragment, Bundle bundle, FragmentTransaction transaction) {
        mViewModel.setLibraryTitle(libraryTitle);

        // Set arguments if bundle is provided
        if (bundle != null) {
            fragment.setArguments(bundle);
        }

        if (fragment instanceof ILibraryInterface) {
            setInterface((ILibraryInterface) fragment);
        }

        transaction.replace(R.id.sub_library, fragment);
    }

    private void dismissKeyboard(View searchInput) {
        Context context = getContext();
        if (context == null) return;

        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
