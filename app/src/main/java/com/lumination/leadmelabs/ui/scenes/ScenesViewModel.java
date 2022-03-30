package com.lumination.leadmelabs.ui.scenes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumination.leadmelabs.models.Scene;
import com.lumination.leadmelabs.services.NetworkService;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ScenesViewModel extends ViewModel {
    private MutableLiveData<List<Scene>> scenes;

    public LiveData<List<Scene>> getScenes() {
        if (scenes == null) {
            scenes = new MutableLiveData<>();
            loadScenes();
        }
        return scenes;
    }

    public void setScenes(JSONArray scenes) throws JSONException {
        List<Scene> st = new ArrayList<>();

        for (int i = 0; i < scenes.length(); i++) {
            //Will need a way to set the icon
            Scene scene = new Scene(scenes.getJSONObject(i).getString("name"), scenes.getJSONObject(i).getInt("id"), scenes.getJSONObject(i).getInt("value"));
            st.add(scene);
        }

        this.setScenes(st);
    }

    public void setScenes(List<Scene> scenes) {
        this.scenes.setValue(scenes);
    }

    private void loadScenes() {
        NetworkService.sendMessage("NUC","Scenes", "List");
    }
}