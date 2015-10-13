package com.example.android.sunshine.app;

import com.example.android.sunshine.app.MainActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static org.hamcrest.Matchers.*;
import android.test.suitebuilder.annotation.LargeTest;
import static com.checkdroid.crema.EspressoPlus.*;
import android.support.test.espresso.Espresso;
import static org.hamcrest.core.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BaristaTests {

  @Rule
  public final ActivityTestRule<MainActivity> main = new ActivityTestRule<>(MainActivity.class);

  /**
   * Test for Sunshine
   */
  @Test
  public void SunshineTest1() {
    onView(withText("Today - Sunny - 88/63")).perform(click());
    onView(withText("Today - Sunny - 88/63")).perform(click());
    onView(withId(R.id.msgTextView)).check(matches(isDisplayed()));
  }

}
