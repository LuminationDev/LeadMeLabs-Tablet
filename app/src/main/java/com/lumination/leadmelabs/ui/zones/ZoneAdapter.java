package com.lumination.leadmelabs.ui.zones;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.R;

import com.lumination.leadmelabs.databinding.CardZoneBinding;
import com.lumination.leadmelabs.databinding.FragmentZonesBinding;
import com.lumination.leadmelabs.models.Zone;

import java.util.ArrayList;

public class ZoneAdapter extends RecyclerView.Adapter {
    private final String TAG = "ZonesAdapter";

    public ArrayList<CardZoneBinding> zoneBindings = new ArrayList<>();

    public ArrayList<Zone> zoneList = new ArrayList<>();
    private FragmentZonesBinding fragmentZonesBinding;

    ZoneAdapter(FragmentZonesBinding fragmentZonesBinding) {
        this.fragmentZonesBinding = fragmentZonesBinding;
    }

    public class ZoneViewHolder extends RecyclerView.ViewHolder {
        private final CardZoneBinding binding;
        public ZoneViewHolder(@NonNull CardZoneBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Zone zone, int position) {
            binding.setZone(zone);
            View finalResult = binding.getRoot();
            finalResult.setOnClickListener(v -> {
                binding.setIsActive(new MutableLiveData<>(true));

                for(CardZoneBinding zoneBinding : zoneBindings) {
                    if(zoneBinding != binding) {
                        zoneBinding.setIsActive(new MutableLiveData<>(false));
                    }
                }

                fragmentZonesBinding.activeCard.setZone(zone);
                SceneAdapter sceneAdapter = new SceneAdapter(finalResult.getContext(), zone);
                sceneAdapter.sceneList = zone.scenes;
                fragmentZonesBinding.sceneList.setAdapter(sceneAdapter);

                Animation slide = new TranslateAnimation((200 * position), 0, 0, 0);
                slide.setDuration(500);
                View view = fragmentZonesBinding.getRoot().findViewById(R.id.active_card);
                view.startAnimation(slide);

                fragmentZonesBinding.selectedZone.setVisibility(View.VISIBLE);
                fragmentZonesBinding.zoneSelection.setVisibility(View.GONE);
            });
            zoneBindings.add(binding);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardZoneBinding binding = CardZoneBinding.inflate(layoutInflater, parent, false);
        return new ZoneAdapter.ZoneViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Zone zone = getItem(position);
        ZoneAdapter.ZoneViewHolder zoneViewHolder = (ZoneAdapter.ZoneViewHolder) holder;
        zoneViewHolder.bind(zone, position);
    }

    @Override
    public int getItemCount() {
        return zoneList != null ? zoneList.size() : 0;
    }

    public Zone getItem(int position) {
        return zoneList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return zoneList.get(position).id;
    }
}
