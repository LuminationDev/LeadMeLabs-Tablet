package com.lumination.leadmelabs.utilities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.managers.ImageManager;
import com.lumination.leadmelabs.models.applications.CustomApplication;
import com.lumination.leadmelabs.models.applications.EmbeddedApplication;
import com.lumination.leadmelabs.models.applications.ReviveApplication;
import com.lumination.leadmelabs.models.applications.SteamApplication;
import com.lumination.leadmelabs.models.applications.ViveApplication;
import com.lumination.leadmelabs.models.stations.Station;
import com.lumination.leadmelabs.ui.settings.SettingsFragment;
import com.lumination.leadmelabs.ui.stations.StationsFragment;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Helpers {
    public static ArrayList<Station> cloneStationList(List<Station> stationList) {
        ArrayList<Station> clone = new ArrayList<>(stationList.size());
        for (Station station:stationList) {
            clone.add(station.clone());
        }
        return clone;
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public static String concatArray(ArrayList<String> list) {
        return String.join(", ", list);
    }

    /**
     * Convert a density pixel value to regular pixels based on the tablets screen density.
     * Dynamically setting dimensions require pixel units instead of density pixels.
     * @param dp A int representing the density pixels to be converted.
     * @return A int representing the relative pixel size for an individual device.
     */
    public static int convertDpToPx(int dp){
        float scale = MainActivity.getInstance().getResources().getDisplayMetrics().density;
        return (int) ((dp * scale) + 0.5f);
    }

    public static boolean urlIsAvailable(String url){
        try {
            URL urlObject = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
            urlConnection.connect();
            urlConnection.disconnect();
            return true;
        } catch (IOException e) {
            Log.e("QaChecks", "Exception", e);
        }
        return false;
    }

    /**
     * Sets the image for the supplied option name.
     * @param name A string of the name of the option
     * @param imageView An imageView where the image is going to be loaded
     * @param view The parent view containing the imageView.
     */
    public static void SetOptionImage(String name, ImageView imageView, View view) {
        int temp;
        switch (name.trim()) {
            case "Station Grid":
                temp = R.drawable.snowy_layouts_vr_stations_grid;
                break;

            case "VRPC Vertical":
                temp = R.drawable.snowy_layouts_vr_stations_vertical;
                break;

            case "Showcase":
                temp = R.drawable.snowy_layouts_presentation;
                break;

            case "Feature":
                temp = R.drawable.snowy_layouts_feature;
                break;

            case "Fullscreen":
                temp = R.drawable.snowy_layouts_fullscreen;
                break;

            default:
                temp = R.drawable.default_layout;
                break;
        }

        //Load the default image for now
        Glide.with(view).load(temp).into(imageView);
    }

    /**
     * Sets the experience image based on the selected application.
     *
     * @param type A string of the experience type.
     * @param name A string of the experience name used to collect the image from memory
     * @param id A string of the id, used mainly for Steam collection
     * @param view The view containing the ImageView for the experience image.
     */
    public static void setExperienceImage(String type, String name, String id, View view) {
        if (view == null) return;

        // Set the experience image
        String filePath = getImageFilePath(type, name , id);

        ImageView imageView = view.findViewById(R.id.placeholder_image);
        LinearLayout progressBarContainer = view.findViewById(R.id.progress_spinner_container);

        // Show progress spinner while image is loading
        showProgressBar(progressBarContainer, imageView);

        // Load the image using Glide
        if (TextUtils.isEmpty(filePath)) {
            hideProgressBar(progressBarContainer, imageView);

            // Load default placeholder image if filePath is empty
            Glide.with(view)
                    .load(R.drawable.default_header)
                    .into(imageView);
        } else {
            // Load the image from filePath
            Glide.with(view)
                    .load(filePath)
                    .placeholder(R.drawable.default_header) // Set a placeholder image
                    .error(R.drawable.default_header) // Set an error image
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            // Hide progress spinner if loading failed
                            hideProgressBar(progressBarContainer, imageView);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            // Hide progress spinner if image loaded successfully
                            hideProgressBar(progressBarContainer, imageView);
                            return false;
                        }
                    })
                    .into(imageView);
        }
    }

    /**
     * Sets the video thumbnail based on the selected application.
     *
     * @param id A string of the id, used mainly for Steam collection
     * @param view The view containing the ImageView for the experience image.
     */
    public static void setVideoImage(String id, View view) {
        String filePath = ImageManager.loadLocalImage(id, "video");
        ImageView imageView = view.findViewById(R.id.placeholder_image);
        LinearLayout progressBarContainer = view.findViewById(R.id.progress_spinner_container);

        // Show progress spinner while image is loading
        showProgressBar(progressBarContainer, imageView);

        // Load the image using Glide
        if (TextUtils.isEmpty(filePath)) {
            hideProgressBar(progressBarContainer, imageView);

            // Load default placeholder image if filePath is empty
            Glide.with(view)
                    .load(R.drawable.default_header)
                    .into(imageView);
        } else {
            // Load the image from filePath
            Glide.with(view)
                    .load(filePath)
                    .placeholder(R.drawable.default_header) // Set a placeholder image
                    .error(R.drawable.default_header) // Set an error image
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            // Hide progress spinner if loading failed
                            hideProgressBar(progressBarContainer, imageView);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            // Hide progress spinner if image loaded successfully
                            hideProgressBar(progressBarContainer, imageView);
                            return false;
                        }
                    })
                    .into(imageView);
        }
    }

    private static String getImageFilePath(String type, String name, String id) {
        switch (type) {
            case "Custom":
                return CustomApplication.getImageUrl(name);
            case "Embedded":
                return EmbeddedApplication.getImageUrl(name);
            case "Steam":
                return SteamApplication.getImageUrl(name, id);
            case "Vive":
                return ViveApplication.getImageUrl(id);
            case "Revive":
                return ReviveApplication.getImageUrl(id);
            default:
                return "";
        }
    }

    private static void showProgressBar(LinearLayout progressBarContainer, ImageView imageView) {
        progressBarContainer.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
    }

    private static void hideProgressBar(LinearLayout progressBarContainer, ImageView imageView) {
        progressBarContainer.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
    }

    /**
     * Retrieves the version name of the application.
     * If the version name cannot be retrieved, returns "0.0.0".
     *
     * @return The version name of the application.
     */
    public static String getAppVersion() {
        Context context = MainActivity.getInstance();

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0.0.0";
        }
    }

    /**
     * Trigger any observers watching the Stations MutableLive data.
     */
    public static void refreshStationsInplace() {
        List<Station> currentStations = StationsFragment.mViewModel.getStations().getValue();
        StationsFragment.mViewModel.setStations(currentStations);
    }

    /**
     * Collect the current stations from the current room. Double check that the room is within the
     * locked rooms in case 'All' is selected.
     * @return An arraylist of station objects.
     */
    public static ArrayList<Station> getRoomStations()
    {
        ArrayList<Station> checkList = new ArrayList<>();

        List<Station> allStations = StationsFragment.mViewModel.getStations().getValue();
        if (allStations == null) return checkList;

        //Limit to the locked rooms
        for(Station station: allStations) {
            if(SettingsFragment.checkLockedRooms(station.room)) {
                checkList.add(station);
            }
        }

        return checkList;
    }

    /**
     * Check if a supplied time stamp is older than x weeks.
     * @param timestamp A string of a time in the format 'Thu, 23 May 2024 22:56:30 GMT'
     * @param weeks An int representing how many weeks to check for.
     * @return A boolean of it the supplied time stamp is older than the supplied weeks.
     */
    public static boolean isOlderThanXWeeks(String timestamp, int weeks) {
        // Define the format of the input timestamp string
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

        try {
            // Parse the timestamp string into a Date object
            Date parsedDate = dateFormat.parse(timestamp);

            // Subtract two weeks from the current date
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.WEEK_OF_YEAR, -weeks);
            Date twoWeeksAgo = calendar.getTime();

            // An error has occurred, bail out
            if (parsedDate == null) return true;

            // Check if the parsed date is before the date two weeks ago
            return parsedDate.before(twoWeeksAgo);
        } catch (ParseException e) {
            // Handle the exception if the timestamp string is not in the expected format
            Log.e("Helpers", String.format("Error: %s", e.getMessage()));
            return false;
        }
    }
}
