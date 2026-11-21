package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.EncryptedTrackPoint
import com.example.data.remote.GroupMember
import com.example.data.remote.GroupMessage
import com.example.data.remote.GuardianGroup
import com.example.data.remote.GuardianResolution
import com.example.data.remote.MessageKind
import com.example.data.remote.ResolutionStatus
import com.example.ui.GuardianPanelState
import com.example.ui.GuardianUiState
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun IncomingResolutionAlertBanner(
    resolutions: List<GuardianResolution>,
    currentUserId: String,
    nowMillis: Long,
    onVetoNow: (GuardianResolution) -> Unit,
    onOpenGuardianSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val urgent = resolutions.firstOrNull {
        it.targetUid == currentUserId && it.parsedStatus == ResolutionStatus.VOTING
    } ?: return

    val unlockMs = urgent.unlockAt?.toDate()?.time ?: (nowMillis + 600_000L)
    val remainingSec = max(0L, (unlockMs - nowMillis) / 1000L)

    Surface(
        color = Color(0xFF991B1B),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .border(1.5.dp, Color(0xFFFCA5A5), RoundedCornerShape(18.dp))
            .testTag("incoming_resolution_alert_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "紧急查看通知",
                    tint = Color(0xFFFEF08A),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "有人申请查看您的加密行程（${formatMmSs(remainingSec)}）",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "发起人：${urgent.initiatorName} · 原因：${urgent.reason}",
                color = Color(0xFFFEE2E2),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onVetoNow(urgent) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF991B1B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("urgent_banner_veto_button")
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("一票否决（拒绝）", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onOpenGuardianSheet,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("查看详情", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GuardianSafetySheetContent(
    viewModel: GuardianViewModel,
    groupsState: GuardianUiState<List<GuardianGroup>>,
    activeGroup: GuardianGroup?,
    members: List<GroupMember>,
    messages: List<GroupMessage>,
    resolutions: List<GuardianResolution>,
    encryptedTracks: List<EncryptedTrackPoint>,
    panelState: GuardianPanelState,
    nowMillis: Long,
    onRequestNotificationPermission: () -> Unit,
    onSignOutClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var chatDraft by rememberSaveable { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf(false) }

    val selfMember = members.firstOrNull { it.uid == viewModel.currentUserId }
    val allGroups = (groupsState as? GuardianUiState.Success)?.data ?: emptyList()

    // Dialogs inside the Guardian sheet
    if (panelState.showCreateGroupDialog) {
        CreateGroupDialog(
            defaultNickname = viewModel.currentUserDisplayName,
            isBusy = panelState.isBusy,
            onDismiss = { viewModel.showCreateGroupDialog(false) },
            onConfirm = { groupName, nickname, cooldown ->
                viewModel.createGroup(groupName, nickname, cooldown)
            }
        )
    }

    if (panelState.showJoinGroupDialog) {
        JoinGroupDialog(
            defaultNickname = viewModel.currentUserDisplayName,
            isBusy = panelState.isBusy,
            onDismiss = { viewModel.showJoinGroupDialog(false) },
            onConfirm = { code, nickname ->
                viewModel.joinGroup(code, nickname)
            }
        )
    }

    if (panelState.showConsentDialog) {
        EmergencyConsentDialog(
            cooldownSeconds = activeGroup?.unlockDurationSeconds ?: 600L,
            onDismiss = { viewModel.showConsentDialog(false) },
            onAccept = {
                onRequestNotificationPermission()
                viewModel.confirmConsentAndEnableTracking()
            }
        )
    }

    val targetForRes = panelState.initiateTargetMember
    if (targetForRes != null && activeGroup != null) {
        InitiateResolutionDialog(
            targetMember = targetForRes,
            cooldownSeconds = activeGroup.unlockDurationSeconds,
            isBusy = panelState.isBusy,
            onDismiss = { viewModel.openInitiateResolutionFor(null) },
            onConfirm = { reason ->
                viewModel.submitInitiateResolution(targetForRes, reason)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 420.dp, max = 680.dp)
            .padding(horizontal = 16.dp)
    ) {
        // 1. Header bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF1677FF).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF1677FF),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "安全守护与加密行踪",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "当前账号：${viewModel.currentUserDisplayName}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onSignOutClick, modifier = Modifier.testTag("sign_out_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "退出登录",
                    tint = Color(0xFF64748B)
                )
            }
            TextButton(onClick = onDismiss) {
                Text("收起", color = Color(0xFF1677FF), fontWeight = FontWeight.Bold)
            }
        }

        // Feedback Banner / Errors
        if (panelState.feedbackBanner != null) {
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = panelState.feedbackBanner,
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearFeedbackBanner() }) {
                        Text("关闭", fontSize = 11.sp)
                    }
                }
            }
        }

        if (panelState.isBusy) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = Color(0xFF1677FF)
            )
        }

        // 2. Incoming Urgent Veto Banner inside sheet as well
        IncomingResolutionAlertBanner(
            resolutions = resolutions,
            currentUserId = viewModel.currentUserId,
            nowMillis = nowMillis,
            onVetoNow = { viewModel.exerciseOneVoteVeto(it) },
            onOpenGuardianSheet = {},
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 3. Group Selector & Invite Code Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allGroups.forEach { grp ->
                FilterChip(
                    selected = grp.id == activeGroup?.id,
                    onClick = { viewModel.selectGroup(grp.id) },
                    label = { Text(grp.name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
            OutlinedButton(
                onClick = { viewModel.showCreateGroupDialog(true) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("open_create_group_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("建群", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { viewModel.showJoinGroupDialog(true) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("open_join_group_button")
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("进群", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (groupsState) {
            is GuardianUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1677FF))
                }
            }

            is GuardianUiState.Error -> {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "同步失败：${groupsState.message}",
                        color = Color(0xFFB91C1C),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is GuardianUiState.Success -> {
                if (activeGroup == null) {
                    NoGroupOnboardingSection(
                        onCreateClick = { viewModel.showCreateGroupDialog(true) },
                        onJoinClick = { viewModel.showJoinGroupDialog(true) }
                    )
                } else {
                    // Invite Code Strip + Group rule summary
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "邀请码：${activeGroup.inviteCode}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                val waitText = if (activeGroup.unlockDurationSeconds >= 60L) {
                                    "${activeGroup.unlockDurationSeconds / 60}分钟"
                                } else {
                                    "${activeGroup.unlockDurationSeconds}秒"
                                }
                                Text(
                                    text = "${activeGroup.memberUids.size}位守护成员 · 通知冷却$waitText · 本人一票否决",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            TextButton(onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("PureNav Invite", activeGroup.inviteCode))
                                copiedNotice = true
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (copiedNotice) "已复制" else "复制邀请码", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Self Encryption Upload Bar
                    SelfEncryptedTrackingCard(
                        selfMember = selfMember,
                        latestCiphertext = panelState.latestCiphertextPreview
                            .ifBlank {
                                encryptedTracks.firstOrNull { it.ownerUid == viewModel.currentUserId }?.encryptedPayload ?: ""
                            },
                        onToggleTracking = { enabled ->
                            if (enabled) onRequestNotificationPermission()
                            viewModel.toggleTrackingPaused(enabled)
                        },
                        onUploadNow = { viewModel.uploadEncryptedLocationNow() },
                        onRevokeConsent = { viewModel.revokeConsentAndDeleteKey() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sub-tabs: 群聊与决议 | 成员与发起 | 审计记录
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF1677FF)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("群聊决议(${resolutions.count { it.parsedStatus == ResolutionStatus.VOTING }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("成员状态(${members.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("安全审计", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (selectedTab) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(resolutions, key = { "res_${it.id}" }) { res ->
                                    ResolutionDecisionCard(
                                        resolution = res,
                                        currentUserId = viewModel.currentUserId,
                                        nowMillis = nowMillis,
                                        onApprove = { viewModel.castVote(res, approve = true) },
                                        onReject = { viewModel.castVote(res, approve = false) },
                                        onVeto = { viewModel.exerciseOneVoteVeto(res) },
                                        onUnlockAndDecrypt = { viewModel.unlockAndDecryptTargetRoute(res) },
                                        onFinalizeRejected = { viewModel.finalizeRejected(res) }
                                    )
                                }

                                items(messages, key = { "msg_${it.id}" }) { msg ->
                                    GroupMessageBubble(
                                        msg = msg,
                                        isSelf = msg.senderUid == viewModel.currentUserId
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatDraft,
                                    onValueChange = { chatDraft = it },
                                    placeholder = { Text("发送群消息报平安或沟通...", fontSize = 13.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("guardian_chat_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (chatDraft.isNotBlank()) {
                                            viewModel.sendGroupMessage(chatDraft)
                                            chatDraft = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF1677FF), CircleShape)
                                        .testTag("guardian_chat_send")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "发送消息",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        1 -> {
                            LazyColumn(
                                modifier = Modifier
                                    .heightIn(max = 360.dp)
                                    .padding(bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(members, key = { it.uid }) { member ->
                                    MemberStatusCard(
                                        member = member,
                                        isSelf = member.uid == viewModel.currentUserId,
                                        trackCount = encryptedTracks.count { it.ownerUid == member.uid },
                                        onInitiateResolution = { viewModel.openInitiateResolutionFor(member) }
                                    )
                                }
                            }
                        }

                        else -> {
                            SecurityAuditTab(
                                group = activeGroup,
                                resolutions = resolutions,
                                encryptedTracks = encryptedTracks,
                                isOwner = activeGroup.ownerUid == viewModel.currentUserId,
                                onLeaveGroup = { viewModel.leaveActiveGroup() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoGroupOnboardingSection(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "建立您的家人/户外同行守护群",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. 所有坐标均先在本地通过 AES-256-GCM 加密，服务端只保存密文。\n2. 失联时只能由其他成员发起查看决议，系统第一时间通知本人。\n3. 本人拥有无条件【一票否决权】，仅当冷却窗期满且同意>反对才可解密查看行程。",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCreateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("创建守护群", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onJoinClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("邀请码加入", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelfEncryptedTrackingCard(
    selfMember: GroupMember?,
    latestCiphertext: String,
    onToggleTracking: (Boolean) -> Unit,
    onUploadNow: () -> Unit,
    onRevokeConsent: () -> Unit
) {
    val active = selfMember != null && selfMember.consentAccepted && selfMember.trackingEnabled
    val cardBg by animateColorAsState(
        targetValue = if (active) Color(0xFFECFDF5) else Color.White,
        label = "trackingCardBg"
    )
    val borderColor = if (active) Color(0xFF6EE7B7) else Color(0xFFE2E8F0)

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EnhancedEncryption,
                    contentDescription = null,
                    tint = if (active) Color(0xFF059669) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (active) "已开启 AES-256 加密定位上报" else "开启加密定位上报（需授权协议）",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (active) "服务器仅保存密文载荷 · 本人随时可一票否决或暂停" else "未授权，他人无法请求您的任何历史坐标",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Switch(
                    checked = active,
                    onCheckedChange = onToggleTracking,
                    modifier = Modifier.testTag("encrypted_tracking_switch")
                )
            }

            if (active) {
                Spacer(modifier = Modifier.height(6.dp))
                if (latestCiphertext.isNotBlank()) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "云端密文样本: ${latestCiphertext.take(54)}...",
                            color = Color(0xFF34D399),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onUploadNow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("manual_encrypted_upload_btn")
                    ) {
                        Text("立即加密上报一次", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onRevokeConsent,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("撤回授权并销钥", fontSize = 11.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResolutionDecisionCard(
    resolution: GuardianResolution,
    currentUserId: String,
    nowMillis: Long,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onVeto: () -> Unit,
    onUnlockAndDecrypt: () -> Unit,
    onFinalizeRejected: () -> Unit
) {
    val status = resolution.parsedStatus
    val isTarget = resolution.targetUid == currentUserId
    val hasApproved = currentUserId in resolution.approveUids
    val hasRejected = currentUserId in resolution.rejectUids
    val unlockMs = resolution.unlockAt?.toDate()?.time ?: (nowMillis + 600_000L)
    val remainingSec = max(0L, (unlockMs - nowMillis) / 1000L)
    val approveCount = resolution.approveUids.size
    val rejectCount = resolution.rejectUids.size
    val canUnlock = (status == ResolutionStatus.VOTING && remainingSec == 0L && approveCount > rejectCount && !isTarget) ||
        status == ResolutionStatus.UNLOCKED

    val borderCol = when (status) {
        ResolutionStatus.VOTING -> if (isTarget) Color(0xFFEF4444) else Color(0xFF3B82F6)
        ResolutionStatus.VETOED -> Color(0xFFF87171)
        ResolutionStatus.UNLOCKED -> Color(0xFF10B981)
        ResolutionStatus.REJECTED -> Color(0xFF94A3B8)
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderCol, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (status) {
                        ResolutionStatus.VOTING -> Icons.Default.Timer
                        ResolutionStatus.VETOED -> Icons.Default.Block
                        ResolutionStatus.UNLOCKED -> Icons.Default.LockOpen
                        ResolutionStatus.REJECTED -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = borderCol,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${resolution.initiatorName} 申请查看「${resolution.targetName}」行程",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = borderCol.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val statusLabel = when (status) {
                        ResolutionStatus.VOTING -> if (remainingSec > 0L) "通知窗 ${formatMmSs(remainingSec)}" else "已满期待结算"
                        ResolutionStatus.VETOED -> "已一票否决"
                        ResolutionStatus.UNLOCKED -> "已解锁密钥"
                        ResolutionStatus.REJECTED -> "未通过"
                    }
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = borderCol,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "申请事由：${resolution.reason}",
                fontSize = 12.sp,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "当前投票：赞成 $approveCount 票  ·  反对 $rejectCount 票  ·  规则：本人未一票否决且满期赞成>反对",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {
                status == ResolutionStatus.VOTING && isTarget -> {
                    Button(
                        onClick = onVeto,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("card_veto_button")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("行使【一票否决权】立即终止", fontWeight = FontWeight.ExtraBold)
                    }
                }

                status == ResolutionStatus.VOTING && !isTarget -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onApprove,
                            enabled = !hasApproved,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasApproved) Color(0xFF10B981) else Color(0xFF1677FF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (hasApproved) "已投赞成" else "同意查看", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            enabled = !hasRejected,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (hasRejected) "已投反对" else "反对查看", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (remainingSec == 0L) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (approveCount > rejectCount) {
                            Button(
                                onClick = onUnlockAndDecrypt,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("unlock_decrypt_button")
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("冷静期已过且同意占优 · 解密投射地图", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onFinalizeRejected,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("同意票未超过反对票 · 结算作废", fontSize = 12.sp)
                            }
                        }
                    }
                }

                canUnlock -> {
                    Button(
                        onClick = onUnlockAndDecrypt,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("已合规解锁 · 在地图查看解密轨迹", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupMessageBubble(msg: GroupMessage, isSelf: Boolean) {
    val kind = msg.parsedKind
    when (kind) {
        MessageKind.SYSTEM, MessageKind.RESOLUTION_ALERT -> {
            Surface(
                color = if (kind == MessageKind.RESOLUTION_ALERT) Color(0xFFFEF2F2) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg.text,
                    fontSize = 11.sp,
                    color = if (kind == MessageKind.RESOLUTION_ALERT) Color(0xFF991B1B) else Color(0xFF475569),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }

        MessageKind.CHAT -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    color = if (isSelf) Color(0xFF1677FF) else Color.White,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .width(260.dp)
                        .border(
                            width = if (isSelf) 0.dp else 1.dp,
                            color = if (isSelf) Color.Transparent else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = if (isSelf) "我" else msg.senderName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelf) Color(0xFFDBEAFE) else Color(0xFF64748B)
                        )
                        Text(
                            text = msg.text,
                            fontSize = 13.sp,
                            color = if (isSelf) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberStatusCard(
    member: GroupMember,
    isSelf: Boolean,
    trackCount: Int,
    onInitiateResolution: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (member.trackingEnabled) Color(0xFF10B981).copy(alpha = 0.14f)
                        else Color(0xFFE2E8F0),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (member.consentAccepted) Icons.Default.VerifiedUser else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (member.trackingEnabled) Color(0xFF059669) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSelf) "${member.displayName}（我）" else member.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = if (member.batteryPercent < 20) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "电量 ${member.batteryPercent}% · 密文点数 $trackCount · ${
                            if (member.trackingEnabled) "加密上报中" else "已暂停上报"
                        }",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                if (member.lastReportAtMs > 0L) {
                    Text(
                        text = "最近加密上传：${formatTimestamp(member.lastReportAtMs)}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            if (!isSelf) {
                Button(
                    onClick = onInitiateResolution,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("发起查看", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "禁止自发起",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityAuditTab(
    group: GuardianGroup,
    resolutions: List<GuardianResolution>,
    encryptedTracks: List<EncryptedTrackPoint>,
    isOwner: Boolean,
    onLeaveGroup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GppGood, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("安全与合规防滥用规则清单", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Text("• 端到端密文：Firestore 累计存储 ${encryptedTracks.size} 条位置密文，无任何明文经纬度字段。", fontSize = 11.sp, color = Color(0xFF475569))
                Text("• 禁止自我查看：数据库规则强制校验 initiatorUid != targetUid。", fontSize = 11.sp, color = Color(0xFF475569))
                Text("• 本人一票否决：状态机锁死，本人一票否决后永久转为 VETOED，任何管理员都无法解锁。", fontSize = 11.sp, color = Color(0xFF475569))
                Text("• 服务端时间门控：必须满足 request.time >= unlockAt 且赞成数大于反对数才放行托管密钥。", fontSize = 11.sp, color = Color(0xFF475569))
            }
        }

        if (resolutions.isEmpty()) {
            Text(
                text = "暂无历史查看决议审计记录。",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            resolutions.forEach { res ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${res.initiatorName} -> ${res.targetName} [${res.status}]",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "原因：${res.reason} · 赞成${res.approveUids.size}/反对${res.rejectUids.size}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        if (!isOwner) {
            OutlinedButton(
                onClick = onLeaveGroup,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出当前守护群（并移除我的云端密钥）", color = Color(0xFFDC2626), fontSize = 12.sp)
            }
        } else {
            Text(
                text = "您是群主（${group.name}），可分享邀请码 ${group.inviteCode} 邀请亲友入群共同守护。",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun EmergencyConsentDialog(
    cooldownSeconds: Long,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    var checked by remember { mutableStateOf(false) }
    val waitLabel = if (cooldownSeconds >= 60L) "${cooldownSeconds / 60} 分钟" else "${cooldownSeconds} 秒"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF1677FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("位置加密与紧急解锁授权书", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "为了保障您的个人隐私并防范偷窥或跟踪风险，开启前请明确知晓以下条款：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text("1. 端到端加密：PureNav 会在您手机本地使用 AES-256-GCM 加密位置与电量数据后再上传，服务端不持有明文轨迹。", fontSize = 12.sp, color = Color(0xFF334155))
                Text("2. 预先知情解锁规则：仅当其他群友因紧急失联发起决议、通知通知弹窗等待 $waitLabel 后，您未行使【一票否决权】，且同意票数严格大于反对票数时，申请人方可解密查看您的行程。", fontSize = 12.sp, color = Color(0xFF334155))
                Text("3. 随时撤回权：您可以随时关闭开关或一键销毁云端托管密钥，未解锁前任何人都无法查看。", fontSize = 12.sp, color = Color(0xFF334155))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { checked = !checked }
                ) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Text("我已阅读并自愿授权加入本群紧急守护机制", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = checked,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("同意并开启加密上报")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("暂不开启") }
        }
    )
}

@Composable
private fun CreateGroupDialog(
    defaultNickname: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (groupName: String, nickname: String, cooldownSeconds: Long) -> Unit
) {
    var groupName by remember { mutableStateOf("家人安全守护群") }
    var nickname by remember { mutableStateOf(defaultNickname.ifBlank { "守护者" }) }
    var cooldownSeconds by remember { mutableLongStateOf(600L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建安全守护群", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it.take(60) },
                    label = { Text("群名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(40) },
                    label = { Text("我在群里的昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("被查看人【一票否决】通知冷静期：", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        600L to "10分钟(标准)",
                        1200L to "20分钟",
                        1800L to "30分钟",
                        15L to "15秒(调试演示)"
                    ).forEach { (sec, label) ->
                        FilterChip(
                            selected = cooldownSeconds == sec,
                            onClick = { cooldownSeconds = sec },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(groupName, nickname, cooldownSeconds) },
                enabled = !isBusy && groupName.isNotBlank()
            ) {
                Text("立即创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun JoinGroupDialog(
    defaultNickname: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (inviteCode: String, nickname: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(defaultNickname.ifBlank { "成员" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("通过邀请码加入守护群", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(16) },
                    label = { Text("6位守护群邀请码") },
                    placeholder = { Text("例如：SAFE88") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(40) },
                    label = { Text("我在群里的昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code, nickname) },
                enabled = !isBusy && code.trim().length >= 4
            ) {
                Text("确认加入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun InitiateResolutionDialog(
    targetMember: GroupMember,
    cooldownSeconds: Long,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("长时间联系不上，确认出行安全") }
    val waitLabel = if (cooldownSeconds >= 60L) "${cooldownSeconds / 60} 分钟" else "${cooldownSeconds} 秒"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "发起查看「${targetMember.displayName}」行程决议",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "系统将立即向「${targetMember.displayName}」及全群推送强提醒通知。$waitLabel 后若对方未一票否决且赞成数>反对数，即可解锁解密路线。",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(200) },
                    label = { Text("紧急核实原因（2~200字）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("超过6小时失联未接电话", "户外徒步偏离预定路线", "夜间独行未按时抵达报备").forEach { preset ->
                        FilterChip(
                            selected = reason == preset,
                            onClick = { reason = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = !isBusy && reason.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF))
            ) {
                Text("发起全群表决")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun formatMmSs(totalSeconds: Long): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return "%02d:%02d".format(mins, secs)
}

private fun formatTimestamp(epochMs: Long): String {
    return try {
        SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(epochMs))
    } catch (_: Exception) {
        ""
    }
}
