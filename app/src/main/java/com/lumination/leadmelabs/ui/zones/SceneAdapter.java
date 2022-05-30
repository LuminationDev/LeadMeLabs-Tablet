package com.lumination.leadmelabs.ui.zones;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.lumination.leadmelabs.databinding.CardSceneBinding;
import com.lumination.leadmelabs.models.Scene;
import com.lumination.leadmelabs.models.Zone;

import java.util.ArrayList;

public class SceneAdapter extends RecyclerView.Adapter {
    private final String TAG = "ScenesAdapter";

    public ArrayList<CardSceneBinding> sceneBindings = new ArrayList<>();

    public ArrayList<Scene> sceneList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Zone zone;
    public boolean animate = true;
    private ZonesViewModel zonesViewModel;

    SceneAdapter(Context context, Zone zone) {
        this.mInflater = LayoutInflater.from(context);
        this.zone = zone;
        zonesViewModel = ViewModelProviders.of((FragmentActivity) context).get(ZonesViewModel.class);
    }

    public class SceneViewHolder extends RecyclerView.ViewHolder {
        private final CardSceneBinding binding;
        public SceneViewHolder(@NonNull CardSceneBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Scene scene, int position) {
            binding.setScene(scene);

            binding.setIsActive(scene.isActive);

            if (animate) {
                Animation slide = new TranslateAnimation(-300 * (position + 1), 0, 0, 0);
                slide.setDuration(1500);
                binding.getRoot().startAnimation(slide);
            }

            binding.getRoot().setOnClickListener(v -> {
                binding.setIsActive(new MutableLiveData<>(true));
                zonesViewModel.setActiveScene(zone.automationGroup, zone.automationId, scene.value, false);
                for(CardSceneBinding sceneBinding : sceneBindings) {
                    if(sceneBinding != binding) {
                        sceneBinding.setIsActive(new MutableLiveData<>(false));
                    }
                }
            });
            sceneBindings.add(binding);
        }
    }

    public Scene getItem(int position) {
        return sceneList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        CardSceneBinding binding = CardSceneBinding.inflate(layoutInflater, parent, false);
        return new SceneViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Scene scene = getItem(position);
        SceneViewHolder sceneViewHolder = (SceneViewHolder) holder;
        sceneViewHolder.bind(scene, position);
    }

    @Override
    public long getItemId(int position) {
        return sceneList.get(position).number;
    }

    @Override
    public int getItemCount() {
        return sceneList != null ? sceneList.size() : 0;
    }
}
