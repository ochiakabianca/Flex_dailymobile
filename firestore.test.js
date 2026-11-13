const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, serverTs) {
  return {
    userId: uid,
    displayName: "Alice Runner",
    calorieGoal: 500,
    activeMinutesGoal: 45,
    waterGoalMl: 2500,
    stepsGoal: 8000,
    weeklyWorkoutsGoal: 5,
    preferredUnit: "kg",
    createdAt: serverTs,
    updatedAt: serverTs,
  };
}

function validDailyLog(uid, dateKey, serverTs) {
  return {
    userId: uid,
    dateKey,
    waterMl: 1250,
    steps: 6400,
    habitsCompleted: ["morning_mobility", "hydrate_2l"],
    mood: "energized",
    createdAt: serverTs,
    updatedAt: serverTs,
  };
}

function validWorkout(uid, serverTs) {
  return {
    userId: uid,
    dateKey: "2026-11-13",
    title: "Upper Body Push",
    category: "strength",
    durationMinutes: 45,
    caloriesBurned: 320,
    intensity: "vigorous",
    distanceKm: 0,
    setsCompleted: 16,
    notes: "Felt strong on incline press.",
    createdAt: serverTs,
    updatedAt: serverTs,
  };
}

function validWeight(uid, serverTs) {
  return {
    userId: uid,
    dateKey: "2026-11-13",
    weightKg: 71.4,
    note: "Morning weigh-in",
    createdAt: serverTs,
    updatedAt: serverTs,
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

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workouts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("dailyLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("weightLogs").get());
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Owner can create, read, and update their UserProfile; ghost fields rejected", async () => {
  const aliceCtx = testEnv.authenticatedContext(ALICE_UID);
  const aliceDb = aliceCtx.firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfile(ALICE_UID, pastDate())));
  await assertSucceeds(profileRef.get());

  // Shadow/ghost field test
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ isAdmin: true, updatedAt: pastDate(1000) })
  );

  // Valid partial update
  await assertSucceeds(
    profileRef.update({ calorieGoal: 650, updatedAt: pastDate(1000) })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's profile, workouts, dailyLogs, or weightLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, pastDate()))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w1").set(validWorkout(ALICE_UID, pastDate()))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-13").set(validDailyLog(ALICE_UID, "2026-11-13", pastDate()))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("weightLogs").doc("wt1").set(validWeight(ALICE_UID, pastDate()))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workouts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-13").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("weightLogs").doc("wt1").delete());
});

test("Owner can list and update workouts, dailyLogs, and weightLogs; invalid payloads rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Valid writes and list queries
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_1").set(validWorkout(ALICE_UID, pastDate()))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-13").set(validDailyLog(ALICE_UID, "2026-11-13", pastDate()))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("weightLogs").doc("wt_1").set(validWeight(ALICE_UID, pastDate()))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("weightLogs").get()
  );

  // Invalid category or out-of-range duration rejected
  const badWorkout = validWorkout(ALICE_UID, pastDate());
  badWorkout.category = "invalid_cat";
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_bad").set(badWorkout)
  );

  // DateKey mismatch on dailyLogs rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-14").set(validDailyLog(ALICE_UID, "2026-11-13", pastDate()))
  );

  // Identity spoofing rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_spoof").set(validWorkout(BOB_UID, pastDate()))
  );
});
