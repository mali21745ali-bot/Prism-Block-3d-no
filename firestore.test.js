const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const SUB_ADMIN_UID = "subadmin_uid_003";
const SUPER_ADMIN_UID = "superadmin_uid_000";
const SUPER_ADMIN_EMAIL = "mali21745ali@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowPast(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function makeInitialUserPayload(uid, email = "alice@example.com", name = "Alice") {
  const ts = nowPast(5000);
  return {
    userId: uid,
    displayName: name,
    email,
    photoUrl: "",
    blocked: false,
    selectedThemeId: "simple_classic",
    unlockedThemeIds: ["simple_classic"],
    classicHighScore: 0,
    totalLinesCleared: 0,
    totalGamesPlayed: 0,
    currentLevel: 1,
    currentLevelLines: 0,
    completedLevelsCount: 0,
    dailyRewardNextDay: 1,
    dailyRewardLastClaimDate: "",
    dailyChallengeLastDate: "",
    dailyChallengeBestScore: 0,
    mainWalletPkrPaisa: 0,
    purchasedCoins: 0,
    earnedCoins: 0,
    walletVerificationName: "",
    walletVerificationProvider: "",
    walletVerificationAccount: "",
    walletVerificationCompleted: false,
    withdrawalPinHash: "",
    withdrawalPinSalt: "",
    musicEnabled: true,
    sfxEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("1. Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboards").where("boardType", "==", "ALL_TIME").get());
  await assertFails(unauthDb.collection("deposits").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("settings").doc("global").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(makeInitialUserPayload(ALICE_UID)));
});

test("2. User account creation enforces zero starting balances and blocks shadow/ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  // Valid Kotlin create payload succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeInitialUserPayload(ALICE_UID))
  );

  // Attempting to create with pre-loaded PKR or Coins fails
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeInitialUserPayload(BOB_UID, "bob@example.com", "Bob"),
      mainWalletPkrPaisa: 100000,
    })
  );

  // Shadow field / @DocumentId `id` injection fails
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeInitialUserPayload(BOB_UID, "bob@example.com", "Bob"),
      id: BOB_UID,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeInitialUserPayload(BOB_UID, "bob@example.com", "Bob"),
      isAdmin: true,
    })
  );
});

test("3. PII Isolation: Bob cannot read or list Alice's user document or wallet history", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(makeInitialUserPayload(ALICE_UID));
    await db.collection("walletTransactions").doc("tx_alice_1").set({
      userId: ALICE_UID,
      walletType: "COIN",
      coinSource: "EARNED",
      txType: "GAME_REWARD",
      amountDelta: 5,
      balanceAfter: 5,
      description: "Cleared 5 lines",
      referenceId: "ses_1",
      createdAt: nowPast(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(bobDb.collection("walletTransactions").doc("tx_alice_1").get());
  await assertFails(bobDb.collection("walletTransactions").get());

  // Alice can read her own profile & scoped transactions
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("walletTransactions").where("userId", "==", ALICE_UID).get());
});

test("4. Gameplay session enforces 1 line = 10 score + 1 coin and blocks score tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeInitialUserPayload(ALICE_UID)));

  // Valid game session (3 lines = 30 score + 3 coins)
  await assertSucceeds(
    aliceDb.collection("gameSessions").doc("ses_valid_1").set({
      userId: ALICE_UID,
      mode: "CLASSIC",
      score: 30,
      linesCleared: 3,
      coinsEarned: 3,
      levelNumber: 0,
      challengeId: "",
      continuesUsed: 0,
      createdAt: nowPast(),
    })
  );

  // Tampered score/coin ratio fails (3 lines claiming 9999 score or 50 coins)
  await assertFails(
    aliceDb.collection("gameSessions").doc("ses_fake_1").set({
      userId: ALICE_UID,
      mode: "CLASSIC",
      score: 9999,
      linesCleared: 3,
      coinsEarned: 3,
      levelNumber: 0,
      challengeId: "",
      continuesUsed: 0,
      createdAt: nowPast(),
    })
  );
  await assertFails(
    aliceDb.collection("gameSessions").doc("ses_fake_2").set({
      userId: ALICE_UID,
      mode: "CLASSIC",
      score: 30,
      linesCleared: 3,
      coinsEarned: 50,
      levelNumber: 0,
      challengeId: "",
      continuesUsed: 0,
      createdAt: nowPast(),
    })
  );

  // Valid gameplay user update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      classicHighScore: 30,
      totalLinesCleared: 3,
      totalGamesPlayed: 1,
      earnedCoins: 3,
      updatedAt: nowPast(500),
    })
  );
});

