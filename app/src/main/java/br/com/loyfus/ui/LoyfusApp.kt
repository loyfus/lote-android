package br.com.loyfus.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import br.com.loyfus.data.HomeBannerStore
import br.com.loyfus.data.LotQuery
import br.com.loyfus.data.LoyfusRepository
import br.com.loyfus.data.RecentSearchesStore
import br.com.loyfus.data.ShelfQuery
import br.com.loyfus.data.ThemeMode
import br.com.loyfus.ui.components.AnimatedBadge
import br.com.loyfus.ui.detail.DetailScreen
import br.com.loyfus.ui.detail.DetailViewModel
import br.com.loyfus.ui.explore.ExploreScreen
import br.com.loyfus.ui.explore.ExploreViewModel
import br.com.loyfus.ui.favourites.FavouritesScreen
import br.com.loyfus.ui.favourites.FavouritesViewModel
import br.com.loyfus.ui.home.HomeScreen
import br.com.loyfus.ui.home.HomeTab
import br.com.loyfus.ui.home.HomeViewModel
import br.com.loyfus.ui.settings.SettingsScreen
import br.com.loyfus.ui.theme.Amber
import br.com.loyfus.ui.theme.AmberDeep
import br.com.loyfus.ui.theme.AmberSoft
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Line
import br.com.loyfus.ui.theme.LocalReducedMotion
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.OnAmber
import br.com.loyfus.ui.theme.Paper
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.sized

