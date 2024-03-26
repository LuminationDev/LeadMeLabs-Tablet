package com.lumination.leadmelabs.utilities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Helpers {
    public static ArrayList<Station> cloneStationList(List<Station> stationList) {
        ArrayList<Station> clone = new ArrayList<Station>(stationList.size());
        for (Station station:stationList) {
            clone.add((Station) station.clone());
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
     * Sets the experience image based on the selected application.
     *
     * @param type A string of the experience type.
     * @param name A string of the experience name used to collect the image from memory
     * @param id A string of the id, used mainly for Steam collection
     * @param view The view containing the ImageView for the experience image.
     */
    public static void SetExperienceImage(String type, String name, String id, View view) {
        if (view == null) return;

        //Set the experience image
        String filePath;
        switch(type) {
            case "Custom":
                filePath = CustomApplication.getImageUrl(name);
                break;
            case "Embedded":
                filePath = EmbeddedApplication.getImageUrl(name);
                break;
            case "Steam":
                filePath = SteamApplication.getImageUrl(name, id);
                break;
            case "Vive":
                filePath = ViveApplication.getImageUrl(id);
                break;
            case "Revive":
                filePath = ReviveApplication.getImageUrl(id);
                break;
            default:
                filePath = "";
        }

        //Attempt to load the image url or a default image if nothing is available
        if(Objects.equals(filePath, "")) {
            Glide.with(view).load(R.drawable.default_header).into((ImageView) view.findViewById(R.id.placeholder_image));
        } else {
            Glide.with(view).load(filePath)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            // Error occurred while loading the image, change the imageUrl to the fallback image
                            MainActivity.runOnUI(() -> {
                                Glide.with(view)
                                        .load(R.drawable.default_header)
                                        .into((ImageView) view.findViewById(R.id.placeholder_image));
                            });
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            // Image loaded successfully
                            return false;
                        }
                    })
                    .into((ImageView) view.findViewById(R.id.placeholder_image));
        }
    }

    /**
     * Sets the video thumbnail based on the selected application.
     *
     * @param id A string of the id, used mainly for Steam collection
     * @param view The view containing the ImageView for the experience image.
     */
    public static void SetVideoImage(String id, View view) {
        String filePath = ImageManager.loadLocalImage(id, "video");

        //Attempt to load the image url or a default image if nothing is available
        if(Objects.equals(filePath, "")) {
            Glide.with(view).load(R.drawable.default_header).into((ImageView) view.findViewById(R.id.placeholder_image));
        } else {
            Glide.with(view).load(filePath)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            // Error occurred while loading the image, change the imageUrl to the fallback image
                            MainActivity.runOnUI(() -> {
                                Glide.with(view)
                                        .load(R.drawable.default_header)
                                        .into((ImageView) view.findViewById(R.id.placeholder_image));
                            });
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            // Image loaded successfully
                            return false;
                        }
                    })
                    .into((ImageView) view.findViewById(R.id.placeholder_image));
        }
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
}
