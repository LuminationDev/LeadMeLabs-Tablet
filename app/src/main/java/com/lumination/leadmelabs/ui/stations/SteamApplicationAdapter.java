package com.lumination.leadmelabs.ui.stations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.lumination.leadmelabs.models.SteamApplication;

import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.services.NetworkService;

import java.util.ArrayList;

public class SteamApplicationAdapter extends BaseAdapter {

    private final String TAG = "SteamApplicationAdapter";

    public ArrayList<SteamApplication> steamApplicationList = new ArrayList<>();
    public int stationId = 0;
    private LayoutInflater mInflater;
    private Context context;

    SteamApplicationAdapter(Context context) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return steamApplicationList != null ? steamApplicationList.size() : 0;
    }

    @Override
    public SteamApplication getItem(int position) {
        return steamApplicationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return steamApplicationList.get(position).id;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = mInflater.inflate(R.layout.steam_tile, null);
        }

        SteamApplication steamApplication = getItem(position);
        Glide.with(view).load(steamApplication.getImageUrl()).into((ImageView) view.findViewById(R.id.steam_image));

        view.setOnClickListener(v -> {
            NetworkService.sendMessage("Station," + stationId, "Steam", "Launch:" + steamApplication.id);
        });

        return view;
    }
}