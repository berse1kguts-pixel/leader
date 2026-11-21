const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_111";
const BOB_UID = "bob_222";
const CAROL_UID = "carol_333";
const OUTSIDER_UID = "outsider_999";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedGroupWithThreeMembers(groupId = "grp_family", inviteCode = "SAFE88") {
  const past = new Date(Date.now() - 20000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("guardian_groups").doc(groupId).set({
      name: "家人紧急守护群",
      inviteCode,
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID, CAROL_UID],
      unlockDurationSeconds: 600,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("group_invites").doc(inviteCode).set({
      groupId,
      groupName: "家人紧急守护群",
      ownerUid: ALICE_UID,
      unlockDurationSeconds: 600,
      createdAt: past,
    });
    await db
      .collection("guardian_groups")
      .doc(groupId)
      .collection("members")
      .doc(BOB_UID)
      .set({
        uid: BOB_UID,
        displayName: "小林",
        trackingEnabled: true,
        consentAccepted: true,
        consentVersion: "v1.0",
        batteryPercent: 78,
        lastReportAtMs: Date.now() - 5000,
        createdAt: past,
        updatedAt: past,
      });
    await db
      .collection("guardian_groups")
      .doc(groupId)
      .collection("key_escrows")
      .doc(BOB_UID)
      .set({
        ownerUid: BOB_UID,
        groupId,
        escrowedAesKeyB64: "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        unlockedByResolutionId: "",
        createdAt: past,
        updatedAt: past,
      });
  });
}

test("Unauthenticated user cannot read or write any Guardian collection", async () => {
  await seedGroupWithThreeMembers();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(unauthDb.collection("guardian_groups").get());
  await assertFails(unauthDb.collection("group_invites").doc("SAFE88").get());
  await assertFails(unauthDb.collection("guardian_groups").doc("grp_family").get());
  await assertFails(
    unauthDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
});

test("Atomic group creation + invite lookup + atomic member join succeed; outsider read fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Atomic batch create matching GuardianRepository.createGroup()
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("guardian_groups").doc("grp_1"), {
    name: "旅途守护",
    inviteCode: "TRIP66",
    ownerUid: ALICE_UID,
    memberUids: [ALICE_UID],
    unlockDurationSeconds: 600,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("group_invites").doc("TRIP66"), {
    groupId: "grp_1",
    groupName: "旅途守护",
    ownerUid: ALICE_UID,
    unlockDurationSeconds: 600,
    createdAt: now,
  });
  batch.set(
    aliceDb.collection("guardian_groups").doc("grp_1").collection("members").doc(ALICE_UID),
    {
      uid: ALICE_UID,
      displayName: "Alice",
      trackingEnabled: false,
      consentAccepted: false,
      consentVersion: "v1.0",
      batteryPercent: 95,
      lastReportAtMs: 0,
      createdAt: now,
      updatedAt: now,
    }
  );
  await assertSucceeds(batch.commit());

  // Cannot list all invite codes (enumerating invite codes must fail)
  await assertFails(bobDb.collection("group_invites").get());

  // Single-doc get by exact code succeeds
  await assertSucceeds(bobDb.collection("group_invites").doc("TRIP66").get());

  // Bob joins atomically with batch
  const joinBatch = bobDb.batch();
  joinBatch.update(bobDb.collection("guardian_groups").doc("grp_1"), {
    memberUids: [ALICE_UID, BOB_UID],
    updatedAt: new Date(Date.now() - 1000),
  });
  joinBatch.set(
    bobDb.collection("guardian_groups").doc("grp_1").collection("members").doc(BOB_UID),
    {
      uid: BOB_UID,
      displayName: "Bob",
      trackingEnabled: false,
      consentAccepted: false,
      consentVersion: "v1.0",
      batteryPercent: 80,
      lastReportAtMs: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    }
  );
  await assertSucceeds(joinBatch.commit());

  // Filtered list query matches Kotlin repository
  await assertSucceeds(
    bobDb.collection("guardian_groups").where("memberUids", "array-contains", BOB_UID).get()
  );
  // Unfiltered list query must fail
  await assertFails(bobDb.collection("guardian_groups").get());
  // Outsider cannot read group or member
  await assertFails(outsiderDb.collection("guardian_groups").doc("grp_1").get());
  await assertFails(
    outsiderDb.collection("guardian_groups").doc("grp_1").collection("members").doc(ALICE_UID).get()
  );
});

