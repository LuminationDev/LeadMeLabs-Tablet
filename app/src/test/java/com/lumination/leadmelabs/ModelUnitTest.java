package com.lumination.leadmelabs;

import static com.lumination.leadmelabs.TestUtils.getOrAwaitValue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.lumination.leadmelabs.models.Scene;
import com.lumination.leadmelabs.models.Station;
import com.lumination.leadmelabs.models.SteamApplication;

/**
 * Testing the creation and execution of model classes.
 */
public class ModelUnitTest {
    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Test
    public void scene_creation() throws InterruptedException {
        Scene scene = new Scene("Home", 0, "1", new int[0]);

        assertEquals("Home", scene.name);
        assertEquals(0, scene.number);
        assertEquals("1", scene.value);

        assertFalse(getOrAwaitValue(scene.isActive));
        scene.isActive = new MutableLiveData<>(true);
        assertTrue(getOrAwaitValue(scene.isActive));
    }

    @Test
    public void station_creation() {
        String apps = "212680|FTL/231324|Test";

        Station station = new Station("One", apps, 0, "Online", 0, 0, "");

        assertEquals(station.name, "One");
        assertEquals(station.id, 0);
        assertEquals(station.status, "Online");

        //Test added steam applications
        assertEquals(station.steamApplications.size(), 2);
        assertEquals(station.steamApplications.get(0).name, "FTL");
        assertEquals(station.steamApplications.get(0).id, 212680);
        assertEquals(SteamApplication.getImageUrl(station.steamApplications.get(0).id), "https://cdn.cloudflare.steamstatic.com/steam/apps/212680/header.jpg");

        String url = SteamApplication.getImageUrl(station.steamApplications.get(0).id);
        assertEquals(TestUtils.MimicHttpRequest(url), 200);

        String invalid = "xyz";
        assertEquals(TestUtils.MimicHttpRequest("https://cdn.cloudflare.steamstatic.com/steam/apps/"+ invalid +"/header.jpg"), 404);
    }

    @Test
    public void steam_application_creation() {
        SteamApplication steamApp = new SteamApplication("FTL", 212680);

        assertEquals(steamApp.name, "FTL");
        assertEquals(steamApp.id, 212680);
        assertEquals(steamApp.getImageUrl(steamApp.id), "https://cdn.cloudflare.steamstatic.com/steam/apps/212680/header.jpg");
        assertEquals(TestUtils.MimicHttpRequest(steamApp.getImageUrl(steamApp.id)), 200);
    }
}