private class Tab(val route: String, val label: String, val selected: ImageVector, val idle: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Início", Icons.Filled.Home, Icons.Outlined.Home),
    Tab(Routes.EXPLORE, "Explorar", Icons.Filled.Search, Icons.Outlined.Search),
    Tab(Routes.FAVOURITES, "Salvos", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
)

/**
 * The app: three tabs on a native navigation bar, and the two screens one level down
 * that hide it: the lot and the settings.
 *
 * What has to outlive a screen is held here, above the navigation graph: Explore (so a
 * trip to a lot or another tab returns to the same results) and the saved lots (so a
 * heart tapped anywhere is filled everywhere and counted on its tab).
 */
@Composable
fun LoyfusApp(
    repository: LoyfusRepository,
    recentSearches: RecentSearchesStore,
    homeBanner: HomeBannerStore,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    // A link opened while the app is already in front arrives as a new intent on the
    // running activity: the navigation graph only reads the intent that started it, so
    // without this the link would be delivered and ignored.
    val activity = LocalContext.current.findActivity() as? ComponentActivity
    DisposableEffect(navController, activity) {
        val listener = Consumer<Intent> { navController.handleDeepLink(it) }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    val favourites: FavouritesViewModel = viewModel(factory = FavouritesViewModel.Factory(repository))
    val explore: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory(repository, recentSearches))
    val saved by favourites.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // A heart the server refused has already gone back; this says why, wherever the
    // reader is. The snackbar is a live region, so TalkBack reads it out too.
    LaunchedEffect(favourites) {
        favourites.refusals.collect { saving ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(
                message = if (saving) "Não foi possível salvar o lote. Tente de novo." else "Não foi possível remover o lote dos salvos. Tente de novo.",
                duration = SnackbarDuration.Short,
            )
        }
    }
    // The notice is dismissed once and for all, so the flag outlives the screen.
    var bannerDismissed by remember { mutableStateOf(homeBanner.isDismissed()) }

    Scaffold(
        containerColor = Paper,
        // Each screen insets itself: Home, Explore and Saved under the status bar, the
        // lot under none at all, so the scaffold hands over only the height of the bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbar) { data -> Snackbar(data, shape = Corners.control) }
        },
        bottomBar = {
            // Leaving for a lot, the bar goes at once, so the lot is laid out at its full
            // height from the first frame and nothing in it moves later. Coming back, the
            // bar takes its space at once too but fades in with the screen it belongs to,
            // instead of landing on top of the lot that is still on its way out.
            AnimatedVisibility(
                visible = route !in BELOW_TABS,
                enter = fadeIn(Motion.enter(Motion.SCREEN)),
                exit = ExitTransition.None,
            ) {
                AppNavigationBar(
                    route = route,
                    savedCount = saved.ids.size,
                    onSelect = navController::navigateToTab,
                )
            }
        },
    ) { padding ->
        val openLot: (Long) -> Unit = { navController.navigate(Routes.detail(it)) }
        val reducedMotion = LocalReducedMotion.current
        // The tabs end where the bar begins: nothing scrolls beneath it, so the bar is
        // plain paper over plain paper and never shows a ghost of the list. The padding is
        // the tabs' own, not the host's: the lot never has the bar, and if the host shrank
        // when the bar came back, the lot on its way out would jump up by the bar's height.
        val barPadding = padding.calculateBottomPadding()

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
            // Two kinds of move. Between tabs nothing is deeper than anything else, so the
            // new tab only fades in over the old one. Opening a lot or the settings goes one
            // level down: it comes in from the trailing edge by a tenth of the width while the list
            // drifts back a little, and Back (including the predictive gesture, which
            // scrubs these same transitions) runs it in reverse. With reduced motion every
            // move is the fade alone.
            enterTransition = {
                if (opensBelow()) pushIn(reducedMotion) else fadeIn(Motion.enter())
            },
            exitTransition = {
                if (opensBelow()) pushOut(reducedMotion) else fadeOut(Motion.exit())
            },
            popEnterTransition = {
                if (closesBelow()) popIn(reducedMotion) else fadeIn(Motion.enter())
            },
            popExitTransition = {
                if (closesBelow()) popOut(reducedMotion) else fadeOut(Motion.exit())
            },
        ) {
            composable(Routes.HOME) {
                val home: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
                TabContent(barPadding) {
                    HomeScreen(
                        viewModel = home,
                        favouriteIds = saved.ids,
                        savedLots = saved.items,
                        bannerDismissed = bannerDismissed,
                        onDismissBanner = {
                            homeBanner.dismiss()
                            bannerDismissed = true
                        },
                        onSetFavourite = favourites::set,
                        onOpenLot = openLot,
                        onOpenSearch = {
                            explore.requestSearchFocus()
                            navController.navigateToTab(Routes.EXPLORE)
                        },
                        onOpenShelf = { query ->
                            explore.openWith(query)
                            navController.navigateToTab(Routes.EXPLORE)
                        },
                        onOpenCategory = { id ->
                            explore.openWith(ShelfQuery(category = id))
                            navController.navigateToTab(Routes.EXPLORE)
                        },
                        onOpenAllCategories = {
                            explore.openWith(ShelfQuery())
                            navController.navigateToTab(Routes.EXPLORE)
                        },
                        onOpenBudget = { min, max ->
                            explore.openWithBudget(min, max)
                            navController.navigateToTab(Routes.EXPLORE)
                        },
                        onOpenSaved = { navController.navigateToTab(Routes.FAVOURITES) },
                        onOpenTab = { tab ->
                            when (tab) {
                                HomeTab.Home -> navController.navigateToTab(Routes.HOME)
                                HomeTab.Explore -> navController.navigateToTab(Routes.EXPLORE)
                                HomeTab.Saved -> navController.navigateToTab(Routes.FAVOURITES)
                            }
                        },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    )
                }
            }
            composable(
                route = Routes.EXPLORE,
                // loyfus://explore is the other half of the contract the iOS client
                // honours. It belongs to this destination and not to the lot one: a deep
                // link carrying no argument cannot open a destination that requires one.
                deepLinks = listOf(navDeepLink { uriPattern = "loyfus://explore" }),
            ) {
                TabContent(barPadding) {
                    ExploreScreen(
                        viewModel = explore,
                        favouriteIds = saved.ids,
                        onSetFavourite = favourites::set,
                        onOpenLot = openLot,
                    )
                }
            }
            composable(Routes.FAVOURITES) {
                TabContent(barPadding) {
                    FavouritesScreen(
                        viewModel = favourites,
                        snackbarHost = snackbar,
                        onOpenLot = openLot,
                        onExplore = { navController.navigateToTab(Routes.EXPLORE) },
                    )
                }
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("lotId") { type = NavType.LongType }),
                deepLinks = listOf(navDeepLink { uriPattern = "loyfus://lot/{lotId}" }),
            ) { entry ->
                val lotId = entry.arguments?.getLong("lotId") ?: 0L
                val detail: DetailViewModel = viewModel(factory = DetailViewModel.Factory(repository, lotId))
                DetailScreen(
                    viewModel = detail,
                    favouriteIds = saved.ids,
                    favouritesLoaded = saved.loaded,
                    onSetFavourite = favourites::set,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                // Settings opens from Home only, and Home is the start destination, so its
                // entry is always under this one: "Como funciona" reads the same catalogue
                // figures Home shows instead of asking the server again.
                val homeEntry = remember(it) { navController.getBackStackEntry(Routes.HOME) }
                val home: HomeViewModel = viewModel(viewModelStoreOwner = homeEntry, factory = HomeViewModel.Factory(repository))
                val homeState by home.state.collectAsStateWithLifecycle()
                val exploreState by explore.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    theme = theme,
                    onThemeChange = onThemeChange,
                    // Through Explore, not the store: the list Explore shows has to empty too.
                    recentSearchCount = exploreState.recents.size,
                    onClearSearches = explore::clearRecents,
                    totalLots = homeState.totalLots,
                    updatedAt = homeState.updatedAt,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/**
 * A tab switch that keeps each tab as it was left: the back stack of the others is
 * saved and restored, and the same tab tapped again is not stacked on itself.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The native Material 3 navigation bar, in the app's colours: the selected tab is a
 * soft amber pill with a deep amber icon and an ink label, the others grey; the
 * ground is the paper at 94% over a hairline. The Saved tab carries the count of saved lots.
 */
@Composable
private fun AppNavigationBar(route: String?, savedCount: Int, onSelect: (String) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        NavigationBar(
            containerColor = Paper.copy(alpha = 0.94f),
            contentColor = Ink,
            tonalElevation = 0.dp,
        ) {
            TABS.forEach { tab ->
                val selected = route == tab.route
                val spoken = if (tab.route == Routes.FAVOURITES && savedCount > 0) {
                    "${tab.label}, $savedCount ${if (savedCount == 1) "lote" else "lotes"}"
                } else {
                    tab.label
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab.route) },
                    modifier = Modifier.semantics { contentDescription = spoken },
                    icon = {
                        BadgedBox(
                            badge = { if (tab.route == Routes.FAVOURITES) AnimatedBadge(savedCount) { CountBadge(it) } },
                        ) {
                            Icon(if (selected) tab.selected else tab.idle, contentDescription = null)
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = Type.caption,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmberDeep,
                        selectedTextColor = Ink,
                        indicatorColor = AmberSoft,
                        unselectedIconColor = Ink60,
                        unselectedTextColor = Ink60,
                    ),
                )
            }
        }
    }
}

/** The count on the Saved tab: amber with its dark label, 18dp at least, 11sp bold. */
@Composable
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(Amber)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = Type.caption.sized(11.0, 14.0, FontWeight.Bold),
            color = OnAmber,
        )
    }
}

