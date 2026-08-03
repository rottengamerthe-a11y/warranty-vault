# Free Backup System Setup Guide

## Overview

Your Warranty Vault app now uses a **completely free backup system** that replaces Firebase Storage with:

1. **Automatic Local Backups** - Scheduled backups stored on device
2. **Optional Google Drive Integration** - Cloud backup using user's existing 15GB Google storage
3. **No subscription costs** - All features are free

## Features

### 1. Automatic Local Backups
- **Completely free** - no external services required
- **Automatic scheduling** - backups created every 7 days (configurable)
- **Version management** - keeps last 5 backups (configurable)
- **Encrypted storage** - optional password protection
- **Manual backups** - create backups anytime
- **Restore functionality** - restore from any backup version

### 2. Google Drive Cloud Backup (Optional)
- **Uses user's existing Google Drive** - 15GB free storage
- **No Firebase subscription needed**
- **App-specific folder** - organized in "WarrantyVault Backups"
- **Encrypted uploads** - same encryption as local backups
- **Cross-device access** - access backups from any device
- **Optional feature** - users can choose to enable or disable

## New Components

### Backup Managers
- `LocalBackupManager.kt` - Handles local file backups
- `GoogleDriveBackupManager.kt` - Handles Google Drive integration
- `AutoBackupScheduler.kt` - Schedules automatic backups
- `AutoBackupWorker.kt` - Background worker for automatic backups

### Configuration
- **Dependencies Added**: Google Drive API libraries
- **No Firebase Storage required** - completely removed dependency
- **DI Providers**: Added to `AppModule.kt`

## Usage

### For Users

#### Automatic Local Backups
1. Go to Settings → Backup Settings
2. Enable "Auto Backup" (default: enabled)
3. Set backup interval (default: 7 days)
4. Set maximum backup versions (default: 5)
5. Optionally set backup password for encryption

#### Manual Local Backups
1. Go to Settings → Backup Settings
2. Tap "Create Manual Backup"
3. Backup will be saved to local storage
4. View backup list and restore from any version

#### Google Drive Backup
1. Go to Settings → Backup Settings
2. Tap "Connect Google Drive"
3. Sign in with Google account
4. Upload backups to Google Drive
5. Restore from any Drive backup

### For Developers

#### Dependency Injection
```kotlin
@Inject lateinit var localBackupManager: LocalBackupManager
@Inject lateinit var googleDriveBackupManager: GoogleDriveBackupManager
@Inject lateinit var autoBackupScheduler: AutoBackupScheduler
```

#### Create Manual Backup
```kotlin
val result = localBackupManager.createManualBackup("my_backup.json")
result.onSuccess { backupInfo ->
    // Backup created successfully
}
```

#### List Local Backups
```kotlin
val backups = localBackupManager.listBackups()
backups.onSuccess { backupList ->
    // Display backup list
}
```

#### Restore from Backup
```kotlin
val result = localBackupManager.restoreBackup(backupFile)
result.onSuccess {
    // Backup restored successfully
}
```

#### Google Drive Upload
```kotlin
val result = googleDriveBackupManager.uploadBackup("drive_backup.json")
result.onSuccess { driveBackupInfo ->
    // Uploaded to Google Drive
}
```

## Migration from Firebase

If you previously had Firebase Storage configured:

1. **Firebase Storage is no longer needed** - the new system replaces it completely
2. **Existing Firebase files** - users should export their data before switching
3. **Firestore sync** - still disabled (was in disabled_cloud folder)
4. **Firebase Auth** - replaced by Google Sign-In for Drive access

## Setup Instructions

### 1. Google Drive Setup (Optional)

For Google Drive integration to work, you need to set up Google Cloud Console:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google Drive API
4. Configure OAuth consent screen:
   - Add "Warranty Vault" as app name
   - Add your email as contact
   - Add scopes: `drive.file`, `drive.appdata`
5. Create OAuth 2.0 credentials:
   - Application type: Android
   - Package name: `com.warrantyvault`
   - SHA-1 fingerprint: Get from your keystore
6. Download `client_secret.json` and place in `app/src/main/`

### 2. Build Configuration

The project is ready to build with the new backup system. No additional configuration needed for local backups.

### 3. Testing

```bash
# Build the app
./gradlew assembleDebug

# Install and test
./gradlew installDebug
```

## Storage Limits

### Local Backups
- **Limited by device storage** - typically several GB available
- **Automatic cleanup** - old backups deleted based on version limit
- **Typical backup size** - 1-10MB depending on attachments

### Google Drive Backups
- **15GB free per user** - standard Google Drive free tier
- **App-specific folder** - doesn't clutter user's main Drive
- **Same storage as local** - encryption overhead minimal

## Security

- **Encryption** - All backups can be password-protected using existing `BackupEncryption`
- **Local storage** - Uses app-private storage directory
- **Google Drive** - Uses app data folder, private to your app
- **No API keys exposed** - Uses OAuth for Google Drive

## Troubleshooting

### Local Backups Not Working
- Check device storage space
- Verify backup permissions in app settings
- Check backup scheduler is enabled

### Google Drive Issues
- Verify Google Drive API is enabled in Cloud Console
- Check OAuth credentials are correct
- Ensure SHA-1 fingerprint matches keystore
- Check network connectivity

### Restore Failures
- Verify backup password is correct
- Check backup file integrity
- Ensure backup format is compatible

## Benefits Over Firebase Storage

✅ **Completely free** - no subscription costs
✅ **No service dependency** - local backups work offline
✅ **User control** - users manage their own storage
✅ **Privacy focused** - data stays on user's device or their personal Drive
✅ **No configuration needed** - works out of the box for local backups
✅ **Optional cloud** - users choose if they want cloud backup

## Future Enhancements

Potential improvements for the backup system:

1. **Additional cloud providers** - Dropbox, OneDrive integration
2. **Backup compression** - reduce storage size
3. **Incremental backups** - only backup changes
4. **Backup scheduling UI** - more granular scheduling options
5. **Backup validation** - verify backup integrity before restore