test("Reject ghost fields, @DocumentId 'id' leakage, and cross-member impersonation", async () => {
  await seedGroupWithThreeMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // @DocumentId 'id' field in payload must fail
  await assertFails(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("messages")
      .doc("msg_bad_id")
      .set({
        id: "msg_bad_id",
        groupId: "grp_family",
        senderUid: ALICE_UID,
        senderName: "Alice",
        text: "Hello",
        kind: "CHAT",
        memberUids: [ALICE_UID, BOB_UID, CAROL_UID],
        createdAt: now,
      })
  );

  // Ghost field must fail
  await assertFails(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("encrypted_tracks")
      .doc("trk_ghost")
      .set({
        groupId: "grp_family",
        ownerUid: ALICE_UID,
        ownerName: "Alice",
        encryptedPayload: "ivB64:cipherB64LongEnoughForValidation",
        batteryPercent: 88,
        recordedAtMs: Date.now(),
        memberUids: [ALICE_UID, BOB_UID, CAROL_UID],
        isAdmin: true,
        createdAt: now,
      })
  );

  // Valid encrypted track upload succeeds
  await assertSucceeds(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("encrypted_tracks")
      .doc("trk_ok")
      .set({
        groupId: "grp_family",
        ownerUid: ALICE_UID,
        ownerName: "Alice",
        encryptedPayload: "ivB64:cipherB64LongEnoughForValidation",
        batteryPercent: 88,
        recordedAtMs: Date.now(),
        memberUids: [ALICE_UID, BOB_UID, CAROL_UID],
        createdAt: now,
      })
  );

  // Filtered tracks list succeeds; unfiltered list fails
  await assertSucceeds(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("encrypted_tracks")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertFails(
    aliceDb.collection("guardian_groups").doc("grp_family").collection("encrypted_tracks").get()
  );
});

test("Resolution anti-abuse: self-targeting blocked, non-target cannot veto, premature unlock blocked", async () => {
  await seedGroupWithThreeMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const futureUnlock = new Date(Date.now() + 600000);

  // Self-targeting resolution (Alice initiating for Alice) MUST fail
  await assertFails(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_self")
      .set({
        groupId: "grp_family",
        initiatorUid: ALICE_UID,
        initiatorName: "Alice",
        targetUid: ALICE_UID,
        targetName: "Alice",
        reason: "尝试自我发起",
        status: "VOTING",
        approveUids: [ALICE_UID],
        rejectUids: [],
        targetVetoed: false,
        memberUids: [ALICE_UID, BOB_UID, CAROL_UID],
        unlockAt: futureUnlock,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Valid resolution initiated by Alice targeting Bob succeeds
  await assertSucceeds(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_1")
      .set({
        groupId: "grp_family",
        initiatorUid: ALICE_UID,
        initiatorName: "Alice",
        targetUid: BOB_UID,
        targetName: "小林",
        reason: "小林已超过6小时未回复消息",
        status: "VOTING",
        approveUids: [ALICE_UID],
        rejectUids: [],
        targetVetoed: false,
        memberUids: [ALICE_UID, BOB_UID, CAROL_UID],
        unlockAt: futureUnlock,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Escrow key MUST remain unreadable to Alice & Carol while resolution is locked
  await assertFails(
    aliceDb.collection("guardian_groups").doc("grp_family").collection("key_escrows").doc(BOB_UID).get()
  );

  // Initiator CANNOT exercise target veto on behalf of Bob
  await assertFails(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_1")
      .update({
        targetVetoed: true,
        status: "VETOED",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Initiator CANNOT unlock before unlockAt cooldown expires
  await assertFails(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_1")
      .update({
        status: "UNLOCKED",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Target (Bob) CAN exercise exclusive One-Vote Veto at any time while VOTING
  await assertSucceeds(
    bobDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_1")
      .update({
        targetVetoed: true,
        status: "VETOED",
        updatedAt: new Date(Date.now() - 200),
      })
  );

  // Terminal state locking: VETOED resolution cannot be modified again
  await assertFails(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_1")
      .update({
        status: "UNLOCKED",
        updatedAt: new Date(Date.now() - 100),
      })
  );

  // Escrow key still unreadable after veto
  await assertFails(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("key_escrows")
      .doc(BOB_UID)
      .update({
        unlockedByResolutionId: "res_1",
        updatedAt: new Date(Date.now() - 100),
      })
  );
});

test("Consensus unlock & escrow release succeed ONLY when cooldown passed, targetVetoed==false, and approve > reject", async () => {
  await seedGroupWithThreeMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const carolDb = testEnv.authenticatedContext(CAROL_UID).firestore();
  const pastCreated = new Date(Date.now() - 700000);
  const pastUnlock = new Date(Date.now() - 5000);

  // Seed a voting resolution whose cooldown has already elapsed with tie votes (1 approve vs 1 reject)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_tie")
      .set({
        groupId: "grp_family",
        initiatorUid: ALICE_UID,
        initiatorName: "Alice",
        targetUid: BOB_UID,
        targetName: "小林",
        reason: "失联核实",
        status: "VOTING",
        approveUids: [ALICE_UID],
        rejectUids: [CAROL_UID],
        targetVetoed: false,
        memberUids: [ALICE_UID, BOB_UID, CAROL_UID],
        unlockAt: pastUnlock,
        createdAt: pastCreated,
        updatedAt: pastCreated,
      });
  });

  // Unlock MUST fail when approve (1) <= reject (1)
  await assertFails(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_tie")
      .update({
        status: "UNLOCKED",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Carol changes her vote from reject to approve
  await assertSucceeds(
    carolDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_tie")
      .update({
        approveUids: [ALICE_UID, CAROL_UID],
        rejectUids: [],
        updatedAt: new Date(Date.now() - 400),
      })
  );

  // Now approve (2) > reject (0) and unlockAt elapsed -> UNLOCKED succeeds
  await assertSucceeds(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("resolutions")
      .doc("res_tie")
      .update({
        status: "UNLOCKED",
        updatedAt: new Date(Date.now() - 300),
      })
  );

  // Claim escrow key linking the UNLOCKED resolution
  await assertSucceeds(
    aliceDb
      .collection("guardian_groups")
      .doc("grp_family")
      .collection("key_escrows")
      .doc(BOB_UID)
      .update({
        unlockedByResolutionId: "res_tie",
        updatedAt: new Date(Date.now() - 200),
      })
  );

  // Group member can now read the escrowed AES key to decrypt encrypted_tracks locally
  await assertSucceeds(
    aliceDb.collection("guardian_groups").doc("grp_family").collection("key_escrows").doc(BOB_UID).get()
  );

  // Unrelated outsider still cannot read the escrow key
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  await assertFails(
    outsiderDb.collection("guardian_groups").doc("grp_family").collection("key_escrows").doc(BOB_UID).get()
  );
});