// The screens one level down, which hide the navigation bar and come in from the side;
// every other move is between tabs.
private val BELOW_TABS = setOf(Routes.DETAIL, Routes.SETTINGS)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.opensBelow(): Boolean =
    targetState.destination.route in BELOW_TABS

private fun AnimatedContentTransitionScope<NavBackStackEntry>.closesBelow(): Boolean =
    initialState.destination.route in BELOW_TABS

private fun pushIn(reduced: Boolean): EnterTransition {
    val fade = fadeIn(Motion.enter(Motion.SCREEN))
    return if (reduced) fade else fade + slideInHorizontally(Motion.enter(Motion.SCREEN)) { it / 10 }
}

private fun pushOut(reduced: Boolean): ExitTransition {
    val fade = fadeOut(Motion.exit(Motion.SCREEN))
    return if (reduced) fade else fade + slideOutHorizontally(Motion.exit(Motion.SCREEN)) { -it / 20 }
}

private fun popIn(reduced: Boolean): EnterTransition {
    val fade = fadeIn(Motion.enter(Motion.SCREEN))
    return if (reduced) fade else fade + slideInHorizontally(Motion.enter(Motion.SCREEN)) { -it / 20 }
}

private fun popOut(reduced: Boolean): ExitTransition {
    val fade = fadeOut(Motion.exit(Motion.SCREEN))
    return if (reduced) fade else fade + slideOutHorizontally(Motion.exit(Motion.SCREEN)) { it / 10 }
}

/** A tab's screen, ending where the navigation bar begins. */
@Composable
private fun TabContent(barPadding: Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(bottom = barPadding)) { content() }
}
