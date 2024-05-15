package com.lumination.leadmelabs.ui.library.application;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardApplicationBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.information.TagUtils;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.pages.LibraryPageFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.Interlinking;
import com.segment.analytics.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ApplicationAdapter extends BaseAdapter implements Filterable {
    public ArrayList<Application> applicationList = new ArrayList<>();

    private final LayoutInflater mInflater;
    private final Context context;
    public static StationsViewModel mViewModel;
    private final FragmentManager fragmentManager;
    private final SideMenuFragment sideMenuFragment;

    ApplicationAdapter(Context context, FragmentManager fragmentManager, SideMenuFragment fragment) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.sideMenuFragment = fragment;
        mViewModel = ViewModelProviders.of((FragmentActivity) context).get(StationsViewModel.class);
    }

    /**
     * Set the adapters primary application list and the filtered application list.
     * @param applications A list of application model objects.
     */
    public void setApplications(ArrayList<Application> applications) {
        applicationList = applications;
    }

    @Override
    public int getCount() {
        return applicationList != null ? applicationList.size() : 0;
    }

    @Override
    public Application getItem(int position) {
        return applicationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String searchTerm = constraint.toString().toLowerCase(Locale.ROOT);

                List<Application> filteredList = ApplicationLibraryFragment.installedApplicationList.stream()
                        .filter(application ->
                                (application.name.toLowerCase(Locale.ROOT).contains(searchTerm)
                                    || containsPartialOrFullMatch(application, searchTerm))
                                && shouldInclude(application))
                        .collect(Collectors.toList());

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                filterResults.count = filteredList.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.values instanceof ArrayList<?>) {
                    applicationList = ((ArrayList<?>) results.values)
                            .stream()
                            .filter(obj -> obj instanceof Application)
                            .map(obj -> (Application) obj)
                            .collect(Collectors.toCollection(ArrayList::new));
                    notifyDataSetChanged(); // Notify adapter about the data change
                }
            }
        };
    }

    /**
     * Determines whether the given application should be included based on the current subject filters.
     * If subject filters are set, the application is included if all of its tags match the filters.
     * If no subject filters are set, the application is included by default.
     *
     * @param application The application to check for inclusion.
     * @return True if the application should be included, false otherwise.
     */
    private boolean shouldInclude(Application application) {
        List<String> subjectFilters = LibraryPageFragment.mViewModel.getSubjectFilters().getValue();
        if (subjectFilters != null && !subjectFilters.isEmpty()) {
            List<String> applicationTags = application.getInformation().getTags();
            return applicationTags.stream().anyMatch(subjectFilters::contains);
        }
        return true; // Include if no subject filters are set
    }

    /**
     * Determines whether the given application should be included in the search results based on
     * the hidden keywords linked to that application.
     *
     * @param application The application to check for inclusion.
     * @param searchString The string of what is being searched by the user.
     * @return True if the application should be included, false otherwise.
     */
    public static boolean containsPartialOrFullMatch(Application application, String searchString) {
        return application.getInformation().getHiddenKeywords().stream().anyMatch(item -> item.toLowerCase().contains(searchString));
    }

    /**
     * ViewHolder class to hold the binding object for each item view.
     */
    private static class ViewHolder {
        final CardApplicationBinding binding;

        ViewHolder(CardApplicationBinding binding) {
            this.binding = binding;
        }
    }

    /**
     * Sets up the view for an item in the ListView.
     *
     * @param position           The position of the item in the ListView.
     * @param convertView        The recycled view to populate.
     * @param parent             The parent ViewGroup.
     * @return                   The populated view for the item.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            CardApplicationBinding binding = CardApplicationBinding.inflate(mInflater, parent, false);
            convertView = binding.getRoot();
            viewHolder = new ViewHolder(binding);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //Set the experience image, tags, and sub tags
        Application currentApplication = getItem(position);
        loadAdditionalInformation(viewHolder, currentApplication);

        TextView applicationName = convertView.findViewById(R.id.application_name);
        applicationName.setText(currentApplication.getName());
        viewHolder.binding.setApplication(currentApplication);


        ImageView info = convertView.findViewById(R.id.application_info);
        info.setOnClickListener(v -> selectApplication(v, currentApplication, true));

        convertView.setOnClickListener(v -> selectApplication(v, currentApplication, false));

        return convertView;
    }

    /**
     * Loads additional information for the given application and updates the UI.
     *
     * @param viewHolder         The ViewHolder object containing the binding for the item view.
     * @param currentApplication The application object containing information to be displayed.
     */
    private void loadAdditionalInformation(ViewHolder viewHolder, Application currentApplication) {
        Helpers.setExperienceImage(currentApplication.type, currentApplication.name, currentApplication.id, viewHolder.binding.getRoot());

        // Set up tags
        LinearLayout tagsContainer = viewHolder.binding.getRoot().findViewById(R.id.tagsContainer);
        TextView subtagsTextView = viewHolder.binding.getRoot().findViewById(R.id.subTags);
        TextView yearLevelTextView = viewHolder.binding.getRoot().findViewById(R.id.yearLevel);
        TagUtils.setupTags(context, tagsContainer, subtagsTextView, yearLevelTextView, currentApplication);
    }

    /**
     * Handles the selection of a game/application.
     *
     * @param view                The View that triggered the selection action.
     * @param currentApplication  The Application object representing the selected game/application.
     * @param showInfo            A boolean of whether to load the information fragment of directly
     *                            launch the application.
     */
    private void selectApplication(View view, Application currentApplication, boolean showInfo) {
        InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        // confirm if it is one of the dodgy apps
        ArrayList<String> gamesWithAdditionalStepsRequired = new ArrayList<>();

        gamesWithAdditionalStepsRequired.add("1165850"); // Il Divino - this requires in app steps

        if (gamesWithAdditionalStepsRequired.contains(currentApplication.id)) {
            BooleanCallbackInterface booleanCallbackInterface = result -> {
                if (result) {
                    completeSelectApplicationAction(currentApplication, showInfo);
                }
            };
            DialogManager.createConfirmationDialog(
                    "Attention",
                    "This game may require additional steps to launch and may not be able to launch automatically.",
                    booleanCallbackInterface,
                    "Go Back",
                    "Continue",
                    false);
        } else {
            completeSelectApplicationAction(currentApplication, showInfo);
        }
    }

    /**
     * Load the next fragment in the application launch cycle based on what was selected on the
     * application card layout and the type of application to be launched.
     *
     * @param currentApplication  The Application object representing the selected game/application.
     * @param showInfo            A boolean of whether to load the information fragment of directly
     *                            launch the application.
     */
    private void completeSelectApplicationAction(Application currentApplication, boolean showInfo) {
        if (LibraryPageFragment.getStationId() > 0) {
            Station station = ApplicationAdapter.mViewModel.getStationById(LibraryPageFragment.getStationId());
            if (station == null) {
                return;
            }

            //Check if application has a shareCode subtype and load the next fragment instead
            if (currentApplication.HasCategory().equals(Constants.ShareCode)) {
                mViewModel.selectSelectedApplication(currentApplication.id);
                mViewModel.setSelectedApplication(currentApplication);
                loadSingleShareCodeFragment();

            //Check if application is of a video type and continue as normal (EXPAND IN FUTURE)
            } else if (currentApplication.HasCategory().equals(Constants.VideoPlayer)) {
                loadApplication(station, currentApplication);

            //Load the information fragment if the Info button was selected
            } else if (showInfo) {
                mViewModel.selectSelectedApplication(currentApplication.id);
                mViewModel.setSelectedApplication(currentApplication);
                loadSingleApplicationFragment();

            //Load the application as normal
            } else {
                loadApplication(station, currentApplication);
            }
        } else {
            mViewModel.selectSelectedApplication(currentApplication.id);
            mViewModel.setSelectedApplication(currentApplication);

            Properties segmentProperties = new Properties();
            segmentProperties.put("classification", LibraryPageFragment.segmentClassification);
            segmentProperties.put("name", currentApplication.getName());
            segmentProperties.put("id", currentApplication.getId());
            segmentProperties.put("type", currentApplication.getType());
            Segment.trackEvent(SegmentConstants.Select_Experience, segmentProperties);

            Bundle args = new Bundle();
            args.putString("selection", "application");
            sideMenuFragment.loadFragment(StationSelectionPageFragment.class, "notMenu", args);
            fragmentManager.beginTransaction()
                    .replace(R.id.rooms, RoomFragment.class, null)
                    .commitNow();
            Segment.trackScreen("stationSelection");
        }
    }

    /**
     * Loads the specified application onto the given station. This method handles the loading of
     * the specified application onto the provided station. It constructs a message containing
     * necessary information about the application launch, and sends it either using a JSON
     * messaging system with fallback or a simpler string-based messaging system, depending on the
     * application's compatibility settings.
     *
     * @param station The station onto which the application is to be loaded.
     * @param currentApplication The application to be loaded.
     */
    private void loadApplication(Station station, Application currentApplication) {
        String joinedStations = Interlinking.joinStations(station, LibraryPageFragment.getStationId(), currentApplication.getName());
        //BACKWARDS COMPATIBILITY - JSON Messaging system with fallback
        if (MainActivity.isNucJsonEnabled) {
            JSONObject message = new JSONObject();
            try {
                message.put("Action", "Launch");
                message.put("ExperienceId", currentApplication.id);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            NetworkService.sendMessage("Station," + joinedStations, "Experience", message.toString());
        }
        else {
            NetworkService.sendMessage("Station," + joinedStations, "Experience", "Launch:" + currentApplication.id);
        }

        Properties segmentProperties = new Properties();
        segmentProperties.put("classification", "General");
        segmentProperties.put("stationId", LibraryPageFragment.getStationId());
        segmentProperties.put("name", currentApplication.getName());
        segmentProperties.put("id", currentApplication.getId());
        segmentProperties.put("type", currentApplication.getType());

        Segment.trackEvent(SegmentConstants.Event_Experience_Launch, segmentProperties);

        sideMenuFragment.loadFragment(DashboardPageFragment.class, "dashboard", null);

        int[] ids = new int[]{station.id};
        if (Interlinking.multiLaunch(currentApplication.getName())) {
            ids = Interlinking.collectNestedStations(station, int[].class);
        }

        DialogManager.awaitStationApplicationLaunch(ids, currentApplication.name, false);
    }

    /**
     * The application has been detected as a Share code subtype. Load in the Share Code fragment
     * for users to input the unique code to be sent to the selected Station.
     */
    private void loadSingleShareCodeFragment() {
        fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                .replace(R.id.main, ApplicationShareCodeFragment.class, null)
                .addToBackStack("menu:dashboard:stationSingle:shareCode")
                .commit();
        Segment.trackScreen("menu:dashboard:stationSingle:shareCode");
    }

    /**
     * The application has been detected as a Share code subtype. Load in the Share Code fragment
     * for users to input the unique code to be sent to the selected Station.
     */
    private void loadSingleApplicationFragment() {
        fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                .replace(R.id.main, ApplicationDetailsFragment.class, null)
                .addToBackStack("menu:dashboard:stationSingle:applicationDetails")
                .commit();
    }
}
