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
import com.lumination.leadmelabs.ui.help.HelpPageFragment;
import com.lumination.leadmelabs.ui.library.application.ApplicationLibraryFragment;
import com.lumination.leadmelabs.ui.library.video.VideoLibraryFragment;
import com.lumination.leadmelabs.ui.logo.LogoFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import java.text.MessageFormat;

public class LibrarySelectionFragment extends Fragment {
    public static LibraryViewModel mViewModel;

    private FragmentLibraryBinding binding;
    public static FragmentManager childManager;

    private ILibraryInterface libraryInterface;

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

        setupButtons(view);
    }

    /**
     * Setup the search input and other buttons that exist on the Library page.
     * @param view The view created.
     */
    private void setupButtons(View view) {
        EditText searchInput = view.findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchTerm = searchInput.getText().toString();
                mViewModel.setCurrentSearch(searchTerm);
                libraryInterface.performSearch(searchTerm);
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
        });

        FlexboxLayout videoButton = view.findViewById(R.id.view_video_button);
        videoButton.setOnClickListener(v -> {
            switchLibrary("videos");
            searchInput.setText("");
        });

        // Check if there are any experiences running or if the selected Station has an experience running
        Button refresh_btn = view.findViewById(R.id.refresh_experiences_btn);
        refresh_btn.setOnClickListener(v -> libraryInterface.refreshList());

        FlexboxLayout helpButton = view.findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            ((SideMenuFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.side_menu)).loadFragment(HelpPageFragment.class, "help", null);
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
        FragmentTransaction transaction = childManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out);

        switch (library) {
            case "vr_experiences":
                setupLibrary("VR Library", "VR Library", "Pick an experience to play in VR", new ApplicationLibraryFragment(), transaction);
                break;

            case "videos":
                setupLibrary("Video Library", "Video Library", "Pick a video to watch", new VideoLibraryFragment(), transaction);
                break;
        }

        // Update the library type
        mViewModel.setLibraryType(library);

        transaction.commitNow();
    }

    /**
     * Sets up the library view with the provided page title, library title, subtitle,
     * fragment, and transaction. Updates ViewModel properties and sets the interface
     * if the fragment implements the ILibraryInterface interface.
     *
     * @param pageTitle    The title of the page.
     * @param libraryTitle The title of the library.
     * @param subTitle     The subtitle of the library.
     * @param fragment     The fragment representing the library.
     * @param transaction  The FragmentTransaction used for the transaction.
     */
    private void setupLibrary(String pageTitle, String libraryTitle, String subTitle, Fragment fragment, FragmentTransaction transaction) {
        mViewModel.setPageTitle(pageTitle);
        mViewModel.setLibraryTitle(libraryTitle);
        mViewModel.setSubTitle(subTitle);

        if (fragment instanceof ILibraryInterface) {
            setInterface((ILibraryInterface) fragment);
        }

        transaction.replace(R.id.sub_library, fragment);
    }

    private void dismissKeyboard(View searchInput) {
        InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(searchInput.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