test("5. Daily Login Rewards advance 1->2..7->1 and block double claims on same date", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeInitialUserPayload(ALICE_UID)));

  // Claim Day 1 -> next becomes Day 2
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyRewardNextDay: 2,
      dailyRewardLastClaimDate: "2026-11-17",
      earnedCoins: 15,
      updatedAt: nowPast(1000),
    })
  );

  // Duplicate claim on same date fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyRewardNextDay: 3,
      dailyRewardLastClaimDate: "2026-11-17",
      earnedCoins: 40,
      updatedAt: nowPast(500),
    })
  );
});

test("6. Wallet exchange enforces exact value conservation (1 Coin = 5 paisa) and blocks earned-coin cashout by default", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      ...makeInitialUserPayload(ALICE_UID),
      mainWalletPkrPaisa: 50000, // PKR 500.00
      purchasedCoins: 1000,
      earnedCoins: 500,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // Valid PKR -> Purchased Coins (PKR 5.00 = 500 paisa -> 100 purchased coins)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mainWalletPkrPaisa: 49500,
      purchasedCoins: 1100,
      earnedCoins: 500,
      updatedAt: nowPast(1000),
    })
  );

  // Valid Purchased Coins -> PKR (100 purchased coins -> 500 paisa)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mainWalletPkrPaisa: 50000,
      purchasedCoins: 1000,
      earnedCoins: 500,
      updatedAt: nowPast(800),
    })
  );

  // Attempt to convert promotional/earned coins to PKR when allowEarnedCoinCashout is false -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mainWalletPkrPaisa: 50500,
      purchasedCoins: 1000,
      earnedCoins: 400,
      updatedAt: nowPast(600),
    })
  );

  // Attempt to mint extra PKR during exchange -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mainWalletPkrPaisa: 999999,
      purchasedCoins: 900,
      earnedCoins: 500,
      updatedAt: nowPast(400),
    })
  );
});

