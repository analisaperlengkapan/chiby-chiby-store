package com.chibychibystore.screenshots

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraph
import androidx.navigation.createGraph
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.components.shared.AppDrawer
import com.chibychibystore.ui.components.shared.drawerNavItems
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.navigation.appDestinations
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the app shell wiring. `AppNavigation` hosts one [AppDrawer] and every
 * top-level screen opens it from its own top bar. Before this was wired up the
 * drawer was never composed, so most of the app (POS, purchases, reports, ...)
 * could not be reached from the UI at all.
 *
 * The real destination graph is built through [appDestinations] so this runs in
 * the unit-test source set without booting Hilt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppNavigationDrawerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun authService(): AuthService = mock()

    private fun navController(): TestNavHostController =
        TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }

    @Test
    fun `every drawer entry resolves to a registered route`() {
        var graph: NavGraph? = null
        composeTestRule.setContent {
            val nav = remember { navController() }
            nav.graph = nav.createGraph(startDestination = Screen.Login.route) {
                appDestinations(
                    navController = nav,
                    authService = authService(),
                    onOpenDrawer = {},
                    navigateToRoute = {}
                )
            }
            graph = nav.graph
        }
        composeTestRule.waitForIdle()

        assertNotNull("nav graph was not built", graph)
        // Sanity check: the graph must actually carry the destinations, otherwise
        // findNode would return null for every entry and mask a broken builder.
        // Sanity check: a known route must resolve, otherwise findNode returning
        // null for every entry would mask a builder that registered nothing.
        assertNotNull(
            "the dashboard destination is missing from the built graph",
            graph!!.findNode(Screen.Dashboard.route)
        )
        drawerNavItems.forEach { item ->
            // findNode returns null when the destination was never registered -
            // the regression where a drawer entry points nowhere and tapping it
            // silently does nothing.
            assertNotNull(
                "Drawer entry '${item.label}' points at unregistered route '${item.route}'",
                graph!!.findNode(item.route)
            )
        }
    }

    @Test
    fun `drawer entry taps navigate to their route`() {
        var navigatedTo: String? = null
        composeTestRule.setContent {
            AppDrawer(
                drawerState = rememberDrawerState(DrawerValue.Open),
                currentRoute = Screen.Dashboard.route,
                onNavigateToRoute = { navigatedTo = it },
                onCloseDrawer = {}
            ) {}
        }

        composeTestRule.onNodeWithText("Manajemen Kas").performClick()
        assertTrue(
            "tapping 'Manajemen Kas' should navigate to ${Screen.CashShift.route}, got $navigatedTo",
            navigatedTo == Screen.CashShift.route
        )
    }

    @Test
    fun `top bar menu action opens the shared drawer`() {
        var opened = 0
        composeTestRule.setContent {
            AppDrawer(
                drawerState = rememberDrawerState(DrawerValue.Closed),
                currentRoute = Screen.Dashboard.route,
                onNavigateToRoute = {},
                onCloseDrawer = {}
            ) {
                MenuProbe(onOpenDrawer = { opened++ })
            }
        }

        composeTestRule.onNodeWithContentDescription("Navigation").performClick()
        composeTestRule.waitForIdle()
        assertTrue("the menu action must open the drawer", opened == 1)
    }

    /**
     * Mirrors what every top-level screen does: a top bar whose navigation icon
     * is the drawer menu. Keeps the assertion independent of a specific screen's
     * ViewModel wiring.
     */
    @Composable
    private fun MenuProbe(onOpenDrawer: () -> Unit) {
        com.chibychibystore.ui.components.ChibyScaffold(
            title = "Dashboard",
            onNavigateUp = onOpenDrawer,
            navigationIcon = Icons.Default.Menu
        ) { PaddingValues() }
    }
}
