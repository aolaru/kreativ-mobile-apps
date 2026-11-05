package com.kreativ.post;
import android.content.Context;
import androidx.test.core.app.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class EditorControlsTest {
    @Test public void realControlsEditBrandFormatsHistoryAndPersistence()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();c.getSharedPreferences("MainActivity",0).edit().clear().commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("Use Oven Notes")).perform(scrollTo(),click());
            onView(withContentDescription("Headline")).perform(scrollTo(),replaceText("Cinnamon Saturday"),closeSoftKeyboard());
            onView(withContentDescription("Price / offer")).perform(scrollTo(),replaceText("€3.90"),closeSoftKeyboard());
            onView(withText("Save")).perform(scrollTo(),click());
            onView(withText("Brand")).perform(scrollTo(),click());
            onView(withContentDescription("Business name")).perform(scrollTo(),replaceText("Little Oak Bakery"),closeSoftKeyboard());
            onView(withText("Save this brand")).perform(scrollTo(),click());
            onView(withText("Story")).perform(scrollTo(),click());
            scenario.onActivity(a->{assertTrue(a.project.story);assertEquals("Cinnamon Saturday",a.project.headline);assertEquals("Little Oak Bakery",a.project.business);});
            onView(withText("Undo")).perform(scrollTo(),click());scenario.onActivity(a->assertFalse(a.project.story));
            onView(withText("Redo")).perform(scrollTo(),click());scenario.onActivity(a->assertTrue(a.project.story));
            scenario.recreate();scenario.onActivity(a->{assertEquals("€3.90",a.project.price);assertEquals("Little Oak Bakery",a.project.business);assertTrue(a.project.story);});
        }
    }
}
