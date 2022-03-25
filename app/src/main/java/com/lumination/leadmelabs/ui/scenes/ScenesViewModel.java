package com.lumination.leadmelabs.ui.scenes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Station;

import java.util.List;

public class ScenesViewModel extends ViewModel {
    private MutableLiveData<List<String>> scenes;

    public LiveData<List<String>> getScenes() {
        if (scenes == null) {
            scenes = new MutableLiveData<>();
            loadScenes();
        }
        return scenes;
    }

    private void loadScenes() {
        // Do an asynchronous operation to fetch saved scenes from NUC.
    }
}