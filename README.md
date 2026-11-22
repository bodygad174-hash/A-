# AFH AI — One Platform. Your AI. Your Identity.

**Arabic Slogan:** كل الذكاء الاصطناعي في مكان واحد

AFH AI is a complete, unified AI platform featuring both a native **Android App (Kotlin + Jetpack Compose + Material 3)** and a synced **Responsive Website & Backend (`web/`)**, sharing the exact same brand identity, official logo artwork, Firebase Authentication, Firestore Enterprise database, AFH-branded model catalog, and Admin Console.

---

## Brand Identity & Logo
- **Official Logo Artwork:** Integrated across the Android adaptive launcher icon, in-app AFH AI logo mark, website navbar, hero section, login card, and browser favicon (`app/src/main/res/drawable/afh_hero_artwork.jpg` and `web/public/afh-logo.jpg`).
- **Palette:** Deep Navy `#080D19`, Dark Blue `#101A30`, Electric Blue `#1677FF`, Cyan `#00D5FF`, Turquoise `#00E6C7`, Soft Purple `#875BFF`, White `#F5F8FF`.
- **Additional SVG Assets (`brand-assets/`):** `afh-logo-icon.svg`, `afh-logo-horizontal.svg`, `afh-logo-square.svg`, `afh-favicon.svg`.

---

## Exclusive AFH AI Model Suite
Normal users see only AFH AI branding across both Android and Web:
1. **AFH Flash** (`afh_flash`) — Fast answers and everyday assistance
2. **AFH Pro** (`afh_pro`) — Advanced reasoning, analysis, and problem solving
3. **AFH Code** (`afh_code`) — Programming, debugging, and software development
4. **AFH Study** (`afh_study`) — Education, explanations, studying, and homework assistance
5. **AFH Creative** (`afh_creative`) — Writing, ideas, creativity, and Image Studio generation
6. **AFH Vision** (`afh_vision`) — Image understanding and visual analysis
7. **AFH Research** (`afh_research`) — Research, information analysis, and long-form answers

*(Internal provider mappings and external model identifiers are hidden from regular users and managed exclusively inside the Admin Console.)*

---

## Live Cross-Platform Sync (Android ↔ Web)
Both apps connect to Firebase project `gen-lang-client-0968772112` and named Firestore Enterprise database `ai-studio-android-afhai-bee1e549-d681-476c-be40-9f20947df408`:
- **Same Google Account & Profile:** Signing in on Android and Web shares your `users/{uid}` profile, quotas, preferred language (`en`/`ar`), and theme (`dark`/`light`).
- **Same Conversations & Messages:** Chats started on Android immediately appear in the Web sidebar and vice versa (`users/{uid}/conversations/{convId}/messages`).
- **Same Model Catalog & Admin Control:** Enabling/disabling models, suspending users, or viewing usage logs in either Admin Dashboard updates both platforms in real time.

---

## Android App (`app/`)
- **Core (`app/src/main/java/com/example/core/`):** `AppCheckInitializer.kt`, `FirestoreErrorHandler.kt`, `AfhStrings.kt` (EN/AR RTL)
- **Models & Data (`model/`, `data/`):** `AfhModels.kt`, `AuthManager.kt` (Credential Manager Google Sign-In), `AfhFirestoreRepository.kt`
- **AI Gateway (`ai/`):** `AiProviderGateway.kt` using Firebase AI Logic (`Firebase.ai`) for streaming, vision/PDF analysis, and image generation
- **UI & ViewModel (`ui/`, `viewmodel/`):** M3 dark/light theme, Home Showcase, Chat & Visual Studio, Settings, Admin Dashboard
- **Verify Security Rules Locally:**
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```
- **Build Android APK:**
  ```bash
  gradle :app:assembleDebug
  ```

---

## Website & Backend (`web/`)
- **Frontend (`web/src/`):** React + Vite + Tailwind CSS v4 + Firebase Web SDK (`firebase.ts`, `models.ts`, `App.tsx`)
- **Backend API (`web/server/index.ts`):** Express SSE `/api/chat` proxy supporting official provider adapters (`GEMINI_API_KEY`, `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `DEEPSEEK_API_KEY`, `OPEN_SOURCE_BASE_URL`) via `web/.env`

### Running the Website Locally
1. Configure server environment variables:
   ```bash
   cd web
   cp .env.example .env
   # Add your server-side provider API key(s), e.g., GEMINI_API_KEY=...
   ```
2. Install dependencies, build, and start the unified Web + API server:
   ```bash
   npm install
   npm run build
   npm run start
   ```
   Or deploy the included `web/Dockerfile` directly to **Cloud Run**, **Render**, or **Railway**.
3. **Firebase Auth Authorized Domains (Important for Web Sign-In):**
   In the Firebase Console (`gen-lang-client-0968772112` → Authentication → Settings → Authorized domains), ensure `localhost` and your deployed website domain are listed so Google Sign-In popup works in browsers.
