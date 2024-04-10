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
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.databinding.CardExperienceBinding;
import com.lumination.leadmelabs.interfaces.BooleanCallbackInterface;
import com.lumination.leadmelabs.managers.DialogManager;
import com.lumination.leadmelabs.models.applications.Application;
import com.lumination.leadmelabs.models.applications.information.TagAdapter;
import com.lumination.leadmelabs.models.applications.information.TagConstants;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.segment.Segment;
import com.lumination.leadmelabs.segment.SegmentConstants;
import com.lumination.leadmelabs.segment.classes.SegmentExperienceEvent;
import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.ui.library.LibrarySelectionFragment;
import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;
import com.lumination.leadmelabs.ui.room.RoomFragment;
import com.lumination.leadmelabs.ui.sidemenu.SideMenuFragment;
import com.lumination.leadmelabs.ui.stations.StationSelectionPageFragment;
import com.lumination.leadmelabs.ui.stations.StationsViewModel;
import com.lumination.leadmelabs.utilities.Constants;
import com.lumination.leadmelabs.utilities.Helpers;
import com.lumination.leadmelabs.utilities.Interlinking;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//TODO implement filterable for easy searching
public class ApplicationAdapter extends BaseAdapter implements Filterable {
    public ArrayList<Application> applicationList = new ArrayList<>();
    private ArrayList<Application> filteredList = new ArrayList<>();

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
        filteredList = applications;
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

    //TODO make this more efficient
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String searchTerm = constraint.toString().toLowerCase(Locale.ROOT);
                filteredList.clear();

                final ArrayList<Application> originalList = ApplicationLibraryFragment.installedApplicationList;

                for (Application application : originalList) {
                    // Apply search filter
                    if (application.name.toLowerCase(Locale.ROOT).contains(searchTerm)) {
                        // Apply tag filter
                        if (shouldInclude(application)) {
                            filteredList.add(application);
                        }
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                filterResults.count = filteredList.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                ArrayList<Application> filteredResults = (ArrayList<Application>) results.values;
                if (filteredResults != null) {
                    applicationList = filteredResults; // Update applicationList with filtered results
                    notifyDataSetChanged(); // Notify adapter about the data change
                }
            }
        };
    }

    private boolean shouldInclude(Application application) {
        ArrayList<String> subjectFilters = LibrarySelectionFragment.mViewModel.getSubjectFilters().getValue();
        if (subjectFilters != null && !subjectFilters.isEmpty()) {
            return application.getInformation().getTags().stream()
                    .anyMatch(subjectFilters::contains);
        }
        return true; // Include if no subject filters are set
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
            CardExperienceBinding binding = CardExperienceBinding.inflate(mInflater, parent, false);
            convertView = binding.getRoot();
            viewHolder = new ViewHolder(binding);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //Set the experience image, tags, and sub tags
        Application currentApplication = getItem(position);
        loadAdditionalInformation(viewHolder, currentApplication);

        viewHolder.binding.setApplication(currentApplication);

        convertView.setOnClickListener(v -> selectGame(v, currentApplication));

        return convertView;
    }

    /**
     * ViewHolder class to hold the binding object for each item view.
     */
    private static class ViewHolder {
        final CardExperienceBinding binding;

        ViewHolder(CardExperienceBinding binding) {
            this.binding = binding;
        }
    }

    /**
     * Handles the selection of a game/application.
     *
     * @param view                The View that triggered the selection action.
     * @param currentApplication  The Application object representing the selected game/application.
     */
    private void selectGame(View view, Application currentApplication) {
        InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        // confirm if it is one of the dodgy apps
        ArrayList<String> gamesWithAdditionalStepsRequired = new ArrayList<>();

        gamesWithAdditionalStepsRequired.add("513490"); // 1943 Berlin Blitz
        gamesWithAdditionalStepsRequired.add("408340"); // Gravity Lab
        gamesWithAdditionalStepsRequired.add("1053760"); // Arkio

        if (gamesWithAdditionalStepsRequired.contains(currentApplication.id)) {
            BooleanCallbackInterface booleanCallbackInterface = result -> {
                if (result) {
                    completeSelectApplicationAction(currentApplication);
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
            completeSelectApplicationAction(currentApplication);
        }
    }

    /**
     * Loads additional information for the given application and updates the UI.
     *
     * @param viewHolder         The ViewHolder object containing the binding for the item view.
     * @param currentApplication The application object containing information to be displayed.
     */
    private void loadAdditionalInformation(ViewHolder viewHolder, Application currentApplication) {
        Helpers.setExperienceImage(currentApplication.type, currentApplication.name, currentApplication.id, viewHolder.binding.getRoot());

        //Setup the tags
        RecyclerView recyclerView = viewHolder.binding.getRoot().findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // Add a default tag if none exist
        List<String> tags = new ArrayList<>(currentApplication.getInformation().getTags());
        if (tags.isEmpty()) {
            tags.add(TagConstants.DEFAULT);
        }
        TagAdapter adapter = new TagAdapter(tags);
        recyclerView.setAdapter(adapter);

        //Setup the sub tags
        TextView textView = viewHolder.binding.getRoot().findViewById(R.id.subTags);
        String subTags = String.join(", ", currentApplication.getInformation().getSubTags());

        if (Helpers.isNullOrEmpty(subTags)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(subTags);
        }
    }

    private void completeSelectApplicationAction(Application currentApplication) {
        if (LibrarySelectionFragment.getStationId() > 0) {
            Station station = ApplicationAdapter.mViewModel.getStationById(LibrarySelectionFragment.getStationId());
            if (station == null) {
                return;
            }

            //Check if application has a shareCode subtype and load the next fragment instead
            if (currentApplication.HasCategory().equals(Constants.ShareCode)) {
                mViewModel.selectSelectedApplication(currentApplication.id);
                mViewModel.setSelectedApplication(currentApplication);
                loadSingleShareCodeFragment();
            }
            //Check if application is of a video type and continue as normal
            else if (currentApplication.HasCategory().equals(Constants.VideoPlayer)) {
                loadApplication(station, currentApplication);
            }
            //load the application details fragment instead
            else {
                mViewModel.selectSelectedApplication(currentApplication.id);
                mViewModel.setSelectedApplication(currentApplication);
                loadSingleApplicationFragment();
            }
        } else {
            mViewModel.selectSelectedApplication(currentApplication.id);
            mViewModel.setSelectedApplication(currentApplication);

            Bundle args = new Bundle();
            args.putString("selection", "application");
            sideMenuFragment.loadFragment(StationSelectionPageFragment.class, "notMenu", args);
            fragmentManager.beginTransaction()
                    .replace(R.id.rooms, RoomFragment.class, null)
                    .commitNow();
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
        String joinedStations = Interlinking.joinStations(station, LibrarySelectionFragment.getStationId(), currentApplication.getName());
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

        // Send data to Segment
        SegmentExperienceEvent event = new SegmentExperienceEvent(
                SegmentConstants.Event_Experience_Launch,
                LibrarySelectionFragment.getStationId(),
                currentApplication.getName(),
                currentApplication.getId(),
                currentApplication.getType());
        Segment.trackAction(event);

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
