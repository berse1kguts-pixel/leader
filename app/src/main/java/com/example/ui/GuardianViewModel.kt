package com.example.ui

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.DecryptedTrackOverlay
import com.example.data.remote.EncryptedTrackPoint
import com.example.data.remote.GroupMember
import com.example.data.remote.GroupMessage
import com.example.data.remote.GuardianGroup
import com.example.data.remote.GuardianRepository
import com.example.data.remote.GuardianResolution
import com.example.data.remote.ResolutionStatus
import com.example.security.GuardianCrypto
import com.example.sensor.GpsAndCompassTracker
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface GuardianUiState<out T> {
    data object Loading : GuardianUiState<Nothing>
    data class Success<T>(val data: T) : GuardianUiState<T>
    data class Error(val message: String) : GuardianUiState<Nothing>
}

data class GuardianPanelState(
    val selectedGroupId: String? = null,
    val showGuardianSheet: Boolean = false,
    val showConsentDialog: Boolean = false,
    val showCreateGroupDialog: Boolean = false,
    val showJoinGroupDialog: Boolean = false,
    val initiateTargetMember: GroupMember? = null,
    val latestCiphertextPreview: String = "",
    val lastUploadTimestampMs: Long = 0L,
    val decryptedOverlay: DecryptedTrackOverlay? = null,
    val isBusy: Boolean = false,
    val feedbackBanner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class GuardianViewModel(
    private val repository: GuardianRepository,
    private val crypto: GuardianCrypto,
    private val sensorTracker: GpsAndCompassTracker,
    val currentUserId: String,
    val currentUserDisplayName: String,
    private val batteryPercentReader: () -> Int
) : ViewModel() {

    private val _panelState = MutableStateFlow(GuardianPanelState())
    val panelState: StateFlow<GuardianPanelState> = _panelState.asStateFlow()

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    val groupsState: StateFlow<GuardianUiState<List<GuardianGroup>>> =
        repository.observeMyGroups(currentUserId)
            .map<List<GuardianGroup>, GuardianUiState<List<GuardianGroup>>> { groups ->
                val currentSelected = _panelState.value.selectedGroupId
                if (currentSelected == null || groups.none { it.id == currentSelected }) {
                    _panelState.value = _panelState.value.copy(
                        selectedGroupId = groups.firstOrNull()?.id
                    )
                }
                GuardianUiState.Success(groups)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Failed observing guardian groups", root)
                emit(GuardianUiState.Error(root.message ?: "读取安全守护群失败"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = GuardianUiState.Loading
            )

    private val activeGroupFlow = combine(groupsState, _panelState) { state, panel ->
        val groups = (state as? GuardianUiState.Success)?.data ?: emptyList()
        groups.firstOrNull { it.id == panel.selectedGroupId } ?: groups.firstOrNull()
    }.distinctUntilChanged()

    val activeGroup: StateFlow<GuardianGroup?> = activeGroupFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    val members: StateFlow<List<GroupMember>> = activeGroupFlow
        .flatMapLatest { grp ->
            if (grp == null) flowOf(emptyList())
            else repository.observeGroupMembers(grp.id, grp.memberUids)
        }
        .catch { error ->
            unwrapOrThrowCancellation(error, "members")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val messages: StateFlow<List<GroupMessage>> = activeGroupFlow
        .flatMapLatest { grp ->
            if (grp == null) flowOf(emptyList())
            else repository.observeGroupMessages(grp.id, currentUserId)
        }
        .catch { error ->
            unwrapOrThrowCancellation(error, "messages")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val resolutions: StateFlow<List<GuardianResolution>> = activeGroupFlow
        .flatMapLatest { grp ->
            if (grp == null) flowOf(emptyList())
            else repository.observeGroupResolutions(grp.id, currentUserId)
        }
        .catch { error ->
            unwrapOrThrowCancellation(error, "resolutions")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val encryptedTracks: StateFlow<List<EncryptedTrackPoint>> = activeGroupFlow
        .flatMapLatest { grp ->
            if (grp == null) flowOf(emptyList())
            else repository.observeEncryptedTracks(grp.id, currentUserId)
        }
        .catch { error ->
            unwrapOrThrowCancellation(error, "tracks")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private var heartbeatJob: Job? = null

    init {
        // 1-second clock ticker for live resolution countdowns
        viewModelScope.launch {
            while (isActive) {
                _nowMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }

        // Continuous encrypted reporting while member has opted-in with active consent
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                val grp = activeGroup.value
                val selfMember = members.value.firstOrNull { it.uid == currentUserId }
                if (grp != null && selfMember != null && selfMember.consentAccepted && selfMember.trackingEnabled) {
                    uploadEncryptedLocationInternal(grp, showToast = false)
                }
            }
        }
    }

    fun openGuardianSheet(open: Boolean) {
        _panelState.value = _panelState.value.copy(showGuardianSheet = open)
    }

    fun selectGroup(groupId: String) {
        _panelState.value = _panelState.value.copy(selectedGroupId = groupId)
    }

    fun showCreateGroupDialog(show: Boolean) {
        _panelState.value = _panelState.value.copy(showCreateGroupDialog = show)
    }

    fun showJoinGroupDialog(show: Boolean) {
        _panelState.value = _panelState.value.copy(showJoinGroupDialog = show)
    }

    fun showConsentDialog(show: Boolean) {
        _panelState.value = _panelState.value.copy(showConsentDialog = show)
    }

    fun openInitiateResolutionFor(member: GroupMember?) {
        _panelState.value = _panelState.value.copy(initiateTargetMember = member)
    }

    fun clearFeedbackBanner() {
        _panelState.value = _panelState.value.copy(feedbackBanner = null)
    }

    fun clearDecryptedOverlay() {
        _panelState.value = _panelState.value.copy(decryptedOverlay = null)
    }

    fun createGroup(groupName: String, nickname: String, cooldownSeconds: Long) {
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true, feedbackBanner = null)
            val result = repository.createGroup(
                rawName = groupName,
                rawDisplayName = nickname.ifBlank { currentUserDisplayName },
                unlockDurationSeconds = cooldownSeconds,
                batteryPercent = batteryPercentReader()
            )
            _panelState.value = result.fold(
                onSuccess = { newId ->
                    _panelState.value.copy(
                        isBusy = false,
                        selectedGroupId = newId,
                        showCreateGroupDialog = false,
                        feedbackBanner = "守护群已创建，请将邀请码发给家人/队友加入"
                    )
                },
                onFailure = { err ->
                    _panelState.value.copy(
                        isBusy = false,
                        feedbackBanner = "创建失败：${err.localizedMessage ?: "请检查网络或输入"}"
                    )
                }
            )
        }
    }

    fun joinGroup(inviteCode: String, nickname: String) {
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true, feedbackBanner = null)
            val result = repository.joinGroupByInviteCode(
                rawInviteCode = inviteCode,
                rawDisplayName = nickname.ifBlank { currentUserDisplayName },
                batteryPercent = batteryPercentReader()
            )
            _panelState.value = result.fold(
                onSuccess = { grpId ->
                    _panelState.value.copy(
                        isBusy = false,
                        selectedGroupId = grpId,
                        showJoinGroupDialog = false,
                        feedbackBanner = "已成功加入守护群！请授权后开启加密上报"
                    )
                },
                onFailure = { err ->
                    _panelState.value.copy(
                        isBusy = false,
                        feedbackBanner = "加入失败：${err.localizedMessage ?: "邀请码无效"}"
                    )
                }
            )
        }
    }

    fun confirmConsentAndEnableTracking() {
        val grp = activeGroup.value ?: return
        val myName = members.value.firstOrNull { it.uid == currentUserId }?.displayName
            ?: currentUserDisplayName
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true)
            val keyB64 = crypto.getOrCreateUserKeyB64(currentUserId)
            val res = repository.acceptConsentAndSetTracking(
                groupId = grp.id,
                displayName = myName,
                consentAccepted = true,
                trackingEnabled = true,
                batteryPercent = batteryPercentReader(),
                aesKeyB64 = keyB64
            )
            if (res.isSuccess) {
                _panelState.value = _panelState.value.copy(
                    isBusy = false,
                    showConsentDialog = false,
                    feedbackBanner = "已开启 AES-256-GCM 端到端加密位置上报"
                )
                uploadEncryptedLocationInternal(grp, showToast = false)
            } else {
                _panelState.value = _panelState.value.copy(
                    isBusy = false,
                    feedbackBanner = "开启失败：${res.exceptionOrNull()?.localizedMessage ?: "重试"}"
                )
            }
        }
    }

    fun toggleTrackingPaused(enable: Boolean) {
        val grp = activeGroup.value ?: return
        val selfMember = members.value.firstOrNull { it.uid == currentUserId }
        if (enable && (selfMember == null || !selfMember.consentAccepted)) {
            showConsentDialog(true)
            return
        }
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true)
            val keyB64 = crypto.getOrCreateUserKeyB64(currentUserId)
            val res = repository.acceptConsentAndSetTracking(
                groupId = grp.id,
                displayName = selfMember?.displayName ?: currentUserDisplayName,
                consentAccepted = true,
                trackingEnabled = enable,
                batteryPercent = batteryPercentReader(),
                aesKeyB64 = keyB64
            )
            _panelState.value = _panelState.value.copy(
                isBusy = false,
                feedbackBanner = if (res.isSuccess) {
                    if (enable) "已恢复端到端加密上报" else "已暂停位置上报（本人隐私优先）"
                } else {
                    "操作失败：${res.exceptionOrNull()?.localizedMessage ?: ""}"
                }
            )
            if (enable && res.isSuccess) {
                uploadEncryptedLocationInternal(grp, showToast = false)
            }
        }
    }

    fun revokeConsentAndDeleteKey() {
        val grp = activeGroup.value ?: return
        val selfMember = members.value.firstOrNull { it.uid == currentUserId }
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true)
            val res = repository.acceptConsentAndSetTracking(
                groupId = grp.id,
                displayName = selfMember?.displayName ?: currentUserDisplayName,
                consentAccepted = false,
                trackingEnabled = false,
                batteryPercent = batteryPercentReader(),
                aesKeyB64 = ""
            )
            crypto.rotateUserKeyB64(currentUserId)
            _panelState.value = _panelState.value.copy(
                isBusy = false,
                feedbackBanner = if (res.isSuccess) "已撤回授权并销毁云端托管密钥" else "撤回失败"
            )
        }
    }

    fun uploadEncryptedLocationNow() {
        val grp = activeGroup.value ?: return
        val selfMember = members.value.firstOrNull { it.uid == currentUserId }
        if (selfMember == null || !selfMember.consentAccepted) {
            showConsentDialog(true)
            return
        }
        viewModelScope.launch {
            uploadEncryptedLocationInternal(grp, showToast = true)
        }
    }

    private suspend fun uploadEncryptedLocationInternal(grp: GuardianGroup, showToast: Boolean) {
        val tel = sensorTracker.telemetry.value
        val bat = batteryPercentReader()
        val keyB64 = crypto.getOrCreateUserKeyB64(currentUserId)
        val now = System.currentTimeMillis()
        val cipher = crypto.encryptLocationPayload(
            keyB64 = keyB64,
            point = tel.currentWgs84,
            speedKmh = tel.speedKmh,
            accuracyMeters = tel.accuracyMeters,
            batteryPercent = bat,
            recordedAtMs = now,
            travelMode = if (tel.speedKmh > 12f) "DRIVING" else "WALKING"
        )
        val myName = members.value.firstOrNull { it.uid == currentUserId }?.displayName
            ?: currentUserDisplayName
        val res = repository.uploadEncryptedLocation(
            groupId = grp.id,
            ownerName = myName,
            encryptedPayload = cipher,
            batteryPercent = bat,
            recordedAtMs = now,
            memberUids = grp.memberUids
        )
        if (res.isSuccess) {
            _panelState.value = _panelState.value.copy(
                latestCiphertextPreview = cipher,
                lastUploadTimestampMs = now,
                feedbackBanner = if (showToast) "已生成并上传密文定位（服务端仅存 AES-256 密文）" else _panelState.value.feedbackBanner
            )
        } else if (showToast) {
            _panelState.value = _panelState.value.copy(
                feedbackBanner = "上报失败：${res.exceptionOrNull()?.localizedMessage ?: "请重试"}"
            )
        }
    }

    fun sendGroupMessage(text: String) {
        val grp = activeGroup.value ?: return
        val myName = members.value.firstOrNull { it.uid == currentUserId }?.displayName
            ?: currentUserDisplayName
        viewModelScope.launch {
            val res = repository.sendChatMessage(
                groupId = grp.id,
                senderName = myName,
                text = text,
                memberUids = grp.memberUids
            )
            if (res.isFailure) {
                _panelState.value = _panelState.value.copy(
                    feedbackBanner = "发送失败：${res.exceptionOrNull()?.localizedMessage ?: ""}"
                )
            }
        }
    }

    fun submitInitiateResolution(targetMember: GroupMember, reason: String) {
        val grp = activeGroup.value ?: return
        val myName = members.value.firstOrNull { it.uid == currentUserId }?.displayName
            ?: currentUserDisplayName
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true)
            val res = repository.initiateResolution(
                group = grp,
                initiatorName = myName,
                targetMember = targetMember,
                reason = reason
            )
            _panelState.value = res.fold(
                onSuccess = {
                    _panelState.value.copy(
                        isBusy = false,
                        initiateTargetMember = null,
                        feedbackBanner = "已发起查看「${targetMember.displayName}」决议，通知已送达群内及本人"
                    )
                },
                onFailure = { err ->
                    _panelState.value.copy(
                        isBusy = false,
                        feedbackBanner = "发起失败：${err.localizedMessage ?: "不满足发起条件"}"
                    )
                }
            )
        }
    }

    fun castVote(resolution: GuardianResolution, approve: Boolean) {
        val grp = activeGroup.value ?: return
        viewModelScope.launch {
            val res = repository.voteResolution(grp.id, resolution, approve)
            if (res.isFailure) {
                _panelState.value = _panelState.value.copy(
                    feedbackBanner = "投票失败：${res.exceptionOrNull()?.localizedMessage ?: ""}"
                )
            }
        }
    }

    fun exerciseOneVoteVeto(resolution: GuardianResolution) {
        val grp = activeGroup.value ?: return
        val myName = members.value.firstOrNull { it.uid == currentUserId }?.displayName
            ?: currentUserDisplayName
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true)
            val res = repository.vetoResolution(grp.id, resolution, myName)
            _panelState.value = _panelState.value.copy(
                isBusy = false,
                feedbackBanner = if (res.isSuccess) {
                    "您已行使【一票否决权】，该查看请求已永久作废！"
                } else {
                    "操作失败：${res.exceptionOrNull()?.localizedMessage ?: ""}"
                }
            )
        }
    }

    fun finalizeRejected(resolution: GuardianResolution) {
        val grp = activeGroup.value ?: return
        viewModelScope.launch {
            val res = repository.markResolutionRejected(grp.id, resolution)
            if (res.isSuccess) {
                _panelState.value = _panelState.value.copy(feedbackBanner = "倒计时结束，同意票数未占优，决议已否决")
            }
        }
    }

    fun unlockAndDecryptTargetRoute(resolution: GuardianResolution) {
        val grp = activeGroup.value ?: return
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true)
            val keyResult = repository.unlockAndFetchEscrowKey(grp.id, resolution)
            keyResult.fold(
                onSuccess = { keyB64 ->
                    val targetCipherTracks = encryptedTracks.value
                        .filter { it.ownerUid == resolution.targetUid }
                        .sortedBy { it.recordedAtMs }
                    val decryptedSamples = targetCipherTracks.mapNotNull { track ->
                        crypto.decryptLocationPayload(keyB64, track.encryptedPayload)
                    }
                    if (decryptedSamples.isEmpty()) {
                        _panelState.value = _panelState.value.copy(
                            isBusy = false,
                            feedbackBanner = "决议已解锁密钥，但「${resolution.targetName}」近期暂无已上传密文轨迹点"
                        )
                    } else {
                        _panelState.value = _panelState.value.copy(
                            isBusy = false,
                            showGuardianSheet = false,
                            decryptedOverlay = DecryptedTrackOverlay(
                                resolutionId = resolution.id,
                                targetUid = resolution.targetUid,
                                targetName = resolution.targetName,
                                samples = decryptedSamples
                            ),
                            feedbackBanner = "已端到端解密「${resolution.targetName}」的 ${decryptedSamples.size} 个位置坐标并投射到地图"
                        )
                    }
                },
                onFailure = { err ->
                    _panelState.value = _panelState.value.copy(
                        isBusy = false,
                        feedbackBanner = "解锁解密失败：${err.localizedMessage ?: "尚未满足服务端时间/投票门控"}"
                    )
                }
            )
        }
    }

    fun leaveActiveGroup() {
        val grp = activeGroup.value ?: return
        viewModelScope.launch {
            _panelState.value = _panelState.value.copy(isBusy = true)
            val res = repository.leaveGroup(grp.id)
            _panelState.value = _panelState.value.copy(
                isBusy = false,
                feedbackBanner = if (res.isSuccess) "已退出守护群" else "退出失败：群主请勿直接退群"
            )
        }
    }

    private fun unwrapOrThrowCancellation(error: Throwable, tag: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Stream error ($tag)", firestoreError ?: error)
    }

    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
    }

    companion object {
        private const val TAG = "GuardianVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun readBatteryPercent(context: Context): Int {
            return try {
                val bm = context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                if (level in 0..100) level else 100
            } catch (_: Exception) {
                100
            }
        }
    }
}
