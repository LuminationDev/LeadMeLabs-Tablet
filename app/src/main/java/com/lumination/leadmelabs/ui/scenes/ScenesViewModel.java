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
    private MutableLiveData<Integer> currentValue;
    private MutableLiveData<Scene> currentScene;
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

    /**
     * Load the last saved scene within a session.
     */
    public LiveData<Scene> getCurrentScene() {
        if (currentScene == null) {
            currentScene = new MutableLiveData<>();
        }

        return currentScene;
    }

    public void setCurrentScene(Scene scene) {
        this.currentScene.setValue(scene);
    }

    /**
     * Loading the current value for the selected scene from a cold start, grabbing the current
     * value from the CBUS system.
     */
    public LiveData<Integer> getCurrentValue() {
        if (currentValue == null) {
            currentValue = new MutableLiveData<>();
            loadSelected();
        }

        return currentValue;
    }

    public void setCurrentValue(Integer value) {
        this.currentValue.setValue(value);
    }

    private void loadSelected() {
        NetworkService.sendMessage("NUC", "Automation", "Get:scenes");
    }
}