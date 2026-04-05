package com.graytsar.livewallpaper.ui

import android.app.Activity
import android.app.Instrumentation
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.init
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.Intents.release
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.graytsar.livewallpaper.R
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.service.ImageWallpaperService
import com.graytsar.livewallpaper.service.VideoWallpaperService
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf.allOf
import org.junit.jupiter.api.Test

class FragmentPickerIntegrationTest {

    @Test
    fun pickerScreenShowsImageAndVideoButtonsOnLaunch() {
        ActivityScenario.launch(ReibuActivity::class.java).use {
            onView(withId(R.id.buttonImage)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonVideo)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun menuSettingsNavigatesToSettingsScreen() {
        ActivityScenario.launch(ReibuActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
                    .menu
                    .performIdentifierAction(R.id.menuSettings, 0)
            }

            onView(withId(R.id.switchDarkMode)).check(matches(isDisplayed()))
            onView(withId(R.id.autoCompleteImageScaling)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun supportNavigateUpReturnsFromSettingsToPicker() {
        ActivityScenario.launch(ReibuActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
                    .menu
                    .performIdentifierAction(R.id.menuSettings, 0)
            }

            onView(withId(R.id.switchDarkMode)).check(matches(isDisplayed()))

            scenario.onActivity { activity ->
                activity.onSupportNavigateUp()
            }

            onView(withId(R.id.buttonImage)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonVideo)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun invalidImageEventShowsImageErrorSnackbar() {
        ActivityScenario.launch(ReibuActivity::class.java).use { scenario ->
            scenario.withPickerFragment { fragment ->
                fragment.handlePickerEvent(
                    ViewModelPicker.PickerEvent.Error(ViewModelPicker.PickerUiError.InvalidImage)
                )
            }

            onView(withText(R.string.error_image_open)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun launchWallpaperServiceEventStartsImageWallpaperIntent() {
        init()
        try {
            intending(hasAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)).respondWith(
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
            )

            ActivityScenario.launch(ReibuActivity::class.java).use { scenario ->
                scenario.withPickerFragment { fragment ->
                    fragment.handlePickerEvent(
                        ViewModelPicker.PickerEvent.LaunchWallpaperService(WallpaperServiceType.IMAGE)
                    )
                }

                intended(
                    allOf(
                        hasAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER),
                        hasWallpaperComponent(ImageWallpaperService::class.java)
                    )
                )
            }
        } finally {
            release()
        }
    }

    @Test
    fun launchWallpaperServiceEventStartsVideoWallpaperIntent() {
        init()
        try {
            intending(hasAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)).respondWith(
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
            )

            ActivityScenario.launch(ReibuActivity::class.java).use { scenario ->
                scenario.withPickerFragment { fragment ->
                    fragment.handlePickerEvent(
                        ViewModelPicker.PickerEvent.LaunchWallpaperService(WallpaperServiceType.VIDEO)
                    )
                }

                intended(
                    allOf(
                        hasAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER),
                        hasWallpaperComponent(VideoWallpaperService::class.java)
                    )
                )
            }
        } finally {
            release()
        }
    }

    private fun ActivityScenario<ReibuActivity>.withPickerFragment(action: (FragmentPicker) -> Unit) {
        onActivity { activity ->
            val navHostFragment =
                activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val fragment = navHostFragment.childFragmentManager.fragments
                .filterIsInstance<FragmentPicker>()
                .first()
            action(fragment)
        }
    }

    private fun hasWallpaperComponent(serviceClass: Class<*>): Matcher<Intent> =
        object : TypeSafeMatcher<Intent>() {
            override fun describeTo(description: Description) {
                description.appendText("Intent with live wallpaper component ${serviceClass.name}")
            }

            override fun matchesSafely(intent: Intent): Boolean {
                val component = extractWallpaperComponent(intent)
                return component?.className == serviceClass.name
            }
        }

    @Suppress("DEPRECATION")
    private fun extractWallpaperComponent(intent: Intent): ComponentName? {
        return intent.getParcelableExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT)
    }
}





