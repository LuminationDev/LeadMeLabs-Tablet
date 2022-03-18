package com.lumination.leadmelab.adapters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lumination.leadmelab.utilities.Application;
import com.lumination.leadmelab.managers.ApplicationManager;
import com.lumination.leadmelab.MainActivity;
import com.lumination.leadmelab.R;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApplicationAdapter extends BaseAdapter {
    private final String TAG = "Application Adapter";

    public ArrayList<Application> mData = new ArrayList<>();
    private final LayoutInflater mInflater;
    private final MainActivity main;
    private final ApplicationManager manager;

    /**
     * Thread pool used specifically for getting images for applications
     */
    private final ExecutorService imageExecutor = Executors.newCachedThreadPool();

    public ApplicationAdapter(MainActivity main, ApplicationManager manager, List<Application> data) {
        this.main = main;
        this.manager = manager;
        this.mInflater = LayoutInflater.from(main);

        mData.addAll(data);
    }

    public void refresh() {
        main.runOnUiThread(() -> {
            notifyDataSetChanged();
            super.notifyDataSetChanged();
        });
    }

    public void addApplication(Application application) {
        //does someone with this name already exist?
        boolean found = mData.contains(application);

        Log.d(TAG, String.valueOf(found));

        if (found) {
            Log.w(TAG, "Already have a record for this application!");
            //remove old one so we keep the newest version
            return;
        }

        //add the newest version
        mData.add(application);
        refresh();

        Log.d(TAG, "Adding " + application.getName() + " to application list, ID: " + application.getID() + ". Now: " + mData.size());
    }

    /**
     * Remove an application from the list, occurs on uninstall
     */
    public void removeApplication(Application app) {
        this.mData.remove(app);
        refresh();
    }

    /**
     * Clear the gridView for another station's applications to load.
     */
    public void clearView() {
        this.mData = new ArrayList<>();
        refresh();
    }

    @Override
    public int getCount() {
        if (mData != null) {
            return mData.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = mInflater.inflate(R.layout.row_application, parent, false);

        TextView applicationName = convertView.findViewById(R.id.application_name);
        ImageView applicationIcon = convertView.findViewById(R.id.application_icon);
        ImageView selectedIndicator = convertView.findViewById(R.id.selected_indicator);

        final Application mApplication = mData.get(position);
        if (mApplication != null) {
            applicationName.setText(mApplication.getName());

            if(mApplication.getPicture() == null) {
                imageExecutor.submit(new ImageRetrieval(mApplication, applicationIcon, mApplication.getID()));
            } else {
                applicationIcon.setImageBitmap(mApplication.getPicture());
            }

            manager.showLoading(false);

            convertView.setLongClickable(true);

            convertView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked on " + mApplication.getName() + ", " + mApplication.getID());
                ApplicationManager.selected = mApplication;
                mApplication.setSelected(!mApplication.isSelected());
                selectedIndicator.setVisibility(mApplication.isSelected() ? View.VISIBLE : View.GONE);
            });
        }
        return convertView;
    }

    /**
     * Unselected any previously selected application
     */
    private void resetSelection() {
        for(Application app : mData) {
            app.setSelected(false);
        }
    }

    /**
     * A runnable to download the bitmap of an image from a URL.
     */
    class ImageRetrieval implements Runnable {
        Application app;
        ImageView appImage;
        String appID;
        URL url;
        Bitmap image;

        public ImageRetrieval(Application mApplication, ImageView appImage, String appID) {
            this.app = mApplication;
            this.appImage = appImage;
            this.appID = appID;
        }

        public void run() {
            try {
                url = new URL("https://cdn.cloudflare.steamstatic.com/steam/apps/" + appID + "/header.jpg");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                image = BitmapFactory.decodeStream(url.openStream());
                this.app.setPicture(image);
            } catch (IOException e) {
                e.printStackTrace();
            }

            main.runOnUiThread(() -> appImage.setImageBitmap(image));
        }
    }

    /**
     * Get the screens current width, useful for scaling images in grid views.
     * @return An int representing the absolute width of a device.
     */
    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    /**
     * Get the screens current height, useful for scaling images in grid views.
     * @return An int representing the absolute height of a device.
     */
    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
}