test("7. Deposit & Withdrawal security: users cannot self-approve, min withdrawal PKR 500, terminal-state locking", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      ...makeInitialUserPayload(ALICE_UID),
      mainWalletPkrPaisa: 100000, // PKR 1,000
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // User creating PENDING deposit succeeds
  await assertSucceeds(
    aliceDb.collection("deposits").doc("dep_test_001").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice",
      amountPaisa: 50000,
      paymentMethod: "JAZZCASH",
      transactionRef: "JC99887766",
      senderAccount: "03001234567",
      status: "PENDING",
      adminNote: "",
      adminPaymentRef: "",
      reviewedBy: "",
      idempotencyKey: "dep_test_001",
      sandboxMode: true,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // User trying to self-approve deposit fails
  await assertFails(
    aliceDb.collection("deposits").doc("dep_test_001").update({
      status: "APPROVED",
      reviewedBy: ALICE_UID,
      updatedAt: nowPast(500),
    })
  );

  // Withdrawal below PKR 500 (49900 paisa) fails
  await assertFails(
    aliceDb.collection("withdrawals").doc("wd_small_001").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice",
      amountPaisa: 49900,
      paymentMethod: "EASYPAISA",
      accountTitle: "Alice",
      accountNumber: "03451234567",
      status: "PENDING",
      adminNote: "",
      payoutReference: "",
      reviewedBy: "",
      idempotencyKey: "wd_small_001",
      sandboxMode: true,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Valid withdrawal >= PKR 500 succeeds
  await assertSucceeds(
    aliceDb.collection("withdrawals").doc("wd_valid_001").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice",
      amountPaisa: 50000,
      paymentMethod: "EASYPAISA",
      accountTitle: "Alice",
      accountNumber: "03451234567",
      status: "PENDING",
      adminNote: "",
      payoutReference: "",
      reviewedBy: "",
      idempotencyKey: "wd_valid_001",
      sandboxMode: true,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );
});

test("8. Super Admin RBAC: email spoofing denied, only Super Admin manages admins, sub-admins cannot promote", async () => {
  // Email spoof with email_verified: false -> DENIED
  const spoofSuperDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, {
    email: SUPER_ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofSuperDb.collection("admins").doc(SUB_ADMIN_UID).set({
      userId: SUB_ADMIN_UID,
      email: "sub@example.com",
      displayName: "Sub Admin",
      role: "ADMIN",
      active: true,
      permissions: ["MANAGE_DEPOSITS"],
      grantedBy: SUPER_ADMIN_UID,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Verified Super Admin grants Sub Admin -> SUCCEEDS
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, {
    email: SUPER_ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    superDb.collection("admins").doc(SUB_ADMIN_UID).set({
      userId: SUB_ADMIN_UID,
      email: "sub@example.com",
      displayName: "Sub Admin",
      role: "ADMIN",
      active: true,
      permissions: ["MANAGE_DEPOSITS"],
      grantedBy: SUPER_ADMIN_UID,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Sub Admin CANNOT promote Bob to Admin -> DENIED
  const subAdminDb = testEnv.authenticatedContext(SUB_ADMIN_UID, {
    email: "sub@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(
    subAdminDb.collection("admins").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      displayName: "Bob",
      role: "ADMIN",
      active: true,
      permissions: ["MANAGE_PLAYERS"],
      grantedBy: SUB_ADMIN_UID,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Sub Admin CAN create a Time Challenge & read auditLogs
  await assertSucceeds(
    subAdminDb.collection("timeChallenges").doc("tc_001").set({
      title: "Friday Speed Run",
      description: "Score highest in 3 minutes",
      durationSeconds: 180,
      startTimeMillis: Date.now() - 10000,
      endTimeMillis: Date.now() + 86400000,
      rewardType: "LEADERBOARD",
      targetScore: 0,
      rewardCoins: 250,
      active: true,
      createdBy: SUB_ADMIN_UID,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Super Admin revokes Sub Admin -> SUCCEEDS
  await assertSucceeds(superDb.collection("admins").doc(SUB_ADMIN_UID).delete());
});

test("9. Leaderboards & Daily Challenge Claims: scoped queries succeed, duplicate claims blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // Create All-Time & Daily leaderboard entries
  await assertSucceeds(
    aliceDb.collection("leaderboards").doc("alltime_" + ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      photoUrl: "",
      boardType: "ALL_TIME",
      dateKey: "ALL_TIME",
      bestScore: 120,
      totalLines: 12,
      highestLevel: 1,
      gamesPlayed: 2,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Filtered leaderboard query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("leaderboards").where("boardType", "==", "ALL_TIME").get());
  await assertFails(aliceDb.collection("leaderboards").get());

  // Daily challenge claim succeeds once; update/delete blocked forever
  await assertSucceeds(
    aliceDb.collection("dailyChallengeClaims").doc("2026-11-17_" + ALICE_UID).set({
      userId: ALICE_UID,
      dateKey: "2026-11-17",
      score: 150,
      linesCleared: 15,
      bonusCoinsClaimed: 40,
      createdAt: nowPast(),
    })
  );
  await assertFails(
    aliceDb.collection("dailyChallengeClaims").doc("2026-11-17_" + ALICE_UID).update({
      bonusCoinsClaimed: 80,
    })
  );
});

test("10. All remaining user update branches (Profile, Theme Spend, Withdrawal Hold) & Admin Deposit Approval Terminal Lock", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      ...makeInitialUserPayload(ALICE_UID),
      mainWalletPkrPaisa: 100000,
      earnedCoins: 400,
    });
    await db.collection("deposits").doc("dep_approve_001").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice",
      amountPaisa: 50000,
      paymentMethod: "JAZZCASH",
      transactionRef: "JC11223344",
      senderAccount: "03001234567",
      status: "PENDING",
      adminNote: "",
      adminPaymentRef: "",
      reviewedBy: "",
      idempotencyKey: "dep_approve_001",
      sandboxMode: true,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // Profile + Wallet Verification Form + Withdrawal PIN save succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Prism",
      walletVerificationName: "Alice Khan",
      walletVerificationProvider: "JAZZCASH",
      walletVerificationAccount: "03001234567",
      walletVerificationCompleted: true,
      withdrawalPinHash: "a1b2c3d4e5f6",
      withdrawalPinSalt: "salt123456",
      musicEnabled: false,
      sfxEnabled: true,
      updatedAt: nowPast(1500),
    })
  );

  // Spend Coins to unlock Premium Pastel Theme succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      earnedCoins: 150,
      purchasedCoins: 0,
      unlockedThemeIds: ["simple_classic", "pastel_macaron"],
      selectedThemeId: "pastel_macaron",
      updatedAt: nowPast(1200),
    })
  );

  // Withdrawal escrow hold (>= 50000 paisa) succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mainWalletPkrPaisa: 50000,
      updatedAt: nowPast(900),
    })
  );

  // Super Admin approves deposit once -> SUCCEEDS; trying to approve again fails (terminal state lock)
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, {
    email: SUPER_ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    superDb.collection("deposits").doc("dep_approve_001").update({
      status: "APPROVED",
      adminNote: "Verified bank receipt",
      adminPaymentRef: "ADM-JC-001",
      reviewedBy: SUPER_ADMIN_UID,
      updatedAt: nowPast(600),
    })
  );
  await assertFails(
    superDb.collection("deposits").doc("dep_approve_001").update({
      status: "APPROVED",
      adminNote: "Duplicate credit attempt",
      adminPaymentRef: "ADM-JC-002",
      reviewedBy: SUPER_ADMIN_UID,
      updatedAt: nowPast(300),
    })
  );

  // Super Admin crediting their own user account (e.g. approving their own test deposit) succeeds cleanly
  await assertSucceeds(
    superDb.collection("users").doc(SUPER_ADMIN_UID).set(
      makeInitialUserPayload(SUPER_ADMIN_UID, SUPER_ADMIN_EMAIL, "Super Admin")
    )
  );
  await assertSucceeds(
    superDb.collection("users").doc(SUPER_ADMIN_UID).update({
      mainWalletPkrPaisa: 100000,
      updatedAt: nowPast(200),
    })
  );
});

