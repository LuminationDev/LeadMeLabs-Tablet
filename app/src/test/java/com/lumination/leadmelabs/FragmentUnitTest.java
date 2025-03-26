package com.lumination.leadmelabs;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.lumination.leadmelabs.ui.pages.DashboardPageFragment;

/**
 * Testing the UI elements of fragments that have been created.
 */
// required to access final members on androidx.loader.content.ModernAsyncTask
@Config(instrumentedPackages = {"androidx.loader.content"})
@RunWith(RobolectricTestRunner.class)
public class FragmentUnitTest {
    //Variables
    private ActivityScenario<MainActivity> scenario;

    //Testing area
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void init() {
        scenario = activityScenarioRule.getScenario();
    }

    @Test
    public void test_dashboard_UI() {
        scenario.onActivity(activity -> {
            activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main, DashboardPageFragment.class, null)
                        .commitNow();
        });

        //Can check whether each of the sub-elements have loaded in.
        onView(withId(R.id.logo_fragment)).check(matches((isDisplayed())));
        onView(withId(R.id.side_menu_fragment)).check(matches((isDisplayed())));

        //Does not like empty rectangles - need to populate first
        //onView(withId(R.id.stations_fragment)).check(matches((isDisplayed())));
        //onView(withId(R.id.scenes_fragment)).check(matches((isDisplayed())));
    }
}
