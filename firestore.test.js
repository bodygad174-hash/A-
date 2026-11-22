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
const ADMIN_UID = "admin_789";

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

function validUserPayload(uid, role = "user") {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Alice",
    email: "alice@example.com",
    role,
    status: "active",
    dailyRequestsUsed: 0,
    monthlyRequestsUsed: 0,
    dailyLimit: 50,
    monthlyLimit: 1000,
    preferredLanguage: "en",
    preferredTheme: "dark",
    defaultAfhModelId: "afh_flash",
    streamingEnabled: true,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write users, conversations, or models", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("ai_models").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
});

test("User profile isolation and privilege escalation guard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Cannot self-assign admin role
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, "admin")));

  // Valid user profile creation succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, "user")));

  // Bob cannot read or list Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Alice cannot elevate her own role or dailyLimit on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can update her own preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "ar",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Conversations and messages strict ownership and schema parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1").set({
      userId: ALICE_UID,
      title: "Architecture Discussion",
      selectedAfhModelId: "afh_pro",
      favorite: false,
      lastMessagePreview: "Hello AFH AI",
      messageCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1")
      .collection("messages").doc("msg_1").set({
        userId: ALICE_UID,
        conversationId: "conv_1",
        role: "user",
        content: "Design a scalable API",
        afhModelId: "afh_pro",
        attachmentNames: [],
        attachmentTypes: [],
        createdAt: now,
        updatedAt: now,
      })
  );

  // Bob cannot list or read Alice's conversations or messages
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("conversations").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1")
      .collection("messages").get()
  );
});

test("Admin-only provider mappings and bootstrapped admin access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof", { email: "bodygad999@gmail.com", email_verified: false }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "bodygad999@gmail.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 1000);

  // Regular user & unverified email spoof cannot access admin_provider_mappings
  await assertFails(aliceDb.collection("admin_provider_mappings").get());
  await assertFails(spoofedAdminDb.collection("admin_provider_mappings").get());

  // Verified admin can write and read provider mappings
  await assertSucceeds(
    adminDb.collection("admin_provider_mappings").doc("afh_flash").set({
      afhModelId: "afh_flash",
      providerIdentifier: "google_gemini",
      underlyingModelId: "gemini-3.8-flash",
      systemPrompt: "You are AFH Flash.",
      rateLimitRpm: 60,
      timeoutSeconds: 45,
      updatedAt: now,
    })
  );
  await assertSucceeds(adminDb.collection("admin_provider_mappings").get());
});
