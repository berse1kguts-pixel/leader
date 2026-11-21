package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LatLngPoint
import com.example.data.local.RouteHistoryEntity
import com.example.data.local.SavedPlaceEntity
import com.example.data.remote.CalculatedRoute
import com.example.data.remote.NavEngineType
import com.example.data.remote.TransitPlanOption
import com.example.data.remote.TransitStationInfo
import com.example.data.remote.TravelMode
import com.example.ui.NavigationUiState
import com.example.ui.NavigationViewModel
import com.example.ui.SheetDetent
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosCardLight
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosSystemGray
import com.example.ui.theme.IosSystemGray2
import com.example.ui.theme.IosSystemGray4
import com.example.ui.theme.IosSystemGray5
import com.example.ui.theme.IosSystemGray6
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class QuickCategoryItem(
    val title: String,
    val keyword: String,
    val icon: ImageVector,
    val tint: Color
)

private val nearbyCategories = listOf(
    QuickCategoryItem("地铁站", "地铁站", Icons.Default.DirectionsSubway, IosPurple),
    QuickCategoryItem("加油站", "加油站", Icons.Default.LocalGasStation, IosBlue),
    QuickCategoryItem("充电桩", "充电站", Icons.Default.EvStation, IosGreen),
    QuickCategoryItem("停车场", "停车场", Icons.Default.LocalParking, IosIndigo),
    QuickCategoryItem("餐饮美食", "餐厅", Icons.Default.Restaurant, IosOrange),
    QuickCategoryItem("咖啡茶饮", "咖啡", Icons.Default.LocalCafe, Color(0xFFA2845E))
)

/**
 * ==================== 1. iOS 18 三段式阻尼跟手探索抽屉 (Explore Sheet) ====================
 */
