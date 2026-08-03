# Firebase Setup Instructions

## Current Status

The Phase 3 cloud features are **ready but disabled** to allow the app to build without Firebase configuration. The app currently works perfectly in local-only mode with all Phase 1 and Phase 2 security features enabled.

## To Enable Cloud Features

### 1. Move Cloud Files Back

```bash
# Move cloud files back to active directory
mv disabled_cloud/cloud app/src/main/java/com/warrantyvault/
```

### 2. Uncomment Firebase Dependencies

In `app/build.gradle.kts`:
- Uncomment the `id("com.google.gms.google-services")` plugin
- Uncomment the Firebase dependencies

### 3. Uncomment Firebase Providers

In `app/src/main/java/com/warrantyvault/di/AppModule.kt`:
- Uncomment the Firebase imports
- Uncomment the provider functions

### 4. Uncomment Firebase Usage

In `app/src/main/java/com/warrantyvault/ui/SettingsViewModel.kt`:
- Uncomment the Firebase imports
- Uncomment the constructor parameters
- Uncomment the cloud feature methods

### 5. Uncomment Cloud UI Features

In `app/src/main/java/com/warrantyvault/ui/SettingsScreen.kt`:
- Uncomment the cloud feature method calls
- Uncomment the password reset dialog

### 6. Complete Firebase Setup

Follow the original setup instructions below:

## Original Firebase Setup Instructions

This app includes Firebase integration for cloud backup, authentication, and multi-device sync. To enable these features:

## 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project: "Warranty Vault"
3. Enable the following services:
   - Authentication (Email/Password)
   - Firestore Database
   - Storage

## 2. Add Android App

1. In Firebase Console, add an Android app
2. Package name: `com.warrantyvault`
3. Package name: `com.warrantyvault.debug` (for debug builds)
4. Download `google-services.json`
5. Replace the placeholder file in `app/google-services.json`

## 3. Configure Authentication

1. Enable Email/Password sign-in method
2. Set up password requirements (min 6 characters recommended)

## 4. Configure Firestore

1. Create Firestore database
2. Choose production mode or test mode
3. Set up security rules (see below)

## 5. Configure Storage

1. Create Storage bucket
2. Set up security rules (see below)

## Security Rules

### Firestore Rules
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      match /warranties/{warrantyId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

### Storage Rules
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## 6. Build and Test

1. Sync Gradle files
2. Build the app
3. Test authentication flow
4. Test cloud backup functionality

## Features Implemented

- ✅ Firebase Authentication (Email/Password)
- ✅ Encrypted Cloud Backup (Firebase Storage)
- ✅ Multi-device Sync (Firestore)
- ✅ Enhanced Privacy Settings
- ✅ Account Recovery Options

## Important Notes

- The current implementation supports both local-only and cloud modes
- Users can choose whether to enable cloud features
- All cloud data is encrypted before upload
- The app maintains full functionality without Firebase (local-only mode)