package com.chibychibystore.screenshots

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavGraph
import androidx.navigation.createGraph
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.components.shared.AppDrawer
import com.chibychibystore.ui.components.shared.drawerNavItems
import com.chibychibystore.ui.navigation.AppShell
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.navigation.appDestinations
import com.chibychibystore.ui.navigation.rememberDrawerNavigation
import org.junit.Assert.assertEquals
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

        val builtGraph = graph
        assertNotNull("nav graph was not built", builtGraph)
        val navGraph = builtGraph!!
        // Sanity check: a known route must resolve, otherwise findNode returning
        // null for every entry would mask a builder that registered nothing.
        assertNotNull(
            "the dashboard destination is missing from the built graph",
            navGraph.findNode(Screen.Dashboard.route)
        )
        drawerNavItems.forEach { item ->
            // findNode returns null when the destination was never registered -
            // the regression where a drawer entry points nowhere and tapping it
            // silently does nothing.
            assertNotNull(
                "Drawer entry '${item.label}' points at unregistered route '${item.route}'",
                navGraph.findNode(item.route)
            )
        }
    }

    /**
     * The drawer is the only production entry point to the modules after login,
     * so a top-level screen without an entry is unreachable. Regression: the
     * menu omitted Inventory, Reports, Settings, Sales history, Suppliers and
     * Dashboard, which also hid the only logout action (it lives on Settings).
     */
    @Test
    fun `every intended top-level screen has a drawer entry`() {
        val intendedTopLevelRoutes = listOf(
            Screen.Dashboard,
            Screen.Pos,
            Screen.Inventory,
            Screen.SalesHistory,
            Screen.PurchaseList,
            Screen.WarehouseList,
            Screen.ExpenseList,
            Screen.UserList,
            Screen.PromotionList,
            Screen.PelangganList,
            Screen.CashShift,
            Screen.CashHistory,
            Screen.AuditList,
            Screen.SupplierList,
            Screen.Reports,
            Screen.BarcodeScanner,
            Screen.BarcodePrint,
            Screen.Backup,
            Screen.Settings
        ).map { it.route }

        val drawerRoutes = drawerNavItems.map { it.route }.toSet()
        intendedTopLevelRoutes.forEach { route ->
            assertTrue(
                "no drawer entry reaches top-level screen '$route'",
                route in drawerRoutes
            )
        }
    }

    @Test
    fun `drawer labels use the localized module names`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val byRoute = drawerNavItems.associateBy { it.route }
        // AGENTS.md requires Indonesian UI strings; these two entries were English
        // ("Dashboard"/"Inventory") while strings.xml already had the wording.
        assertEquals(
            context.getString(com.chibychibystore.R.string.dashboard),
            byRoute[Screen.Dashboard.route]?.label
        )
        assertEquals(
            context.getString(com.chibychibystore.R.string.inventory),
            byRoute[Screen.Inventory.route]?.label
        )
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

        // "Pengaturan" is the last entry and the only route that exposes logout,
        // so it also proves the list scrolls far enough to reach the tail.
        composeTestRule.onNodeWithText("Pengaturan").performScrollTo().performClick()
        assertTrue(
            "tapping 'Pengaturan' should navigate to ${Screen.Settings.route}, got $navigatedTo",
            navigatedTo == Screen.Settings.route
        )
    }

    @Test
    fun `top bar menu action opens the shared drawer`() {
        lateinit var drawerState: DrawerState
        composeTestRule.setContent {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            AppDrawer(
                drawerState = drawerState,
                currentRoute = Screen.Dashboard.route,
                onNavigateToRoute = {},
                onCloseDrawer = {}
            ) {
                // Production screens get exactly this callback from AppNavigation.
                MenuProbe(onOpenDrawer = rememberDrawerNavigation(drawerState))
            }
        }

        composeTestRule.onNodeWithContentDescription("Navigation").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            drawerState.currentValue == DrawerValue.Open
        }
        // The drawer sheet only occupies the screen once the state is Open.
        composeTestRule.onNodeWithText("FITUR LANJUTAN").assertIsDisplayed()
    }

    @Test
    fun `drawer is not mounted before login`() {
        composeTestRule.setContent {
            AppShell(
                isLoggedIn = false,
                currentRoute = Screen.Login.route,
                drawerState = rememberDrawerState(DrawerValue.Closed),
                onNavigateToRoute = {},
                onCloseDrawer = {}
            ) {
                Text("login-content")
            }
        }

        composeTestRule.onNodeWithText("login-content").assertIsDisplayed()
        // Regression: the drawer used to wrap the whole NavHost, so it could be
        // swiped open over the login screen to reach a protected module.
        composeTestRule.onNodeWithText("FITUR LANJUTAN").assertDoesNotExist()
    }

    /**
     * Regression: a session that is still sitting on the login destination (after
     * logout, or a restored back stack) must not expose the menu either. Gating on
     * user state alone left the modules reachable from the login form.
     */
    @Test
    fun `drawer is not mounted when a signed-in session sits on login`() {
        composeTestRule.setContent {
            AppShell(
                isLoggedIn = true,
                currentRoute = Screen.Login.route,
                drawerState = rememberDrawerState(DrawerValue.Closed),
                onNavigateToRoute = {},
                onCloseDrawer = {}
            ) {
                Text("login-content")
            }
        }

        composeTestRule.onNodeWithText("login-content").assertIsDisplayed()
        composeTestRule.onNodeWithText("FITUR LANJUTAN").assertDoesNotExist()
    }

    @Test
    fun `drawer is mounted after login`() {
        composeTestRule.setContent {
            AppShell(
                isLoggedIn = true,
                currentRoute = Screen.Dashboard.route,
                drawerState = rememberDrawerState(DrawerValue.Closed),
                onNavigateToRoute = {},
                onCloseDrawer = {}
            ) {
                Text("dashboard-content")
            }
        }

        composeTestRule.onNodeWithText("dashboard-content").assertIsDisplayed()
        // The drawer sheet is composed once a session exists (it is closed by
        // default, but its content is part of the tree).
        composeTestRule.onNodeWithText("FITUR LANJUTAN").assertExists()
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
