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

test("Unauthenticated user: cannot read user profiles or members", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("members").get());
});

test("Authenticated user: can create own profile and member", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      schoolName: "SMP Negeri 1",
      adminName: "Pak Guru Budi",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_1").set({
      id: "mem_1",
      userId: ALICE_UID,
      name: "Ahmad Siswa",
      role: "STUDENT",
      identifier: "NIS-1001",
      departmentOrClass: "Kelas 7A",
      createdAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's attendance", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("attendances").doc("att_1").set({
      id: "att_1",
      userId: ALICE_UID,
      memberId: "mem_1",
      memberName: "Ahmad Siswa",
      memberRole: "STUDENT",
      departmentOrClass: "Kelas 7A",
      date: "2026-10-19",
      time: "07:05:00",
      type: "CHECK_IN",
      status: "ON_TIME",
      createdAt: new Date(),
    })
  );
});
