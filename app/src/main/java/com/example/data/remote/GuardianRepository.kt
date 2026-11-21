package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Date

class GuardianRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // CRITICAL: Resolve R.string.firestore_database_id cleanly
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("必须先通过 Google 登录才能访问安全守护云端数据")
    }

    fun observeMyGroups(userId: String): Flow<List<GuardianGroup>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "会话已切换，拒绝跨账号读取群组" }
        val path = "guardian_groups"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GuardianGroup::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeGroupMembers(groupId: String, memberUids: List<String>): Flow<List<GroupMember>> = flow {
        val uid = requireUserId()
        if (groupId.isBlank() || memberUids.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val memberFlows = memberUids.distinct().take(30).map { mUid ->
            val docPath = "guardian_groups/$groupId/members/$mUid"
            db.collection("guardian_groups")
                .document(groupId)
                .collection("members")
                .document(mUid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(GroupMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowException(error, OperationType.GET, docPath, uid)
                    emit(null)
                }
        }
        if (memberFlows.isEmpty()) {
            emit(emptyList())
        } else {
            emitAll(
                combine(memberFlows) { array ->
                    array.filterNotNull().sortedByDescending { it.lastReportAtMs }
                }
            )
        }
    }

    fun observeGroupMessages(groupId: String, userId: String): Flow<List<GroupMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "会话已切换" }
        if (groupId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "guardian_groups/$groupId/messages"
        emitAll(
            db.collection("guardian_groups")
                .document(groupId)
                .collection("messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GroupMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeGroupResolutions(groupId: String, userId: String): Flow<List<GuardianResolution>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "会话已切换" }
        if (groupId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "guardian_groups/$groupId/resolutions"
        emitAll(
            db.collection("guardian_groups")
                .document(groupId)
                .collection("resolutions")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GuardianResolution::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeEncryptedTracks(groupId: String, userId: String): Flow<List<EncryptedTrackPoint>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "会话已切换" }
        if (groupId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "guardian_groups/$groupId/encrypted_tracks"
        emitAll(
            db.collection("guardian_groups")
                .document(groupId)
                .collection("encrypted_tracks")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EncryptedTrackPoint::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.recordedAtMs }
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createGroup(
        rawName: String,
        rawDisplayName: String,
        unlockDurationSeconds: Long,
        batteryPercent: Int
    ): Result<String> {
        var logPath = "guardian_groups"
        return try {
            val uid = requireUserId()
            val name = rawName.trim().take(60)
            require(name.isNotEmpty()) { "请输入守护群名称" }
            val displayName = rawDisplayName.trim().ifEmpty { "守护成员" }.take(50)
            val duration = unlockDurationSeconds.coerceIn(15L, 86400L)
            val bat = batteryPercent.coerceIn(0, 100).toLong()

            val groupRef = db.collection("guardian_groups").document()
            val groupId = groupRef.id
            val inviteCode = generateInviteCode()
            val inviteRef = db.collection("group_invites").document(inviteCode)
            val memberRef = groupRef.collection("members").document(uid)
            logPath = groupRef.path

            val batch = db.batch()
            batch.set(
                groupRef,
                mapOf(
                    "name" to name,
                    "inviteCode" to inviteCode,
                    "ownerUid" to uid,
                    "memberUids" to listOf(uid),
                    "unlockDurationSeconds" to duration,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                inviteRef,
                mapOf(
                    "groupId" to groupId,
                    "groupName" to name,
                    "ownerUid" to uid,
                    "unlockDurationSeconds" to duration,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                memberRef,
                mapOf(
                    "uid" to uid,
                    "displayName" to displayName,
                    "trackingEnabled" to false,
                    "consentAccepted" to false,
                    "consentVersion" to CONSENT_VERSION,
                    "batteryPercent" to bat,
                    "lastReportAtMs" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            // Post initial welcome message
            sendChatMessage(
                groupId = groupId,
                senderName = displayName,
                text = "已创建「$name」安全守护群，请先阅读并签署紧急授权协议后再开启加密定位上报。分享邀请码：$inviteCode",
                memberUids = listOf(uid),
                kind = MessageKind.SYSTEM
            )

            Result.success(groupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logPath)
            Result.failure(e)
        }
    }

    suspend fun joinGroupByInviteCode(
        rawInviteCode: String,
        rawDisplayName: String,
        batteryPercent: Int
    ): Result<String> {
        val code = rawInviteCode.trim().uppercase()
        var logPath = "group_invites/$code"
        return try {
            val uid = requireUserId()
            require(code.matches(Regex("^[A-Z0-9_\\-]{4,16}$"))) { "邀请码格式无效（需 4~16 位字母数字）" }
            val displayName = rawDisplayName.trim().ifEmpty { "新成员" }.take(50)
            val bat = batteryPercent.coerceIn(0, 100).toLong()

            val inviteSnap = db.collection("group_invites").document(code).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("未找到邀请码「$code」，请核对后重试"))
            }
            val groupId = inviteSnap.getString("groupId")
                ?: return Result.failure(IllegalStateException("邀请码数据缺失"))
            logPath = "guardian_groups/$groupId"

            val groupRef = db.collection("guardian_groups").document(groupId)
            val existingGroupSnap = try {
                groupRef.get().await()
            } catch (_: Exception) {
                null
            }
            val currentMembers = (existingGroupSnap?.get("memberUids") as? List<*>)
                ?.filterIsInstance<String>()
                ?: emptyList()

            if (uid in currentMembers) {
                return Result.success(groupId)
            }

            // Cannot read guardian_groups before joining! So update using arrayUnion + set member in one atomic WriteBatch.
            val batch = db.batch()
            batch.update(
                groupRef,
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                groupRef.collection("members").document(uid),
                mapOf(
                    "uid" to uid,
                    "displayName" to displayName,
                    "trackingEnabled" to false,
                    "consentAccepted" to false,
                    "consentVersion" to CONSENT_VERSION,
                    "batteryPercent" to bat,
                    "lastReportAtMs" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            // Now member can read updated group.memberUids and broadcast system join message
            val joinedSnap = groupRef.get().await()
            val updatedMembers = (joinedSnap.get("memberUids") as? List<*>)
                ?.filterIsInstance<String>()
                ?: listOf(uid)
            sendChatMessage(
                groupId = groupId,
                senderName = displayName,
                text = "$displayName 已通过邀请码加入安全守护群（当前共 ${updatedMembers.size} 人）",
                memberUids = updatedMembers,
                kind = MessageKind.SYSTEM
            )

            Result.success(groupId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, logPath)
            Result.failure(e)
        }
    }

    suspend fun acceptConsentAndSetTracking(
        groupId: String,
        displayName: String,
        consentAccepted: Boolean,
        trackingEnabled: Boolean,
        batteryPercent: Int,
        aesKeyB64: String
    ): Result<Unit> {
        var logPath = "guardian_groups/$groupId/members"
        return try {
            val uid = requireUserId()
            require(groupId.isNotBlank())
            val safeName = displayName.trim().ifEmpty { "守护成员" }.take(50)
            val bat = batteryPercent.coerceIn(0, 100).toLong()
            val groupRef = db.collection("guardian_groups").document(groupId)
            val memberRef = groupRef.collection("members").document(uid)
            val escrowRef = groupRef.collection("key_escrows").document(uid)
            logPath = memberRef.path

            val memberSnap = memberRef.get().await()
            if (memberSnap.exists()) {
                memberRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "consentAccepted" to consentAccepted,
                        "consentVersion" to CONSENT_VERSION,
                        "trackingEnabled" to trackingEnabled,
                        "batteryPercent" to bat,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                memberRef.set(
                    mapOf(
                        "uid" to uid,
                        "displayName" to safeName,
                        "trackingEnabled" to trackingEnabled,
                        "consentAccepted" to consentAccepted,
                        "consentVersion" to CONSENT_VERSION,
                        "batteryPercent" to bat,
                        "lastReportAtMs" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            logPath = escrowRef.path
            if (consentAccepted && trackingEnabled) {
                val escrowSnap = escrowRef.get().await()
                if (escrowSnap.exists()) {
                    escrowRef.update(
                        mapOf(
                            "escrowedAesKeyB64" to aesKeyB64,
                            "unlockedByResolutionId" to "",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    escrowRef.set(
                        mapOf(
                            "ownerUid" to uid,
                            "groupId" to groupId,
                            "escrowedAesKeyB64" to aesKeyB64,
                            "unlockedByResolutionId" to "",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else if (!consentAccepted) {
                // User withdrew consent -> remove cloud escrowed key immediately
                val escrowSnap = escrowRef.get().await()
                if (escrowSnap.exists()) {
                    escrowRef.delete().await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, logPath)
            Result.failure(e)
        }
    }

    suspend fun uploadEncryptedLocation(
        groupId: String,
        ownerName: String,
        encryptedPayload: String,
        batteryPercent: Int,
        recordedAtMs: Long,
        memberUids: List<String>
    ): Result<Unit> {
        var logPath = "guardian_groups/$groupId/encrypted_tracks"
        return try {
            val uid = requireUserId()
            require(groupId.isNotBlank() && encryptedPayload.length in 16..2048)
            val safeName = ownerName.trim().ifEmpty { "守护成员" }.take(50)
            val bat = batteryPercent.coerceIn(0, 100).toLong()
            val members = (memberUids + uid).distinct().take(30)

            val groupRef = db.collection("guardian_groups").document(groupId)
            val trackRef = groupRef.collection("encrypted_tracks").document()
            val memberRef = groupRef.collection("members").document(uid)
            logPath = trackRef.path

            val batch = db.batch()
            batch.set(
                trackRef,
                mapOf(
                    "groupId" to groupId,
                    "ownerUid" to uid,
                    "ownerName" to safeName,
                    "encryptedPayload" to encryptedPayload,
                    "batteryPercent" to bat,
                    "recordedAtMs" to recordedAtMs,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                memberRef,
                mapOf(
                    "batteryPercent" to bat,
                    "lastReportAtMs" to recordedAtMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logPath)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        groupId: String,
        senderName: String,
        text: String,
        memberUids: List<String>,
        kind: MessageKind = MessageKind.CHAT,
        relatedResolutionId: String? = null
    ): Result<Unit> {
        var logPath = "guardian_groups/$groupId/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(500)
            require(cleanText.isNotEmpty()) { "消息内容不能为空" }
            val safeName = senderName.trim().ifEmpty { "成员" }.take(50)
            val members = (memberUids + uid).distinct().take(30)

            val msgRef = db.collection("guardian_groups")
                .document(groupId)
                .collection("messages")
                .document()
            logPath = msgRef.path

            val payload = mapOf(
                "groupId" to groupId,
                "senderUid" to uid,
                "senderName" to safeName,
                "text" to cleanText,
                "kind" to kind.name,
                "relatedResolutionId" to relatedResolutionId?.takeIf { it.isNotBlank() },
                "memberUids" to members,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            msgRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logPath)
            Result.failure(e)
        }
    }

    suspend fun initiateResolution(
        group: GuardianGroup,
        initiatorName: String,
        targetMember: GroupMember,
        reason: String
    ): Result<String> {
        var logPath = "guardian_groups/${group.id}/resolutions"
        return try {
            val uid = requireUserId()
            require(uid != targetMember.uid) { "安全规则禁止对本人发起查看决议，只能由其他群友发起" }
            val cleanReason = reason.trim()
            require(cleanReason.length in 2..200) { "请填写 2~200 字的紧急情况说明" }
            val safeInitiator = initiatorName.trim().ifEmpty { "群成员" }.take(50)
            val safeTarget = targetMember.displayName.trim().ifEmpty { "被查看人" }.take(50)
            val members = (group.memberUids + uid + targetMember.uid).distinct().take(30)
            require(members.size >= 2) { "至少需要 2 位群成员才可发起决议" }

            val groupRef = db.collection("guardian_groups").document(group.id)
            val resRef = groupRef.collection("resolutions").document()
            logPath = resRef.path

            val cooldownSeconds = group.unlockDurationSeconds.coerceIn(15L, 86400L)
            val unlockDate = Date(System.currentTimeMillis() + cooldownSeconds * 1000L)

            resRef.set(
                mapOf(
                    "groupId" to group.id,
                    "initiatorUid" to uid,
                    "initiatorName" to safeInitiator,
                    "targetUid" to targetMember.uid,
                    "targetName" to safeTarget,
                    "reason" to cleanReason,
                    "status" to ResolutionStatus.VOTING.name,
                    "approveUids" to listOf(uid),
                    "rejectUids" to emptyList<String>(),
                    "targetVetoed" to false,
                    "memberUids" to members,
                    "unlockAt" to Timestamp(unlockDate),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val waitLabel = if (cooldownSeconds >= 60L) "${cooldownSeconds / 60} 分钟" else "${cooldownSeconds} 秒"
            sendChatMessage(
                groupId = group.id,
                senderName = safeInitiator,
                text = "【紧急决议通知 @$safeTarget】$safeInitiator 申请查看「$safeTarget」的加密行程（原因：$cleanReason）。本人拥有【一票否决权】，若 $waitLabel 后本人未拒绝且同意数大于反对数将自动解锁。",
                memberUids = members,
                kind = MessageKind.RESOLUTION_ALERT,
                relatedResolutionId = resRef.id
            )

            Result.success(resRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logPath)
            Result.failure(e)
        }
    }

    suspend fun voteResolution(
        groupId: String,
        resolution: GuardianResolution,
        approve: Boolean
    ): Result<Unit> {
        val docPath = "guardian_groups/$groupId/resolutions/${resolution.id}"
        return try {
            val uid = requireUserId()
            require(uid != resolution.targetUid) { "被查看人请直接使用【一票否决】或等待结果" }
            require(resolution.parsedStatus == ResolutionStatus.VOTING) { "该决议已结束" }

            val updates = if (approve) {
                mapOf(
                    "approveUids" to FieldValue.arrayUnion(uid),
                    "rejectUids" to FieldValue.arrayRemove(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "approveUids" to FieldValue.arrayRemove(uid),
                    "rejectUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            db.collection("guardian_groups")
                .document(groupId)
                .collection("resolutions")
                .document(resolution.id)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun vetoResolution(
        groupId: String,
        resolution: GuardianResolution,
        targetDisplayName: String
    ): Result<Unit> {
        val docPath = "guardian_groups/$groupId/resolutions/${resolution.id}"
        return try {
            val uid = requireUserId()
            require(uid == resolution.targetUid) { "仅被查看人本人可执行【一票否决】" }
            require(resolution.parsedStatus == ResolutionStatus.VOTING) { "该决议已结束" }

            db.collection("guardian_groups")
                .document(groupId)
                .collection("resolutions")
                .document(resolution.id)
                .update(
                    mapOf(
                        "targetVetoed" to true,
                        "status" to ResolutionStatus.VETOED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            sendChatMessage(
                groupId = groupId,
                senderName = targetDisplayName,
                text = "「$targetDisplayName」已行使【一票否决权】，本次位置查看决议已即时作废并锁定密钥。",
                memberUids = resolution.memberUids,
                kind = MessageKind.SYSTEM,
                relatedResolutionId = resolution.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unlockAndFetchEscrowKey(
        groupId: String,
        resolution: GuardianResolution
    ): Result<String> {
        val resRef = db.collection("guardian_groups")
            .document(groupId)
            .collection("resolutions")
            .document(resolution.id)
        val escrowRef = db.collection("guardian_groups")
            .document(groupId)
            .collection("key_escrows")
            .document(resolution.targetUid)
        var logPath = resRef.path
        return try {
            val uid = requireUserId()
            require(!resolution.targetVetoed) { "被查看人已一票否决，严禁解密" }
            require(resolution.approveUids.size > resolution.rejectUids.size) {
                "同意票数（${resolution.approveUids.size}）未大于反对票数（${resolution.rejectUids.size}），无法解锁"
            }

            // 1. Ensure resolution reaches UNLOCKED on server (rule verifies request.time >= unlockAt)
            if (resolution.parsedStatus == ResolutionStatus.VOTING) {
                require(uid != resolution.targetUid) { "需由其他群友触发解锁" }
                resRef.update(
                    mapOf(
                        "status" to ResolutionStatus.UNLOCKED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                require(resolution.parsedStatus == ResolutionStatus.UNLOCKED) { "当前状态不可解密：${resolution.status}" }
            }

            // 2. Link resolution ID on target's key_escrows document if calling user is not target
            logPath = escrowRef.path
            if (uid != resolution.targetUid) {
                try {
                    escrowRef.update(
                        mapOf(
                            "unlockedByResolutionId" to resolution.id,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: Exception) {
                    // If already unlocked by another member, `get()` below will still succeed; otherwise rethrow.
                    handleFirestoreError(e, OperationType.UPDATE, logPath)
                }
            }

            // 3. Read escrowed AES-256 key (gated on server by unlockedByResolutionId -> UNLOCKED)
            val escrowSnap = escrowRef.get().await()
            if (!escrowSnap.exists()) {
                return Result.failure(IllegalStateException("对方尚未签署授权并上传托管密钥，暂无可解密密文"))
            }
            val keyB64 = escrowSnap.getString("escrowedAesKeyB64")
                ?: return Result.failure(IllegalStateException("云端密钥数据异常"))
            Result.success(keyB64)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, logPath)
            Result.failure(e)
        }
    }

    suspend fun markResolutionRejected(
        groupId: String,
        resolution: GuardianResolution
    ): Result<Unit> {
        val docPath = "guardian_groups/$groupId/resolutions/${resolution.id}"
        return try {
            requireUserId()
            require(resolution.parsedStatus == ResolutionStatus.VOTING)
            require(resolution.approveUids.size <= resolution.rejectUids.size)
            db.collection("guardian_groups")
                .document(groupId)
                .collection("resolutions")
                .document(resolution.id)
                .update(
                    mapOf(
                        "status" to ResolutionStatus.REJECTED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> {
        val docPath = "guardian_groups/$groupId"
        return try {
            val uid = requireUserId()
            val groupRef = db.collection("guardian_groups").document(groupId)
            groupRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            try {
                groupRef.collection("members").document(uid).delete().await()
            } catch (_: Exception) {}
            try {
                groupRef.collection("key_escrows").document(uid).delete().await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun handleFlowException(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        return buildString(6) {
            repeat(6) { append(chars[random.nextInt(chars.length)]) }
        }
    }

    companion object {
        const val CONSENT_VERSION = "v1.0-cn-safety"
    }
}
