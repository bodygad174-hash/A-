import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

// Connected to the exact same Firebase project and named Firestore database as the AFH AI Android app
const firebaseConfig = {
  apiKey: "AIzaSyDR6wCHj69Qn0g_tB_xxjMdwAd40NB8hr0",
  authDomain: "gen-lang-client-0968772112.firebaseapp.com",
  projectId: "gen-lang-client-0968772112",
  storageBucket: "gen-lang-client-0968772112.firebasestorage.app",
  messagingSenderId: "382119320788",
  appId: "1:382119320788:web:afhai-web-client",
};

export const FIRESTORE_DATABASE_ID =
  "ai-studio-android-afhai-bee1e549-d681-476c-be40-9f20947df408";

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
export const db = getFirestore(app, FIRESTORE_DATABASE_ID);
