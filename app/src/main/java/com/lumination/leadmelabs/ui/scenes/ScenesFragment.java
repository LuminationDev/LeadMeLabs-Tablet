package com.lumination.leadmelabs.ui.scenes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.Scene;

import java.util.ArrayList;

public class ScenesFragment extends Fragment {

    public static ScenesViewModel mViewModel;
    private View view;
    private SceneAdapter sceneAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_scenes, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.scene_list);
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        sceneAdapter = new SceneAdapter(getContext(), gridView);
        sceneAdapter.sceneList = new ArrayList<>();
        gridView.setAdapter(sceneAdapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(ScenesViewModel.class);

        mViewModel.getCurrentValue().observe(getViewLifecycleOwner(), selected -> {
            sceneAdapter.selected = selected;
            sceneAdapter.notifyDataSetChanged();
        });

        mViewModel.getScenes().observe(getViewLifecycleOwner(), scenes -> {
            sceneAdapter.sceneList = (ArrayList<Scene>) scenes;
            sceneAdapter.notifyDataSetChanged();
        });
    }
}