function makeShopItemPayload(overrides = {}) {
  const ts = nowPast(3000);
  return {
    name: "Diamond 3D Blocks",
    description: "Sparkling faceted 3D diamond block skin",
    category: "BLOCK_SKIN",
    subType: "DIAMOND",
    themeKey: "",
    assetKey: "skin_diamond",
    imageUrl: "",
    priceCoins: 350,
    promoActive: false,
    promoPriceCoins: 0,
    promoStartMillis: 0,
    promoEndMillis: 0,
    promoLabel: "",
    premiumPastel: false,
    featured: true,
    newArrival: true,
    limitedEdition: false,
    available: true,
    consumable: false,
    boosterPackQty: 1,
    boosterMaxPerGame: 0,
    boosterPowerRadius: 0,
    sortOrder: 10,
    accentHex: "#38E8FF",
    totalSalesCount: 0,
    uniqueOwnersCount: 0,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function makeInitialInventoryPayload(uid) {
  const ts = nowPast(3000);
  return {
    userId: uid,
    ownedItemIds: ["theme_simple_classic"],
    equippedThemeId: "simple_classic",
    equippedBlockSkinId: "",
    equippedBackgroundId: "",
    equippedEffectId: "",
    equippedAvatarId: "",
    equippedFrameId: "",
    equippedBadgeId: "",
    equippedExclusiveId: "",
    hammerCount: 0,
    undoCount: 0,
    shuffleCount: 0,
    rowBlasterCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("11. Coin Shop Catalog, Categories & Promotions: granular Admin RBAC enforced server-side", async () => {
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, {
    email: SUPER_ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const priceAdminUid = "price_admin_001";
  const noShopAdminUid = "noshop_admin_002";

  // Super Admin creates two scoped sub-admins
  await assertSucceeds(
    superDb.collection("admins").doc(priceAdminUid).set({
      userId: priceAdminUid,
      email: "priceadmin@example.com",
      displayName: "Price Admin",
      role: "ADMIN",
      active: true,
      permissions: ["MANAGE_SHOP_PRICES"],
      grantedBy: SUPER_ADMIN_UID,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );
  await assertSucceeds(
    superDb.collection("admins").doc(noShopAdminUid).set({
      userId: noShopAdminUid,
      email: "depositsonly@example.com",
      displayName: "Deposits Admin",
      role: "ADMIN",
      active: true,
      permissions: ["MANAGE_DEPOSITS"],
      grantedBy: SUPER_ADMIN_UID,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Super Admin cannot be forged even by Super Admin writing role=SUPER_ADMIN into admins
  await assertFails(
    superDb.collection("admins").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      displayName: "Bob",
      role: "SUPER_ADMIN",
      active: true,
      permissions: ["MANAGE_SHOP"],
      grantedBy: SUPER_ADMIN_UID,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Super Admin creates shop category, shop item, and promotional offer -> SUCCEEDS
  await assertSucceeds(
    superDb.collection("shopCategories").doc("cat_block_skin").set({
      categoryKey: "BLOCK_SKIN",
      displayName: "Block Skins",
      description: "3D block textures and finishes",
      iconKey: "cube",
      accentHex: "#38E8FF",
      enabled: true,
      sortOrder: 2,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );
  await assertSucceeds(
    superDb.collection("shopItems").doc("skin_diamond").set(makeShopItemPayload())
  );
  await assertSucceeds(
    superDb.collection("promotionalOffers").doc("promo_diamond_20").set({
      title: "Weekend Diamond Sale",
      description: "Save 20% on Diamond 3D Blocks",
      itemId: "skin_diamond",
      categoryKey: "BLOCK_SKIN",
      discountPercent: 20,
      promoPriceCoins: 280,
      startTimeMillis: Date.now() - 60000,
      endTimeMillis: Date.now() + 86400000,
      active: true,
      createdBy: SUPER_ADMIN_UID,
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Sub-admin with MANAGE_SHOP_PRICES CAN update price & promo fields ONLY
  const priceAdminDb = testEnv.authenticatedContext(priceAdminUid, {
    email: "priceadmin@example.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    priceAdminDb.collection("shopItems").doc("skin_diamond").update({
      priceCoins: 420,
      promoActive: true,
      promoPriceCoins: 299,
      promoStartMillis: Date.now() - 10000,
      promoEndMillis: Date.now() + 86400000,
      promoLabel: "FLASH SALE",
      updatedAt: nowPast(500),
    })
  );

  // Price-only admin CANNOT rename, delete items, or edit categories
  await assertFails(
    priceAdminDb.collection("shopItems").doc("skin_diamond").update({
      name: "Hacked Name",
      updatedAt: nowPast(200),
    })
  );
  await assertFails(priceAdminDb.collection("shopItems").doc("skin_diamond").delete());
  await assertFails(priceAdminDb.collection("shopCategories").doc("cat_block_skin").delete());

  // Deposits-only admin CANNOT touch shop items, prices, or promos
  const noShopDb = testEnv.authenticatedContext(noShopAdminUid, {
    email: "depositsonly@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(
    noShopDb.collection("shopItems").doc("skin_diamond").update({
      priceCoins: 1,
      updatedAt: nowPast(100),
    })
  );
  await assertFails(noShopDb.collection("promotionalOffers").doc("promo_diamond_20").delete());

  // Regular player CANNOT modify any shop item or category
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(aliceDb.collection("shopItems").doc("skin_diamond").update({ priceCoins: 0, updatedAt: nowPast(100) }));
  await assertSucceeds(aliceDb.collection("shopItems").where("available", "==", true).get());
  await assertFails(aliceDb.collection("shopItems").get());
});

test("12. Coin Shop Purchases: blocks disabled items, blocks duplicate non-consumables, enforces permanent receipts", async () => {
  const superDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, {
    email: SUPER_ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    superDb.collection("shopItems").doc("skin_diamond").set(makeShopItemPayload({ available: true }))
  );
  await assertSucceeds(
    superDb.collection("shopItems").doc("skin_disabled").set(makeShopItemPayload({
      name: "Retired Skin",
      assetKey: "skin_disabled",
      available: false,
    }))
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  // Attempting to buy a DISABLED item -> DENIED by rules
  await assertFails(
    aliceDb.collection("purchasedItems").doc(`${ALICE_UID}_skin_disabled`).set({
      userId: ALICE_UID,
      itemId: "skin_disabled",
      itemName: "Retired Skin",
      category: "BLOCK_SKIN",
      assetKey: "skin_disabled",
      pricePaidCoins: 350,
      originalPriceCoins: 350,
      promoApplied: false,
      quantityPurchased: 1,
      consumable: false,
      idempotencyKey: "buy_disabled_01",
      purchasedAt: nowPast(),
    })
  );

  // Buying an ACTIVE non-consumable item succeeds with deterministic ID
  await assertSucceeds(
    aliceDb.collection("purchasedItems").doc(`${ALICE_UID}_skin_diamond`).set({
      userId: ALICE_UID,
      itemId: "skin_diamond",
      itemName: "Diamond 3D Blocks",
      category: "BLOCK_SKIN",
      assetKey: "skin_diamond",
      pricePaidCoins: 350,
      originalPriceCoins: 350,
      promoApplied: false,
      quantityPurchased: 1,
      consumable: false,
      idempotencyKey: "buy_diamond_01",
      purchasedAt: nowPast(),
    })
  );

  // Attempting to buy the SAME non-consumable again (overwrite/update) -> DENIED!
  await assertFails(
    aliceDb.collection("purchasedItems").doc(`${ALICE_UID}_skin_diamond`).set({
      userId: ALICE_UID,
      itemId: "skin_diamond",
      itemName: "Diamond 3D Blocks",
      category: "BLOCK_SKIN",
      assetKey: "skin_diamond",
      pricePaidCoins: 350,
      originalPriceCoins: 350,
      promoApplied: false,
      quantityPurchased: 1,
      consumable: false,
      idempotencyKey: "buy_diamond_02",
      purchasedAt: nowPast(500),
    })
  );

  // Attempting to forge a random doc ID for a non-consumable item -> DENIED!
  await assertFails(
    aliceDb.collection("purchasedItems").doc("random_dup_id").set({
      userId: ALICE_UID,
      itemId: "skin_diamond",
      itemName: "Diamond 3D Blocks",
      category: "BLOCK_SKIN",
      assetKey: "skin_diamond",
      pricePaidCoins: 350,
      originalPriceCoins: 350,
      promoApplied: false,
      quantityPurchased: 1,
      consumable: false,
      idempotencyKey: "buy_diamond_03",
      purchasedAt: nowPast(400),
    })
  );

  // Shop transaction receipt & atomic +1 sales counter update -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("shopTransactions").doc(`stx_${ALICE_UID}_buy_diamond_01`).set({
      userId: ALICE_UID,
      userDisplayName: "Alice",
      itemId: "skin_diamond",
      itemName: "Diamond 3D Blocks",
      category: "BLOCK_SKIN",
      pricePaidCoins: 350,
      originalPriceCoins: 350,
      coinSource: "EARNED",
      quantity: 1,
      promoOfferId: "",
      idempotencyKey: "buy_diamond_01",
      status: "COMPLETED",
      createdAt: nowPast(300),
    })
  );
  await assertSucceeds(
    aliceDb.collection("shopItems").doc("skin_diamond").update({
      totalSalesCount: 1,
      uniqueOwnersCount: 1,
      updatedAt: nowPast(200),
    })
  );

  // Even after Admin disables item and doubles its price, Alice's ownership receipt stays intact & readable
  await assertSucceeds(
    superDb.collection("shopItems").doc("skin_diamond").update({
      priceCoins: 999,
      available: false,
      updatedAt: nowPast(100),
    })
  );
  await assertSucceeds(aliceDb.collection("purchasedItems").doc(`${ALICE_UID}_skin_diamond`).get());
});

test("13. Player Inventory & Booster Security: zero starting boosters, equip/consume allowed, cross-user read blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  // Creating inventory with free boosters -> DENIED
  await assertFails(
    aliceDb.collection("playerInventory").doc(ALICE_UID).set({
      ...makeInitialInventoryPayload(ALICE_UID),
      hammerCount: 50,
    })
  );

  // Creating clean zero-booster inventory -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("playerInventory").doc(ALICE_UID).set(makeInitialInventoryPayload(ALICE_UID))
  );

  // Cross-user read/write by Bob -> DENIED
  await assertFails(bobDb.collection("playerInventory").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("playerInventory").get());

  // Granting 1 owned item + 3 hammers during purchase -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("playerInventory").doc(ALICE_UID).update({
      ownedItemIds: ["theme_simple_classic", "skin_diamond"],
      hammerCount: 3,
      updatedAt: nowPast(1500),
    })
  );

  // Equipping cosmetic -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("playerInventory").doc(ALICE_UID).update({
      equippedBlockSkinId: "skin_diamond",
      updatedAt: nowPast(1000),
    })
  );

  // Consuming 1 hammer in gameplay (3 -> 2) -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("playerInventory").doc(ALICE_UID).update({
      hammerCount: 2,
      updatedAt: nowPast(500),
    })
  );

  // Attempting to wipe owned items -> DENIED
  await assertFails(
    aliceDb.collection("playerInventory").doc(ALICE_UID).update({
      ownedItemIds: [],
      updatedAt: nowPast(200),
    })
  );

  // Attempting to set negative booster count -> DENIED
  await assertFails(
    aliceDb.collection("playerInventory").doc(ALICE_UID).update({
      hammerCount: -1,
      updatedAt: nowPast(100),
    })
  );
});

test("14. PlayerDataRepository parity: UserProfile partial updates, /users/{uid}/levelProgress monotonicity, and LeaderboardScore upserts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  // Seed Alice's UserProfile doc at /users/{uid}
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeInitialUserPayload(ALICE_UID))
  );

  // Unauthenticated read/write on levelProgress -> DENIED
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("levelProgress").get()
  );

  // Owner listing empty levelProgress subcollection -> SUCCEEDS (verifies no existing() bug in allow list)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").get()
  );

  const createdTs = nowPast(4000);
  const validLevel1 = {
    userId: ALICE_UID,
    levelNumber: 1,
    linesCleared: 250,
    requiredLines: 1000,
    bestScore: 2500,
    attemptsCount: 1,
    starsEarned: 1,
    completed: false,
    createdAt: createdTs,
    updatedAt: createdTs,
  };

  // Mismatched deterministic doc ID (wrong_id when levelNumber=1) -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("wrong_id").set(validLevel1)
  );

  // Unexpected @DocumentId shadow field `id` -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").set({
      ...validLevel1,
      id: "level_1",
    })
  );

  // Completed=true while linesCleared < requiredLines -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").set({
      ...validLevel1,
      completed: true,
      completedAt: nowPast(3500),
    })
  );

  // Valid incomplete level_1 create (10 exact fields, NO completedAt) -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").set(validLevel1)
  );

  // Cross-user (Bob) reading, listing, or updating Alice's levelProgress -> DENIED
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("levelProgress").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      linesCleared: 500,
      updatedAt: nowPast(2000),
    })
  );

  // Owner monotonic progress update (250 -> 650 lines, 1 -> 2 stars) -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      linesCleared: 650,
      bestScore: 6500,
      attemptsCount: 2,
      starsEarned: 2,
      completed: false,
      updatedAt: nowPast(2000),
    })
  );

  // Regressing linesCleared (650 -> 400) -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      linesCleared: 400,
      updatedAt: nowPast(1500),
    })
  );

  // Tampering requiredLines or mutating createdAt -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      requiredLines: 10,
      updatedAt: nowPast(1500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      createdAt: nowPast(1000),
      updatedAt: nowPast(1000),
    })
  );

  // Completing level_1 (1000 / 1000 lines, completed=true, completedAt added -> 11 exact fields) -> SUCCEEDS
  const completedTs = nowPast(1000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      linesCleared: 1000,
      bestScore: 10000,
      attemptsCount: 3,
      starsEarned: 3,
      completed: true,
      completedAt: completedTs,
      updatedAt: completedTs,
    })
  );

  // Replaying completed level to raise bestScore without mutating completedAt -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      linesCleared: 1000,
      bestScore: 14500,
      attemptsCount: 4,
      starsEarned: 3,
      completed: true,
      updatedAt: nowPast(500),
    })
  );

  // Attempting to un-complete or tamper completedAt -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      completed: false,
      updatedAt: nowPast(400),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").update({
      completedAt: nowPast(300),
      updatedAt: nowPast(300),
    })
  );

  // Deleting levelProgress is blocked -> DENIED
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("levelProgress").doc("level_1").delete()
  );

  // PlayerDataRepository.updateUserProfile partial update -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Star",
      photoUrl: "https://example.com/alice.png",
      musicEnabled: false,
      sfxEnabled: true,
      updatedAt: nowPast(300),
    })
  );

  // PlayerDataRepository.submitDailyAndAllTimeScores create & monotonic update -> SUCCEEDS
  const dailyDocId = `daily_2026-11-18_${ALICE_UID}`;
  const allTimeDocId = `alltime_${ALICE_UID}`;
  await assertSucceeds(
    aliceDb.collection("leaderboards").doc(dailyDocId).set({
      userId: ALICE_UID,
      displayName: "Alice Star",
      photoUrl: "https://example.com/alice.png",
      boardType: "DAILY",
      dateKey: "2026-11-18",
      bestScore: 1200,
      totalLines: 120,
      highestLevel: 2,
      gamesPlayed: 1,
      createdAt: nowPast(500),
      updatedAt: nowPast(500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("leaderboards").doc(allTimeDocId).set({
      userId: ALICE_UID,
      displayName: "Alice Star",
      photoUrl: "https://example.com/alice.png",
      boardType: "ALL_TIME",
      dateKey: "ALL_TIME",
      bestScore: 1200,
      totalLines: 120,
      highestLevel: 2,
      gamesPlayed: 1,
      createdAt: nowPast(500),
      updatedAt: nowPast(500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("leaderboards").doc(allTimeDocId).update({
      displayName: "Alice Star",
      photoUrl: "https://example.com/alice.png",
      bestScore: 1850,
      totalLines: 185,
      highestLevel: 3,
      gamesPlayed: 2,
      updatedAt: nowPast(200),
    })
  );
  // Attempting to lower bestScore on leaderboard -> DENIED
  await assertFails(
    aliceDb.collection("leaderboards").doc(allTimeDocId).update({
      bestScore: 500,
      updatedAt: nowPast(100),
    })
  );
  // Rule-aligned DAILY + ALL_TIME queries succeed; unfiltered list fails
  await assertSucceeds(
    aliceDb.collection("leaderboards").where("boardType", "==", "DAILY").where("dateKey", "==", "2026-11-18").get()
  );
  await assertSucceeds(
    aliceDb.collection("leaderboards").where("boardType", "==", "ALL_TIME").get()
  );
  await assertFails(aliceDb.collection("leaderboards").get());
});



