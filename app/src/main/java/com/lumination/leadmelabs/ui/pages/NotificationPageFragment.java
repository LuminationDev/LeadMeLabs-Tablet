package com.lumination.leadmelabs.ui.pages;

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
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.PageNotificationsBinding;
import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.notifications.NotificationAdapter;
import com.lumination.leadmelabs.notifications.NotificationViewModel;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.utilities.Callback;
import com.lumination.leadmelabs.utilities.Debouncer;
import com.segment.analytics.Properties;

import java.util.concurrent.CopyOnWriteArrayList;

import io.sentry.Sentry;

public class NotificationPageFragment extends Fragment {
    public static NotificationViewModel mViewModel;

    public static FragmentManager childManager;
    private PageNotificationsBinding binding;

    private static NotificationAdapter adapter;

    public static final String segmentClassification = "Notification";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page_notifications, container, false);
        childManager = getChildFragmentManager();
        binding = DataBindingUtil.bind(view);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        adapter = new NotificationAdapter(new CopyOnWriteArrayList<>()); // Initialize adapter with an empty list
        recyclerView.setAdapter(adapter);

        //Refresh the notifications
        FlexboxLayout refreshButton = view.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> {
            FirebaseManager.checkForNotifications();
        });

        //Open the help page
        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            SideMenuFragment fragment = ((SideMenuFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu));
            if (fragment == null) return;

            fragment.loadFragment(HelpPageFragment.class, "help", null);
        });

        //Setup the search input
        setupSearchInput(view);

        return view;
    }

    @SuppressLint({"ClickableViewAccessibility", "NotifyDataSetChanged"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setNotification(mViewModel);

        mViewModel.getObjects().observe(getViewLifecycleOwner(), objects -> {
            adapter.setObjects(objects); // Update adapter with new list of objects
            adapter.notifyDataSetChanged();
        });
    }

    /**
     * The underlying data set has changed refresh the notification adapter.
     */
    @SuppressLint("NotifyDataSetChanged")
    public static void refreshData() {
        if (adapter != null) {
            adapter.setObjects(mViewModel.getObjects().getValue());
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Sets up the search input field with event listeners for handling text changes and search actions.
     * It initialises the search properties for tracking, debounce search events, and updates the view model
     * with search queries.
     *
     * @param view the view containing the search input field.
     */
    private void setupSearchInput(View view) {
        //Reset any previous searches
        mViewModel.setCurrentSearch("");

        Properties segmentSearchProperties = new Properties();
        segmentSearchProperties.put("classification", segmentClassification);
        class ReportSearch implements Callback {
            @Override
            public void call(Object o) {
                Segment.trackEvent(SegmentConstants.Search_Library, segmentSearchProperties);
            }
        }
        ReportSearch reportSearch = new ReportSearch();
        Debouncer<ReportSearch> debouncer = new Debouncer<ReportSearch>(reportSearch, 2000);

        EditText searchInput = view.findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchTerm = searchInput.getText().toString();
                mViewModel.searchNotifications(searchTerm);

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
                mViewModel.searchNotifications(searchTerm);
                dismissKeyboard(searchInput);
            }
            return false;
        });

        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                dismissKeyboard(searchInput);
            }
        });

        // Dismiss the keyboard if selecting anywhere on the notification page
        RelativeLayout notificationArea = view.findViewById(R.id.notificationArea);
        notificationArea.setOnClickListener(v -> dismissKeyboard(searchInput));
    }

    private void dismissKeyboard(View searchInput) {
        Context context = getContext();
        if (context == null) return;

        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
