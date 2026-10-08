package com.drsherif.ruleofnines

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.activity.compose.setContent
import com.drsherif.ruleofnines.analysis.PixelCoverageAnalyzer
import com.drsherif.ruleofnines.graphics.BitmapTransform
import com.drsherif.ruleofnines.model.BodyView
import com.drsherif.ruleofnines.ui.BurnViewModel
import com.drsherif.ruleofnines.ui.WelcomeScreen
import com.drsherif.ruleofnines.ui.theme.RuleOfNinesTheme
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BurnScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun awaitWelcomeCompletion() {
        rule.waitUntil(5_000) {
            rule.onAllNodesWithTag("bodyCanvas").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun welcomeReusesLauncherLogoAndShowsExactCreditAtBottom() {
        val credit = "Made with ❤️ by Omar El-gazzar and Sol"
        var continued = false
        fun showWelcome() {
            rule.runOnUiThread {
                rule.activity.setContent {
                    RuleOfNinesTheme { WelcomeScreen(onContinue = { continued = true }) }
                }
            }
            rule.waitForIdle()
        }
        fun verifyAndCapture(filename: String) {
            rule.onNodeWithText(credit, useUnmergedTree = true).assertIsDisplayed()
            rule.onNodeWithTag("welcomeLogo", useUnmergedTree = true).assertIsDisplayed()
            val screen = rule.onNodeWithTag("welcomeScreen").fetchSemanticsNode().boundsInRoot
            val footer = rule.onNodeWithTag("creatorCredit", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("Credit is not at the lower edge", footer.center.y > screen.top + screen.height * 0.8f)
            assertTrue("Credit extends beyond the safe content edge", footer.bottom <= screen.bottom + 1f)
            val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.openFileOutput(filename, android.content.Context.MODE_PRIVATE).use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        }
        showWelcome()
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val icon = android.util.TypedValue()
            assertTrue(rule.activity.theme.resolveAttribute(android.R.attr.windowSplashScreenAnimatedIcon, icon, true))
            assertEquals(rule.activity.applicationInfo.icon, icon.resourceId)
        }
        verifyAndCapture("welcome-portrait.png")
        rule.onNodeWithTag("welcomeScreen").performClick()
        assertTrue("Welcome cannot be skipped by tapping", continued)
        rule.runOnUiThread { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        showWelcome()
        verifyAndCapture("welcome-landscape.png")
        rule.runOnUiThread { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        }
    }

    @Test fun launcherIconLoadsAndMatchesBranding() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("BurnMap 9", context.packageManager.getApplicationLabel(context.applicationInfo).toString())
        val icon = context.packageManager.getApplicationIcon(context.applicationInfo)
        // Honor themes can wrap both PackageManager and Resources drawables as bitmaps.
        // Inflate the compiled XML directly to check the packaged adaptive contract.
        val resourceIcon = context.resources.getXml(context.applicationInfo.icon).use { parser ->
            android.graphics.drawable.Drawable.createFromXml(context.resources, parser, context.theme)
        }
        assertTrue("Packaged icon is ${resourceIcon.javaClass.name}",
            resourceIcon is android.graphics.drawable.AdaptiveIconDrawable)
        println("Packaged icon: ${resourceIcon.javaClass.name}; launcher drawable: ${icon.javaClass.name}")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            assertNotNull((resourceIcon as android.graphics.drawable.AdaptiveIconDrawable).monochrome)
        }
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        resourceIcon.setBounds(0, 0, 512, 512)
        resourceIcon.draw(android.graphics.Canvas(bitmap))
        val pixels = IntArray(512 * 512)
        bitmap.getPixels(pixels, 0, 512, 0, 0, 512, 512)
        assertTrue("Icon has no white silhouette", pixels.count { it == Color.WHITE } > 100)
        assertTrue("Icon has no red paint mark", pixels.count { it == Color.rgb(240, 68, 69) } > 10)
        context.openFileOutput("icon-preview.png", android.content.Context.MODE_PRIVATE).use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        // Also render the OEM-facing icon, which may legitimately use different colors.
        bitmap.eraseColor(Color.TRANSPARENT)
        icon.setBounds(0, 0, 512, 512)
        icon.draw(android.graphics.Canvas(bitmap))
        bitmap.getPixels(pixels, 0, 512, 0, 0, 512, 512)
        assertTrue("Launcher icon is empty", pixels.count { Color.alpha(it) > 0 } > 100)
        context.openFileOutput("launcher-icon-preview.png", android.content.Context.MODE_PRIVATE).use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }

    private fun viewModel(): BurnViewModel = rule.runOnIdle {
        ViewModelProvider(rule.activity)[BurnViewModel::class.java]
    }

    private fun tapBody(point: Offset) {
        rule.onNodeWithTag("bodyCanvas").performTouchInput {
            val transform = BitmapTransform.fit(width.toFloat(), height.toFloat())
            click(transform.toViewport(point))
        }
    }

    private fun analyze() {
        rule.onNodeWithTag("analyzeButton").performClick()
        val vm = viewModel()
        rule.waitUntil(10_000) { vm.state.value.totalTbsaPercent != null || vm.state.value.error != null }
        rule.runOnIdle { assertNull(vm.state.value.error) }
    }

    @Test fun emptyAnalysisShowsZeroAndAllEightRegions() {
        analyze()
        rule.onNodeWithTag("totalTbsa").assertTextEquals("0.0%")
        rule.onNodeWithTag("regionResults").performScrollToNode(hasTestTag("region_PERINEUM"))
        rule.onNodeWithTag("region_PERINEUM").assertIsDisplayed()
        assertEquals(8, viewModel().state.value.results.size)
    }

    @Test fun tapAndDragActuallyRasterizeIntoCanonicalCoordinates() {
        tapBody(Offset(256f, 300f))
        val vm = viewModel()
        rule.runOnIdle {
            val snapshot = vm.paintController.snapshot()
            assertEquals(Color.RED, snapshot.bitmaps.getValue(BodyView.FRONT).getPixel(256, 300))
            snapshot.release()
        }
        rule.onNodeWithTag("bodyCanvas").performTouchInput {
            val transform = BitmapTransform.fit(width.toFloat(), height.toFloat())
            swipe(transform.toViewport(Offset(256f, 350f)), transform.toViewport(Offset(256f, 430f)), 400)
        }
        rule.runOnIdle {
            val snapshot = vm.paintController.snapshot()
            assertEquals(Color.RED, snapshot.bitmaps.getValue(BodyView.FRONT).getPixel(256, 400))
            snapshot.release()
        }
        analyze()
        assertTrue(vm.state.value.totalTbsaPercent!! > 0.0)
    }

    @Test fun frontAndBackRemainIndependentAndEditingHidesOldTotal() {
        tapBody(Offset(256f, 300f))
        analyze()
        val vm = viewModel()
        val frontTotal = vm.state.value.totalTbsaPercent!!
        rule.onNodeWithTag("view_BACK").performClick()
        rule.runOnIdle {
            val snapshot = vm.paintController.snapshot()
            assertEquals(0, snapshot.bitmaps.getValue(BodyView.BACK).getPixel(256, 300))
            assertEquals(Color.RED, snapshot.bitmaps.getValue(BodyView.FRONT).getPixel(256, 300))
            snapshot.release()
        }
        tapBody(Offset(256f, 300f))
        rule.onNodeWithTag("outdatedResult").assertIsDisplayed()
        rule.onNodeWithTag("totalTbsa").assertDoesNotExist()
        analyze()
        assertTrue(vm.state.value.totalTbsaPercent!! > frontTotal)
    }

    @Test fun clearRequiresConfirmationAndResetsBothViewsAndResults() {
        tapBody(Offset(256f, 300f))
        rule.onNodeWithTag("view_BACK").performClick()
        tapBody(Offset(256f, 300f))
        analyze()
        rule.onNodeWithTag("clearButton").performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithTag("totalTbsa").assertIsDisplayed()
        rule.onNodeWithTag("clearButton").performClick()
        rule.onNodeWithTag("confirmClear").performClick()
        rule.onNodeWithTag("totalTbsa").assertDoesNotExist()
        val vm = viewModel()
        rule.runOnIdle {
            val snapshot = vm.paintController.snapshot()
            BodyView.entries.forEach { assertEquals(0, snapshot.bitmaps.getValue(it).getPixel(256, 300)) }
            snapshot.release()
        }
        analyze()
        rule.onNodeWithTag("totalTbsa").assertTextEquals("0.0%")
    }

    @Test fun activityRecreationRetainsPaintAndResults() {
        tapBody(Offset(256f, 300f))
        analyze()
        val vm = viewModel()
        val total = vm.state.value.totalTbsaPercent
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        assertSame(vm, viewModel())
        assertEquals(total, viewModel().state.value.totalTbsaPercent)
        rule.runOnIdle {
            val snapshot = vm.paintController.snapshot()
            assertEquals(Color.RED, snapshot.bitmaps.getValue(BodyView.FRONT).getPixel(256, 300))
            snapshot.release()
        }
    }

    @Test fun analysisResultsDoNotRecomposeTheCanvas() {
        tapBody(Offset(256f, 300f))
        rule.waitForIdle()
        val controller = viewModel().paintController
        val before = controller.canvasCompositionCount.get()
        analyze()
        rule.waitForIdle()
        assertEquals("Result updates recomposed the canvas", before, controller.canvasCompositionCount.get())
    }

    @Test fun completeFrontBackAndWholeBodyPassThroughRealRenderingAndViewModel() {
        val vm = viewModel()
        fun fill(view: BodyView) = rule.runOnIdle {
            for (y in 0..1024 step 64) {
                vm.paintController.begin(view, Offset(0f, y.toFloat()), 64f)
                vm.paintController.drag(Offset(512f, y.toFloat()))
                vm.paintController.end()
            }
        }
        fill(BodyView.FRONT)
        analyze()
        assertEquals(50.5, vm.state.value.totalTbsaPercent!!, 0.0)
        fill(BodyView.BACK)
        analyze()
        assertEquals(100.0, vm.state.value.totalTbsaPercent!!, 0.0)
        rule.runOnIdle { vm.clearAll() }
        fill(BodyView.BACK)
        analyze()
        assertEquals(49.5, vm.state.value.totalTbsaPercent!!, 0.0)
    }

    @Test fun backgroundAndResumeRetainSession() {
        tapBody(Offset(256f, 300f))
        analyze()
        val vm = viewModel()
        val total = vm.state.value.totalTbsaPercent
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        assertSame(vm, viewModel())
        assertEquals(total, vm.state.value.totalTbsaPercent)
        rule.onNodeWithTag("totalTbsa").assertIsDisplayed()
    }

    @Test fun portraitAndLandscapeRemainUsableAndProduceVisualEvidence() {
        tapBody(Offset(256f, 300f))
        analyze()
        fun capture(name: String) {
            val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.openFileOutput(name, android.content.Context.MODE_PRIVATE).use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        }
        capture("validation-portrait.png")
        rule.runOnIdle { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        rule.waitForIdle()
        rule.onNodeWithTag("analyzeButton").assertIsDisplayed()
        rule.onNodeWithTag("totalTbsa").assertIsDisplayed()
        val canvasHeightDp = rule.onNodeWithTag("bodyCanvas").fetchSemanticsNode().size.height /
            rule.activity.resources.displayMetrics.density
        assertTrue("Landscape painting area is too small: ${canvasHeightDp}dp", canvasHeightDp >= 160f)
        rule.onNodeWithTag("region_HEAD_NECK").assertIsDisplayed()
        rule.onNodeWithTag("regionResults").performScrollToNode(hasTestTag("region_PERINEUM"))
        rule.onNodeWithTag("region_PERINEUM").assertIsDisplayed()
        rule.onNodeWithTag("regionResults").performScrollToNode(hasTestTag("region_HEAD_NECK"))
        capture("validation-landscape.png")
        rule.runOnIdle { rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        }
    }

    @Test fun editingCancelsAQueuedAnalysisAndPreventsStalePublication() {
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val store = ViewModelStore()
        lateinit var vm: BurnViewModel
        rule.runOnIdle {
            vm = BurnViewModel(PixelCoverageAnalyzer(dispatcher), dispatcher)
            store.put("queued-analysis", vm)
            vm.analyze()
            assertTrue(vm.state.value.isAnalyzing)
            vm.paintController.begin(BodyView.FRONT, Offset(256f, 300f), 24f)
            vm.paintController.end()
        }
        scheduler.runCurrent()
        rule.waitForIdle()
        rule.runOnIdle {
            assertTrue(vm.state.value.isOutdated)
            assertNull(vm.state.value.totalTbsaPercent)
            assertNull(vm.state.value.analyzedRevision)
            store.clear()
        }
    }

    @Test fun invalidAnalysisInputPublishesAnErrorInsteadOfATotal() {
        val store = ViewModelStore()
        lateinit var vm: BurnViewModel
        rule.runOnIdle {
            vm = BurnViewModel()
            store.put("invalid-analysis", vm)
            vm.diagrams.getValue(BodyView.FRONT).hiddenMap.recycle()
            vm.analyze()
        }
        rule.waitUntil(10_000) { !vm.state.value.isAnalyzing }
        rule.runOnIdle {
            assertNotNull(vm.state.value.error)
            assertNull(vm.state.value.totalTbsaPercent)
            store.clear()
        }
    }
}
