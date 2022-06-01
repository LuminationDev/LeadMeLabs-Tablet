package com.lumination.leadmelabs;

import static com.lumination.leadmelabs.TestUtils.getOrAwaitValue;
import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.lumination.leadmelabs.ui.logo.LogoViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(instrumentedPackages = {"androidx.loader.content"})
@RunWith(RobolectricTestRunner.class)
public class ViewModelUnitTest {
    //Variables
    private ViewModelStoreOwner owner;

    //Testing area
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Before
    public void init() {
        ActivityScenario<MainActivity> scenario = activityScenarioRule.getScenario();
        scenario.onActivity(activity -> owner = activity);
    }

    @Test
    public void test_logo() throws InterruptedException {
        LogoViewModel viewModel = new ViewModelProvider(owner).get(LogoViewModel.class);

        assertNull(getOrAwaitValue(viewModel.getInfo()));
    }

    //Haven't look at mimicking/mocking the calls within the model yet
//    @Test
//    public void test_nuc() throws InterruptedException {
//        NucViewModel viewModel = new ViewModelProvider(owner).get(NucViewModel.class);
//
//        assertNull(getOrAwaitValue(viewModel.getNuc()));
//    }
}
