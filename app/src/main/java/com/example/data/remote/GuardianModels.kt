package com.example.data.remote

import androidx.annotation.Keep
import com.example.security.DecryptedLocationSample
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MessageKind {
    CHAT,
    SYSTEM,
    RESOLUTION_ALERT;

    companion object {
        fun safeFrom(raw: String): MessageKind =
            enumValues<MessageKind>().firstOrNull { it.name == raw } ?: CHAT
    }
}

enum class ResolutionStatus {
    VOTING,
    VETOED,
    REJECTED,
    UNLOCKED;

    companion object {
        fun safeFrom(raw: String): ResolutionStatus =
            enumValues<ResolutionStatus>().firstOrNull { it.name == raw } ?: VOTING
    }
}

@Keep
@IgnoreExtraProperties
data class GroupInvite(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val ownerUid: String = "",
    val unlockDurationSeconds: Long = 600L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GuardianGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val unlockDurationSeconds: Long = 600L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMember(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val trackingEnabled: Boolean = false,
    val consentAccepted: Boolean = false,
    val consentVersion: String = "v1.0",
    val batteryPercent: Long = 100L,
    val lastReportAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMessage(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val kind: String = MessageKind.CHAT.name,
    val relatedResolutionId: String? = null,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val parsedKind: MessageKind get() = MessageKind.safeFrom(kind)
}

@Keep
@IgnoreExtraProperties
data class GuardianResolution(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val initiatorUid: String = "",
    val initiatorName: String = "",
    val targetUid: String = "",
    val targetName: String = "",
    val reason: String = "",
    val status: String = ResolutionStatus.VOTING.name,
    val approveUids: List<String> = emptyList(),
    val rejectUids: List<String> = emptyList(),
    val targetVetoed: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val unlockAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: ResolutionStatus get() = ResolutionStatus.safeFrom(status)
}

@Keep
@IgnoreExtraProperties
data class KeyEscrow(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val groupId: String = "",
    val escrowedAesKeyB64: String = "",
    val unlockedByResolutionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EncryptedTrackPoint(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val encryptedPayload: String = "",
    val batteryPercent: Long = 100L,
    val recordedAtMs: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DecryptedTrackOverlay(
    val resolutionId: String,
    val targetUid: String,
    val targetName: String,
    val samples: List<DecryptedLocationSample>
)
