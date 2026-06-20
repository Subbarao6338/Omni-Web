import { initializeApp, getApp, getApps } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup } from 'firebase/auth';
import { getFirestore, doc, getDocFromServer } from 'firebase/firestore';

// Default configuration with environment fallbacks
const defaultFirebaseConfig = {
  apiKey: "AIzaSy...", // Placeholder
  authDomain: "nature-browser.firebaseapp.com",
  projectId: "nature-browser",
  storageBucket: "nature-browser.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abcdef"
};

let firebaseConfig = defaultFirebaseConfig;

// Use glob to avoid build errors when file is missing
const configModules = import.meta.glob('../firebase-applet-config.json', { eager: true });
const configKey = '../firebase-applet-config.json';

if (configModules[configKey]) {
  // @ts-ignore
  firebaseConfig = configModules[configKey].default || configModules[configKey];
}

const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
export const auth = getAuth(app);
// @ts-ignore
export const db = getFirestore(app, firebaseConfig.firestoreDatabaseId);

export const googleProvider = new GoogleAuthProvider();

export async function loginWithGoogle() {
  try {
    const result = await signInWithPopup(auth, googleProvider);
    return result.user;
  } catch (error) {
    console.error("Login failed:", error);
    throw error;
  }
}

// Error Handling Spec for Firestore
export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: any;
}

export function handleFirestoreError(error: unknown, operationType: OperationType, path: string | null) {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous
    },
    operationType,
    path
  };
  console.error('Firestore Error: ', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

// Connection test
async function testConnection() {
  try {
    await getDocFromServer(doc(db, 'test', 'connection'));
    console.log("Firebase connection successful.");
  } catch (error) {
    console.error("Firebase connection test failed:", error);
    if(error instanceof Error && (error.message.includes('the client is offline') || error.message.includes('permission-denied'))) {
      console.warn("Firebase configuration issue detected. Verification attempt starting...");
    }
  }
}

testConnection();
