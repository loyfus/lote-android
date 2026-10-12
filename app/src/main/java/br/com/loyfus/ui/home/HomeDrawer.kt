package br.com.loyfus.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.loyfus.core.categoryShortLabel
import br.com.loyfus.data.Category
import br.com.loyfus.ui.components.categoryIcon
import br.com.loyfus.ui.components.Press
import br.com.loyfus.ui.components.pressClickable
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Ink80
import br.com.loyfus.ui.theme.Line
import br.com.loyfus.ui.theme.Paper
import br.com.loyfus.ui.theme.Space
import br.com.loyfus.ui.theme.Touch
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.AmberText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The menu behind the hamburger: where the app goes, and every category by name.
 *
 * The horizontal strip on the hero is a shortcut, not a list — it cuts "Movimentação"
 * in half and hides the thirteen categories that do not fit. Here the same data is a
 * column of whole words, which is what a menu is for. The sheet is the platform's
 * (a modal drawer on Android, a sheet on iOS); the content is identical in both.
 */
@Composable
fun HomeMenu(
    drawerState: DrawerState,
    categories: List<Category>,
    currentTab: HomeTab,
    savedCount: Int,
    scope: CoroutineScope,
    onSelectTab: (HomeTab) -> Unit,
    onSelectCategory: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val close: () -> Unit = {
        scope.launch { drawerState.close() }
        Unit
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Paper,
                drawerContentColor = Ink,
                drawerShape = RoundedCornerShape(0.dp),
            ) {
                MenuContent(
                    categories = categories,
                    currentTab = currentTab,
                    savedCount = savedCount,
                    onSelectTab = { tab ->
                        close()
                        onSelectTab(tab)
                    },
                    onSelectCategory = { id ->
                        close()
                        onSelectCategory(id)
                    },
                )
            }
        },
    ) {
        content()
    }
}

/** The three destinations and the categories, in one scrollable column. */
@Composable
private fun MenuContent(
    categories: List<Category>,
    currentTab: HomeTab,
    savedCount: Int,
    onSelectTab: (HomeTab) -> Unit,
    onSelectCategory: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = Space.md),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Touch.target)
                .padding(start = Space.sm, end = Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wordmark(color = Ink)
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(top = Space.md, bottom = Space.x4),
        ) {
            item {
                MenuRow(
                    title = "Início",
                    icon = Icons.Outlined.Home,
                    selectedIcon = Icons.Filled.Home,
                    selected = currentTab == HomeTab.Home,
                    onClick = { onSelectTab(HomeTab.Home) },
                )
            }
            item {
                MenuRow(
                    title = "Explorar",
                    icon = Icons.Outlined.Search,
                    selectedIcon = Icons.Filled.Search,
                    selected = currentTab == HomeTab.Explore,
                    onClick = { onSelectTab(HomeTab.Explore) },
                )
            }
            item {
                MenuRow(
                    title = if (savedCount > 0) "Salvos ($savedCount)" else "Salvos",
                    icon = Icons.Outlined.FavoriteBorder,
                    selectedIcon = Icons.Filled.Favorite,
                    selected = currentTab == HomeTab.Saved,
                    onClick = { onSelectTab(HomeTab.Saved) },
                )
            }
            item {
                Box(Modifier.fillMaxWidth().padding(top = Space.md, bottom = Space.sm).height(1.dp).background(Line))
                Text(
                    text = "Categorias",
                    style = Type.caption,
                    color = Ink60,
                    modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs),
                )
            }
            items(categories, key = { it.externalId }) { category ->
                val label = categoryShortLabel(category.name)
                MenuRow(
                    title = label,
                    icon = categoryIcon(category.name),
                    selectedIcon = categoryIcon(category.name),
                    selected = false,
                    onClick = { onSelectCategory(category.externalId) },
                )
            }
        }
    }
}

/** One destination: a 44dp row, the icon at 22, the name at the label size. */
@Composable
private fun MenuRow(
    title: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Touch.target)
            .clip(Corners.field)
            .pressClickable(onClick = onClick, press = Press.Surface)
            .padding(horizontal = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = null,
            tint = if (selected) AmberText else Ink80,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            style = Type.body,
            color = if (selected) Ink else Ink80,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** The three destinations the menu offers. Mirrors the bottom navigation bar. */
enum class HomeTab { Home, Explore, Saved }