@Composable
fun IosExploreBottomSheet(
    uiState: NavigationUiState,
    savedPlaces: List<SavedPlaceEntity>,
    recentRoutes: List<RouteHistoryEntity>,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    navViewModel: NavigationViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    var liveDragPx by remember { mutableFloatStateOf(0f) }

    val baseTargetHeight = when (uiState.sheetDetent) {
        SheetDetent.COLLAPSED -> 108.dp
        SheetDetent.HALF -> 284.dp
        SheetDetent.EXPANDED -> 610.dp
    }
    val dragOffsetDp = with(density) { (-liveDragPx).toDp() }
    val animatedSheetHeight by animateDpAsState(
        targetValue = (baseTargetHeight + dragOffsetDp).coerceIn(96.dp, 645.dp),
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iosSheetHeight"
    )

    val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

    Surface(
        color = Color(0xFFF2F2F7).copy(alpha = 0.96f),
        shape = sheetShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedSheetHeight)
            .shadow(
                elevation = 24.dp,
                shape = sheetShape,
                ambientColor = Color.Black.copy(alpha = 0.16f)
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.92f),
                shape = sheetShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
        ) {
            // 顶部拖拽手势热区 + iOS Capsule Grabber
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(uiState.sheetDetent) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                when {
                                    liveDragPx < -90f -> {
                                        val next = if (uiState.sheetDetent == SheetDetent.COLLAPSED) {
                                            SheetDetent.HALF
                                        } else {
                                            SheetDetent.EXPANDED
                                        }
                                        navViewModel.setSheetDetent(next)
                                    }
                                    liveDragPx > 90f -> {
                                        val next = if (uiState.sheetDetent == SheetDetent.EXPANDED) {
                                            SheetDetent.HALF
                                        } else {
                                            SheetDetent.COLLAPSED
                                        }
                                        navViewModel.setSheetDetent(next)
                                    }
                                }
                                liveDragPx = 0f
                            },
                            onDragCancel = { liveDragPx = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                liveDragPx = (liveDragPx + dragAmount).coerceIn(-320f, 320f)
                            }
                        )
                    }
                    .clickable { navViewModel.cycleSheetDetent() }
                    .padding(top = 8.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .background(Color(0x4D3C3C43), CircleShape)
                )
            }

            // iOS 18 搜索栏 + 右侧圆形账号/引擎圆章
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0x1F767680),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = Color(0x993C3C43),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "搜索地图",
                                    color = Color(0x993C3C43),
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { navViewModel.onSearchQueryChange(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color(0xFF1C1C1E),
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.3).sp
                                ),
                                cursorBrush = SolidColor(IosBlue),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        navViewModel.performSearch()
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("destination_search_input")
                            )
                        }
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    navViewModel.performSearch()
                                },
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("search_submit_button")
                            ) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = "执行搜索",
                                    tint = IosBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { navViewModel.dismissSearchSheet() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "清空",
                                    tint = IosSystemGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "快捷搜索北京南站",
                                tint = IosSystemGray,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        navViewModel.searchByCategory("北京南站")
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 右侧纯正 iOS 18 轻透圆形引擎图标（点击查看双引擎与数据源设置）
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x1F767680))
                        .clickable { navViewModel.toggleEngineArchitectureSheet(true) }
                        .testTag("open_engine_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            uiState.isAmapKeyConfigured -> "高"
                            uiState.isTencentKeyConfigured -> "腾"
                            uiState.preferredEngine == NavEngineType.AMAP_LBS -> "高"
                            uiState.preferredEngine == NavEngineType.TENCENT_LBS -> "腾"
                            else -> "源"
                        },
                        color = IosBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (uiState.sheetDetent == SheetDetent.COLLAPSED && !uiState.showSearchSheet) {
                return@Column
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 搜索结果面板
            if (uiState.showSearchSheet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.statusBanner,
                        fontSize = 12.sp,
                        color = IosSystemGray
                    )
                    Text(
                        text = "取消",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosBlue,
                        modifier = Modifier.clickable { navViewModel.dismissSearchSheet() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = IosBlue, strokeWidth = 2.8.dp)
                    }
                } else {
                    Surface(
                        color = IosCardLight,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                            itemsIndexed(uiState.searchResults, key = { _, item -> item.id }) { idx, poi ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            keyboardController?.hide()
                                            navViewModel.inspectPlace(
                                                title = poi.title,
                                                address = poi.address,
                                                category = poi.category,
                                                wgs84Point = poi.wgs84Point,
                                                sourceLabel = poi.sourceEngine
                                            )
                                        }
                                        .padding(horizontal = 14.dp, vertical = 11.dp)
                                        .testTag("search_result_${poi.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(IosRed.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = IosRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = poi.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1C1C1E),
                                            letterSpacing = (-0.2).sp
                                        )
                                        Text(
                                            text = poi.address,
                                            fontSize = 12.sp,
                                            color = IosSystemGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        onClick = {
                                            keyboardController?.hide()
                                            navViewModel.planRouteTo(
                                                destName = poi.title,
                                                destAddress = poi.address,
                                                destWgs84 = poi.wgs84Point
                                            )
                                        },
                                        color = IosBlue.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "去这里",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IosBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                if (idx < uiState.searchResults.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 58.dp),
                                        color = IosSystemGray5,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
                return@Column
            }

            // 探索态主体内容：仅保留纯净苹果地图「个人收藏 + 周边探索 + 最近路线」，交通工具选型留待选定目的地后展示
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. iOS 18 个人收藏 (Favorites) InsetGrouped 卡片
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "个人收藏",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E),
                            letterSpacing = (-0.3).sp
                        )
                        if (!hasLocationPermission) {
                            Text(
                                text = "开启定位",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = IosBlue,
                                modifier = Modifier
                                    .clickable { onRequestLocationPermission() }
                                    .testTag("grant_location_button")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            itemsIndexed(savedPlaces, key = { _, p -> "fav_${p.id}" }) { idx, place ->
                                val palette = listOf(IosBlue, IosOrange, Color(0xFF34C759), IosIndigo, IosPurple)
                                val tint = palette[idx % palette.size]
                                val icon = when (place.category) {
                                    "常用" -> Icons.Default.Work
                                    "枢纽" -> Icons.Default.DirectionsSubway
                                    else -> Icons.Default.Star
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(66.dp)
                                        .clickable {
                                            navViewModel.planRouteTo(
                                                destName = place.title,
                                                destAddress = place.address,
                                                destWgs84 = LatLngPoint(place.latitude, place.longitude)
                                            )
                                        }
                                        .testTag("quick_place_${place.id}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(tint.copy(alpha = 0.14f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = place.title,
                                            tint = tint,
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(7.dp))
                                    Text(
                                        text = place.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1C1C1E),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        letterSpacing = (-0.2).sp
                                    )
                                    Text(
                                        text = place.category,
                                        fontSize = 11.sp,
                                        color = IosSystemGray
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. iOS 苹果地图「查找附近 (Find Nearby)」分类圆胶囊
                item {
                    Text(
                        text = "周边探索",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E),
                        letterSpacing = (-0.2).sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(nearbyCategories) { _, cat ->
                            Surface(
                                onClick = { navViewModel.searchByCategory(cat.keyword) },
                                color = IosCardLight,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.border(
                                    0.5.dp,
                                    IosSystemGray5,
                                    RoundedCornerShape(20.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(cat.tint, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            cat.icon,
                                            contentDescription = cat.title,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = cat.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1C1C1E)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. iOS「最近路线 (Recents)」InsetGrouped 列表
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最近路线",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E),
                            letterSpacing = (-0.2).sp
                        )
                        if (recentRoutes.isNotEmpty()) {
                            Text(
                                text = "清除",
                                fontSize = 12.sp,
                                color = IosBlue,
                                modifier = Modifier
                                    .clickable { navViewModel.clearAllHistory() }
                                    .testTag("clear_history_button")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = IosCardLight,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (recentRoutes.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(IosSystemGray6, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = IosSystemGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "点击上方「北京南站 / 中关村」或长按地图任意位置，即刻生成汽车、公交地铁或自行车导航方案",
                                    fontSize = 12.sp,
                                    color = IosSystemGray
                                )
                            }
                        } else {
                            Column {
                                recentRoutes.take(6).forEachIndexed { idx, route ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navViewModel.planRouteTo(
                                                    destName = route.destName,
                                                    destAddress = route.destAddress,
                                                    destWgs84 = LatLngPoint(route.destLat, route.destLon)
                                                )
                                            }
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(IosBlue.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Navigation,
                                                contentDescription = null,
                                                tint = IosBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = route.destName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1C1C1E)
                                            )
                                            val km = String.format("%.1f 公里", route.distanceMeters / 1000.0)
                                            val mins = (route.durationSeconds / 60).coerceAtLeast(1)
                                            Text(
                                                text = "${route.travelMode} · $km · 约 $mins 分钟",
                                                fontSize = 12.sp,
                                                color = IosSystemGray
                                            )
                                        }
                                        IconButton(
                                            onClick = { navViewModel.deleteHistoryItem(route.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "删除",
                                                tint = IosSystemGray2,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (idx < recentRoutes.take(6).lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 58.dp),
                                            color = IosSystemGray5,
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ==================== 2. iOS 18 地点详情抽屉卡片 (Apple Maps Place Card) ====================
 */
@Composable
fun IosPlaceDetailSheet(
    uiState: NavigationUiState,
    navViewModel: NavigationViewModel
) {
    val place = uiState.selectedPlace ?: return
    val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val distText = if (place.distanceMeters >= 1000) {
        String.format("%.1f 公里", place.distanceMeters / 1000.0)
    } else {
        "${place.distanceMeters} 米"
    }

    Surface(
        color = Color(0xFFF2F2F7).copy(alpha = 0.96f),
        shape = sheetShape,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, sheetShape, ambientColor = Color.Black.copy(alpha = 0.16f))
            .border(
                0.5.dp,
                Color.White.copy(alpha = 0.92f),
                sheetShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .background(Color(0x4D3C3C43), CircleShape)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E),
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${place.category} · 距您 $distText · ${place.sourceLabel}",
                        fontSize = 13.sp,
                        color = IosSystemGray
                    )
                }

                Surface(
                    onClick = { navViewModel.closePlaceOrRouteBackToExplore() },
                    color = Color(0x1F767680),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("close_place_detail_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0x993C3C43),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 四大出行方式快捷入口（汽车 / 公交地铁 / 自行车 / 收藏）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = {
                        navViewModel.selectTravelMode(TravelMode.DRIVING)
                        navViewModel.planRouteTo(place.title, place.subtitle, place.wgs84)
                    },
                    color = IosBlue,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(58.dp)
                        .testTag("place_card_directions_button")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${place.estimatedDriveMinutes} 分钟",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text("汽车路线", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                }

                IosPlaceActionTile(
                    icon = Icons.Default.DirectionsSubway,
                    label = "公交地铁",
                    tint = IosPurple,
                    onClick = {
                        navViewModel.selectTravelMode(TravelMode.TRANSIT)
                        navViewModel.planRouteTo(place.title, place.subtitle, place.wgs84)
                    },
                    modifier = Modifier.weight(1f)
                )

                IosPlaceActionTile(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    label = "自行车",
                    tint = IosGreen,
                    onClick = {
                        navViewModel.selectTravelMode(TravelMode.CYCLING)
                        navViewModel.planRouteTo(place.title, place.subtitle, place.wgs84)
                    },
                    modifier = Modifier.weight(1f)
                )

                IosPlaceActionTile(
                    icon = Icons.Default.BookmarkBorder,
                    label = "收藏",
                    tint = IosOrange,
                    onClick = {
                        navViewModel.bookmarkDestination(
                            title = place.title,
                            address = place.subtitle,
                            category = place.category,
                            point = place.wgs84
                        )
                    },
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("bookmark_current_dest_button")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = IosCardLight,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "详细地址与坐标系校准",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosSystemGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = place.subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1C1C1E)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = IosSystemGray5,
                        thickness = 0.5.dp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("GCJ-02 (中国地图标准)", fontSize = 11.sp, color = IosSystemGray)
                            Text(
                                text = String.format("%.5f, %.5f", place.gcj02.latitude, place.gcj02.longitude),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("WGS-84 (GPS 原始坐标)", fontSize = 11.sp, color = IosSystemGray)
                            Text(
                                text = String.format("%.5f, %.5f", place.wgs84.latitude, place.wgs84.longitude),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosPlaceActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color(0xFFE9E9EE),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(58.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}

/**
 * ==================== 3. iOS 18 路线规划抽屉（支持汽车、公交地铁、自行车、步行专属卡片） ====================
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IosRoutePreviewSheet(
    uiState: NavigationUiState,
    expandSteps: Boolean,
    onToggleExpandSteps: () -> Unit,
    navViewModel: NavigationViewModel
) {
    val route = uiState.activeRoute
    val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

    Surface(
        color = Color(0xFFF2F2F7).copy(alpha = 0.96f),
        shape = sheetShape,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, sheetShape, ambientColor = Color.Black.copy(alpha = 0.16f))
            .border(
                0.5.dp,
                Color.White.copy(alpha = 0.92f),
                sheetShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Grabber
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .background(Color(0x4D3C3C43), CircleShape)
                )
            }

            if (uiState.isCalculatingRoute || route == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = IosBlue, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("正在为您规划最优${uiState.travelMode.label}路线...", fontSize = 14.sp, color = IosSystemGray)
                    }
                }
                return@Column
            }

            // 1. 顶部标题与关闭圆叉
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${uiState.travelMode.label}前往 ${route.destinationName}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E),
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "从 ${route.originName} 出发 · ${route.engineNotice}",
                        fontSize = 11.sp,
                        color = IosSystemGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = { navViewModel.closePlaceOrRouteBackToExplore() },
                    color = Color(0x1F767680),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("close_route_preview_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭路线",
                            tint = Color(0x993C3C43),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. iOS 四大出行模式分段滑块控制器：汽车 / 公交地铁 / 自行车 / 步行
            Surface(
                color = Color(0x1F767680),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.5.dp)
                ) {
                    TravelMode.entries.forEach { mode ->
                        val selected = uiState.travelMode == mode
                        Surface(
                            onClick = { navViewModel.selectTravelMode(mode) },
                            color = if (selected) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = if (selected) 1.5.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = modeIcon(mode),
                                    contentDescription = mode.label,
                                    tint = if (selected) IosBlue else Color(0xFF636366),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = shortModeLabel(mode),
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) Color(0xFF1C1C1E) else Color(0xFF636366)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 根据当前所选模式展示专属 iOS 视觉卡片
            if (uiState.travelMode == TravelMode.TRANSIT && route.transitOptions.isNotEmpty()) {
                IosTransitPlansSection(
                    route = route,
                    selectedIndex = uiState.selectedRouteOptionIndex,
                    onSelectPlan = { navViewModel.selectRouteOption(it) },
                    onStartTransitGuide = { navViewModel.startRealNavigation(enableCruiseDemo = true) }
                )
            } else {
                IosRoadModeRouteCard(
                    uiState = uiState,
                    route = route,
                    expandSteps = expandSteps,
                    onToggleExpandSteps = onToggleExpandSteps,
                    navViewModel = navViewModel
                )
            }
        }
    }
}

/**
 * iOS 苹果地图风格「公交地铁 (Transit)」多方案换乘卡片 + 竖向站点时间轴
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IosTransitPlansSection(
    route: CalculatedRoute,
    selectedIndex: Int,
    onSelectPlan: (Int) -> Unit,
    onStartTransitGuide: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 10.dp)
    ) {
        itemsIndexed(route.transitOptions, key = { _, p -> p.id }) { idx, plan ->
            val isSelected = idx == selectedIndex
            Surface(
                onClick = { onSelectPlan(idx) },
                color = IosCardLight,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.8.dp else 0.5.dp,
                        color = if (isSelected) IosBlue else IosSystemGray5,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${plan.totalDurationMinutes} 分钟",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1C1C1E),
                                    letterSpacing = (-0.4).sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = IosPurple.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = plan.tagLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IosPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "票价 ¥${plan.costYuan} · 步行 ${plan.walkingDistanceMeters} 米 · ${plan.frequencyText}",
                                fontSize = 12.sp,
                                color = IosSystemGray
                            )
                        }

                        Surface(
                            onClick = onStartTransitGuide,
                            color = IosGreen,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("start_transit_nav_$idx")
                        ) {
                            Text(
                                text = "出发",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // iOS 经典彩色地铁/公交线路胶囊流 (Subway & Bus Pill Flow)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        plan.segments.forEachIndexed { sIdx, seg ->
                            val badgeColor = Color(seg.lineBadgeColorHex)
                            if (seg.type == "WALK") {
                                Surface(
                                    color = IosSystemGray6,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.DirectionsWalk,
                                            contentDescription = null,
                                            tint = IosSystemGray,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${seg.durationMinutes}分",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = IosSystemGray
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = badgeColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (seg.type == "SUBWAY") {
                                                Icons.Default.DirectionsSubway
                                            } else {
                                                Icons.Default.DirectionsBus
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${seg.lineName} (${seg.stationCount}站)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            if (sIdx < plan.segments.lastIndex) {
                                Text(
                                    text = "›",
                                    fontSize = 13.sp,
                                    color = IosSystemGray2,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }

                    // 选中该公交地铁方案时，展开竖向进出站口时间轴
                    AnimatedVisibility(visible = isSelected) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            HorizontalDivider(color = IosSystemGray5, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            plan.segments.forEach { seg ->
                                val dotColor = Color(seg.lineBadgeColorHex)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(8.dp)
                                            .background(dotColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${seg.lineName} · ${seg.departureStop} → ${seg.arrivalStop}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1C1C1E)
                                        )
                                        if (seg.exitInfo.isNotEmpty()) {
                                            Text(
                                                text = "出站指引：${seg.exitInfo}（共乘 ${seg.stationCount} 站，约 ${seg.durationMinutes} 分钟）",
                                                fontSize = 11.sp,
                                                color = IosBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 汽车 / 自行车 / 步行专属 iOS 路线卡片
 */
@Composable
private fun IosRoadModeRouteCard(
    uiState: NavigationUiState,
    route: CalculatedRoute,
    expandSteps: Boolean,
    onToggleExpandSteps: () -> Unit,
    navViewModel: NavigationViewModel
) {
    val baseMins = (uiState.remainingDurationSeconds / 60).coerceAtLeast(1)
    val baseKm = uiState.remainingDistanceMeters / 1000.0
    val etaStr = remember(uiState.remainingDurationSeconds) {
        val etaMillis = System.currentTimeMillis() + uiState.remainingDurationSeconds * 1000L
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(etaMillis))
    }

    Surface(
        color = IosCardLight,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$baseMins 分钟",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1C1C1E),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val badgeText = when (uiState.travelMode) {
                            TravelMode.DRIVING -> "最快 · 畅通"
                            TravelMode.CYCLING -> "单车绿道优先"
                            TravelMode.WALKING -> "人行步道优先"
                            TravelMode.TRANSIT -> "公共交通"
                        }
                        Surface(
                            color = IosGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.padding(bottom = 3.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = String.format("%.1f 公里 · 预计 %s 到达", baseKm, etaStr),
                        fontSize = 12.sp,
                        color = IosSystemGray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { navViewModel.startRealNavigation(enableCruiseDemo = false) },
                        color = Color(0xFFE5E5EA),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.testTag("start_cruise_demo_button")
                    ) {
                        Text(
                            text = "GPS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }

                    Surface(
                        onClick = { navViewModel.startRealNavigation(enableCruiseDemo = true) },
                        color = Color(0xFF34C759),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.testTag("start_real_nav_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "出发",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 模式专属特征指标条（汽车显示红绿灯与路况光谱；自行车显示爬升与卡路里）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .background(IosSystemGray6, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (uiState.travelMode) {
                    TravelMode.DRIVING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Traffic,
                                contentDescription = null,
                                tint = IosOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "途经 ${route.trafficLightsCount} 个红绿灯 · ¥0 免高速费",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                        Text(
                            text = "92% 路段畅通",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosGreen
                        )
                    }
                    TravelMode.CYCLING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Terrain,
                                contentDescription = null,
                                tint = IosGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "累计爬升 ${route.elevationGainMeters} 米 · 坡度平缓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = IosOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${route.caloriesBurnedKcal} 千卡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosOrange
                            )
                        }
                    }
                    else -> {
                        val stepsCount = (route.totalDistanceMeters * 1.35).toInt()
                        Text(
                            text = "预计步数 $stepsCount 步 · 消耗 ${route.caloriesBurnedKcal} 千卡",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1C1C1E)
                        )
                        Text(
                            text = "人行绿道",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosGreen
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                color = IosSystemGray5,
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpandSteps() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("toggle_steps_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expandSteps) "收起诱导步骤详情" else "查看完整 ${route.steps.size} 步导航详情",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosBlue
                )
                Text(
                    text = route.engineUsed.displayName,
                    fontSize = 11.sp,
                    color = IosSystemGray
                )
            }

            AnimatedVisibility(visible = expandSteps) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .heightIn(max = 165.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                ) {
                    itemsIndexed(route.steps, key = { _, s -> "step_${s.index}" }) { idx, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    step.actionType.contains("左") -> Icons.Default.TurnLeft
                                    step.actionType.contains("右") -> Icons.Default.TurnRight
                                    step.actionType.contains("掉头") -> Icons.Default.UTurnLeft
                                    else -> Icons.Default.Navigation
                                },
                                contentDescription = step.actionType,
                                tint = IosBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step.instruction,
                                fontSize = 13.sp,
                                color = Color(0xFF1C1C1E),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${step.distanceMeters}米",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosSystemGray
                            )
                        }
                        if (idx < route.steps.lastIndex) {
                            HorizontalDivider(color = IosSystemGray5, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

private fun modeIcon(mode: TravelMode): ImageVector {
    return when (mode) {
        TravelMode.DRIVING -> Icons.Default.DirectionsCar
        TravelMode.TRANSIT -> Icons.Default.DirectionsSubway
        TravelMode.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
        TravelMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    }
}

private fun shortModeLabel(mode: TravelMode): String {
    return when (mode) {
        TravelMode.DRIVING -> "驾车"
        TravelMode.TRANSIT -> "公交"
        TravelMode.CYCLING -> "骑行"
        TravelMode.WALKING -> "步行"
    }
}

/**
 * ==================== 3b. 公交地铁模式专属：周边地铁站与公交站实时到站时刻表、途经站点序列与进出站口指引 ====================
 */
@Composable
private fun TransitStationExplorerSection(
    stations: List<TransitStationInfo>,
    selectedStation: TransitStationInfo,
    onSelectStation: (TransitStationInfo) -> Unit,
    onWalkToStation: (TransitStationInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 顶部标题 + 站点类型统计
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsSubway,
                    contentDescription = null,
                    tint = Color(0xFF0891B2),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "附近地铁站与公交站 (实时到站)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
            }
            Text(
                text = "点击站点切换电子站牌",
                fontSize = 11.sp,
                color = IosSystemGray
            )
        }

        // 横向站点快速切换胶囊栏
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            itemsIndexed(stations, key = { _, st -> "chip_${st.id}" }) { _, st ->
                val isSelected = st.id == selectedStation.id
                val lineColor = Color(st.lines.firstOrNull()?.badgeColorHex ?: 0xFF1677FF)
                val isSubway = st.stationType == "SUBWAY"
                Surface(
                    onClick = { onSelectStation(st) },
                    color = if (isSelected) lineColor else IosCardLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isSelected) lineColor else lineColor.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("station_chip_${st.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSubway) Icons.Default.DirectionsSubway else Icons.Default.DirectionsBus,
                            contentDescription = st.name,
                            tint = if (isSelected) Color.White else lineColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = st.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF1C1C1E)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${st.distanceMeters}m",
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.88f) else IosSystemGray
                        )
                    }
                }
            }
        }

        // 当前选中站点：实时电子站牌详情大卡片
        val primaryColor = Color(selectedStation.lines.firstOrNull()?.badgeColorHex ?: 0xFF1677FF)
        Surface(
            color = IosCardLight,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 站点头部：站名 + 最近出入口 + 步行去进站口按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryColor,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (selectedStation.stationType == "SUBWAY") "地铁站" else "公交站",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = selectedStation.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "最近进站：${selectedStation.nearestExit} · 步行 ${selectedStation.distanceMeters}米 (约${selectedStation.walkingMinutes}分钟)",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    Surface(
                        onClick = { onWalkToStation(selectedStation) },
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("walk_to_station_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "步行去进站口",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "去站口",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }

                HorizontalDivider(color = IosSystemGray5, thickness = 0.5.dp)

                // 各条线路实时到站时刻与途经站点序列
                selectedStation.lines.forEachIndexed { idx, line ->
                    val badgeColor = Color(line.badgeColorHex)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = badgeColor,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = line.lineName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = line.directionTerminal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${line.nextStopName} · ${line.firstLastTime}",
                                    fontSize = 11.sp,
                                    color = IosSystemGray
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = if (line.nextArrivalMinutes <= 1) "即将进站" else "${line.nextArrivalMinutes} 分钟",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF059669)
                                    )
                                }
                                Text(
                                    text = "下班 ${line.secondArrivalMinutes}分 · ${line.crowdStatus}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // 途经站点横向序列条（高亮当前站与列车行驶方向）
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(line.stopsSequence, key = { sIdx, stop -> "${idx}_${sIdx}_$stop" }) { sIdx, stopName ->
                                val isCurrentStop = sIdx == line.currentStopIndex
                                val isPassed = sIdx < line.currentStopIndex
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = when {
                                            isCurrentStop -> badgeColor
                                            isPassed -> Color(0xFFE2E8F0)
                                            else -> badgeColor.copy(alpha = 0.12f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrentStop) "● $stopName(本站)" else stopName,
                                            fontSize = 10.sp,
                                            fontWeight = if (isCurrentStop) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = when {
                                                isCurrentStop -> Color.White
                                                isPassed -> Color(0xFF94A3B8)
                                                else -> Color(0xFF1E293B)
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (sIdx < line.stopsSequence.lastIndex) {
                                        Text(
                                            text = "→",
                                            fontSize = 10.sp,
                                            color = if (isPassed) Color(0xFFCBD5E1) else badgeColor,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 进出站口与周边地标指引
                if (selectedStation.exits.isNotEmpty()) {
                    Text(
                        text = "进出站口与街景地标指引",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        selectedStation.exits.forEach { exit ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = exit.exitCode,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFACC15),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = exit.landmarkDesc,
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${exit.distanceMeters}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IosSystemGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ==================== 4. 模式自适应顶部导航诱导大卡片（驾车=高德深蓝黑车道卡；步行=翡翠绿步行罗盘卡；骑行=青绿破风卡；地铁探索态=附近地铁公交实时电子站牌） ====================
 */
@Composable
fun IosTopTurnByTurnBanner(
    uiState: NavigationUiState,
    isVoiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    onSelectTransitStation: (TransitStationInfo) -> Unit = {},
    onWalkToTransitStation: (TransitStationInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val effectiveMode = uiState.activeRoute?.travelMode ?: uiState.travelMode
    // 当处于「公交地铁」站点探索模式（无导航路线）时，顶部展示「附近地铁与公交实时电子站牌」而非驾车转向诱导卡！
    if (effectiveMode == TravelMode.TRANSIT && uiState.activeRoute == null && uiState.nearbyTransitStations.isNotEmpty()) {
        val station = uiState.selectedTransitStation ?: uiState.nearbyTransitStations.first()
        val firstLine = station.lines.firstOrNull()
        val secondLine = station.lines.getOrNull(1)
        val lineColor = Color(firstLine?.badgeColorHex ?: 0xFFC23A30)

        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            shape = RoundedCornerShape(24.dp),
            modifier = modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 第一行：当前电子站牌站名 + 最近站口距离 + 步行去进站口按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(lineColor, RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (station.stationType == "SUBWAY") Icons.Default.DirectionsSubway else Icons.Default.DirectionsBus,
                                contentDescription = station.name,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${station.name} (${if (station.stationType == "SUBWAY") "地铁站" else "公交站"})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "距您 ${station.distanceMeters}m",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "最近进站：${station.nearestExit}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = { onWalkToTransitStation(station) },
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "去进站口",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // 第二行：实时列车/公交到站 LED 电子屏显示
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOfNotNull(firstLine, secondLine).forEach { line ->
                        val lColor = Color(line.badgeColorHex)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = lColor,
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = line.shortBadge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "${line.directionTerminal} · ${line.nextStopName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = if (line.nextArrivalMinutes <= 1) "⚡ 即将进站" else "⚡ ${line.nextArrivalMinutes}分钟 · 下班${line.secondArrivalMinutes}分",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }
            }
        }
        return
    }
    val steps = uiState.activeRoute?.steps ?: emptyList()
    val currentStep = steps.getOrNull(uiState.currentStepIndex)
    val nextStep = steps.getOrNull(uiState.currentStepIndex + 1)
    val action = currentStep?.actionType ?: "直行"
    val distMeters = if (uiState.activeRoute != null) {
        uiState.distanceToNextTurnMeters.coerceAtLeast(10)
    } else {
        120
    }
    val defaultRoad = when (effectiveMode) {
        TravelMode.WALKING -> "南池子人行步道"
        TravelMode.CYCLING -> "东华门非机动车道"
        TravelMode.TRANSIT -> "地铁换乘步行接驳"
        TravelMode.DRIVING -> "南池子大街"
    }
    val roadName = currentStep?.roadName?.takeIf { it.isNotBlank() && it != "无名道路" } ?: defaultRoad

    // iOS Apple Maps 导航顶栏配色：深炭黑（驾车/公交）与深果绿（步行/骑行）
    val primaryBannerColor = when (effectiveMode) {
        TravelMode.WALKING, TravelMode.CYCLING -> Color(0xFF134E2E).copy(alpha = 0.96f)
        TravelMode.TRANSIT -> Color(0xFF1C2C3E).copy(alpha = 0.96f)
        TravelMode.DRIVING -> Color(0xFF1C1C1E).copy(alpha = 0.95f)
    }
    val secondaryStripColor = when (effectiveMode) {
        TravelMode.WALKING, TravelMode.CYCLING -> Color(0xFF0E3B23).copy(alpha = 0.98f)
        TravelMode.TRANSIT -> Color(0xFF152230).copy(alpha = 0.98f)
        TravelMode.DRIVING -> Color(0xFF2C2C2E).copy(alpha = 0.98f)
    }

    val bannerShape = RoundedCornerShape(22.dp)

    Surface(
        color = primaryBannerColor,
        shape = bannerShape,
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, bannerShape, ambientColor = Color.Black.copy(alpha = 0.28f))
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), bannerShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 主诱导区：大转向图标 + 距离 + 路名 + 右侧单颗毛玻璃静音按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (effectiveMode == TravelMode.TRANSIT) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(IosBlue, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsSubway,
                            contentDescription = "地铁换乘",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = when {
                            action.contains("右") -> Icons.Default.TurnRight
                            action.contains("掉头") -> Icons.Default.UTurnLeft
                            action.contains("直") -> Icons.Default.Straight
                            else -> Icons.Default.TurnLeft
                        },
                        contentDescription = action,
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (distMeters >= 1000) {
                            Text(
                                text = String.format("%.1f", distMeters / 1000.0),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.8).sp
                            )
                            Text(
                                text = " 公里",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEBEBF5).copy(alpha = 0.72f),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        } else {
                            Text(
                                text = "$distMeters",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.8).sp
                            )
                            Text(
                                text = " 米",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEBEBF5).copy(alpha = 0.72f),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = roadName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEBEBF5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Apple Maps 圆形毛玻璃语音开关
                Surface(
                    onClick = onToggleVoice,
                    color = Color.White.copy(alpha = if (isVoiceEnabled) 0.16f else 0.08f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("toggle_voice_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isVoiceEnabled) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = if (isVoiceEnabled) "关闭语音" else "开启语音",
                            tint = if (isVoiceEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Apple Maps "随后" 下一动作提示条
            if (nextStep != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(secondaryStripColor)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "随后",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEBEBF5).copy(alpha = 0.68f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = when {
                            nextStep.actionType.contains("右") -> Icons.Default.TurnRight
                            nextStep.actionType.contains("掉头") -> Icons.Default.UTurnLeft
                            nextStep.actionType.contains("直") -> Icons.Default.Straight
                            else -> Icons.Default.TurnLeft
                        },
                        contentDescription = nextStep.actionType,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = nextStep.roadName.takeIf { it.isNotBlank() && it != "无名道路" } ?: nextStep.actionType,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * ==================== 5. 纯正 iOS 18 Apple Maps 底部导航控制栏 ====================
 */
@Composable
fun IosActiveNavigationBottomCard(
    uiState: NavigationUiState,
    onOpenHud: () -> Unit,
    onEndRoute: () -> Unit
) {
    val effectiveMode = uiState.activeRoute?.travelMode ?: uiState.travelMode
    val distMeters = uiState.remainingDistanceMeters.coerceAtLeast(0)
    val durSec = uiState.remainingDurationSeconds.coerceAtLeast(0)

    val etaClock = remember(durSec) {
        val etaMillis = System.currentTimeMillis() + durSec * 1000L
        SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(etaMillis))
    }
    val remMins = (durSec / 60).coerceAtLeast(1)
    val distValue = if (distMeters >= 1000) {
        String.format("%.1f", distMeters / 1000.0)
    } else {
        "$distMeters"
    }
    val distUnit = if (distMeters >= 1000) "公里" else "米"

    val cardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Surface(
        color = Color(0xFFF9F9FB).copy(alpha = 0.96f),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, cardShape, ambientColor = Color.Black.copy(alpha = 0.16f))
            .border(0.5.dp, Color.White.copy(alpha = 0.92f), cardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0x4D3C3C43), CircleShape)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 到达时间 / 剩余分钟 / 剩余里程三列紧凑 Apple 排版
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = if (distMeters == 0) "已到达" else etaClock,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E),
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = "预计到达",
                        fontSize = 11.sp,
                        color = IosSystemGray
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (distMeters == 0) "0 分" else "$remMins 分",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (effectiveMode == TravelMode.WALKING || effectiveMode == TravelMode.CYCLING) {
                            Color(0xFF248A3D)
                        } else {
                            Color(0xFF1C1C1E)
                        },
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = "$distValue $distUnit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = IosSystemGray
                    )
                }

                // 2. HUD全览小圆按钮 + Apple 红色「结束」胶囊按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenHud,
                        color = Color(0xFFE5E5EA),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("open_hud_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "HUD 抬头显示",
                                tint = Color(0xFF1C1C1E),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onEndRoute,
                        color = Color(0xFFFF3B30).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(19.dp),
                        modifier = Modifier.testTag("stop_nav_button")
                    ) {
                        Text(
                            text = "结束",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF3B30),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * ==================== 6. iOS 18 「选择地图与中国地图数据源设置」半屏弹窗 ====================
 */
@Composable
fun IosMapModeSelectorSheet(
    currentStyle: MapTileStyle,
    preferredEngine: NavEngineType,
    useNativeAmap3d: Boolean,
    hasAmapAndroidKey: Boolean,
    isEmulatorEnv: Boolean,
    onSelectStyle: (MapTileStyle) -> Unit,
    onSelectEngine: (NavEngineType) -> Unit,
    onToggleNativeAmap3d: (Boolean) -> Unit,
    onOpenArchitectureDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "地图模式与渲染引擎",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            Surface(
                onClick = onDismiss,
                color = Color(0xFFE3E3E8),
                shape = CircleShape,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color(0xFF636366),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val styles = MapTileStyle.entries
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (rowIdx in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (colIdx in 0..1) {
                        val style = styles[rowIdx * 2 + colIdx]
                        val isSelected = style == currentStyle
                        Surface(
                            onClick = { onSelectStyle(style) },
                            color = IosCardLight,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) IosBlue else IosSystemGray4,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = style.label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) IosBlue else Color(0xFF1C1C1E)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "当前选中",
                                            tint = IosBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = style.subtitle,
                                    fontSize = 11.sp,
                                    color = IosSystemGray,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 地图渲染引擎切换：轻量安全底图 vs 高德原生 3D OpenGL 引擎
        Text(
            text = "地图渲染引擎",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = IosSystemGray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = IosCardLight,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { onToggleNativeAmap3d(false) },
                        color = if (!useNativeAmap3d) IosBlue else IosSystemGray6,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_lightweight_map_engine")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "轻量高清底图",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!useNativeAmap3d) Color.White else Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "模拟器稳定 · 极速加载",
                                fontSize = 10.sp,
                                color = if (!useNativeAmap3d) Color.White.copy(alpha = 0.85f) else IosSystemGray
                            )
                        }
                    }

                    Surface(
                        onClick = { if (hasAmapAndroidKey) onToggleNativeAmap3d(true) },
                        color = if (useNativeAmap3d && hasAmapAndroidKey) IosBlue else IosSystemGray6,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_amap_native_3d_engine")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "高德原生 3D",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    useNativeAmap3d && hasAmapAndroidKey -> Color.White
                                    !hasAmapAndroidKey -> IosSystemGray
                                    else -> Color(0xFF1C1C1E)
                                }
                            )
                            Text(
                                text = if (hasAmapAndroidKey) "真 3D 建筑 · 推荐真机" else "需配置 Android Key",
                                fontSize = 10.sp,
                                color = if (useNativeAmap3d && hasAmapAndroidKey) {
                                    Color.White.copy(alpha = 0.85f)
                                } else {
                                    IosSystemGray
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (useNativeAmap3d) {
                        "当前已开启高德官方原生 3D 建筑引擎（MapView）。如模拟器画面异常，可随时切回轻量高清底图。"
                    } else {
                        "当前为轻量高清底图（零原生 OpenGL 启动负载，最稳定）。点击右侧「高德原生 3D」可随时切换体验真 3D 建筑。"
                    },
                    fontSize = 11.sp,
                    color = IosSystemGray,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "中国地图算路与公交地铁数据源",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = IosSystemGray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = IosCardLight,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavEngineType.entries.forEach { engine ->
                        val active = preferredEngine == engine
                        Surface(
                            onClick = { onSelectEngine(engine) },
                            color = if (active) IosBlue else IosSystemGray6,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = engine.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = engine.coordBadge,
                                    fontSize = 10.sp,
                                    color = if (active) Color.White.copy(alpha = 0.85f) else IosSystemGray
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenArchitectureDetails() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "查看「中国地图开源 vs 商业接口」深度解析",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosBlue
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = IosBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
