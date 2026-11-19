# Photo Locker

Private Photo & Video Vault for Android by Wins Mediatama.

## Project status

Early implementation foundation. Security-first, local-first application.

## Planned stack
- Kotlin + Jetpack Compose + Material 3
- MVVM / Clean Architecture
- Room
- Android Keystore
- AES-256-GCM authenticated encryption
- Android Photo Picker
- Media3 for video playback
- BiometricPrompt

## Security principles
- Vault media is encrypted before being persisted.
- Keys are protected using Android Keystore.
- No cloud upload is required for core functionality.
- Original media is not deleted automatically during import.
- Backup must be encrypted and recovery behavior documented.

## Modules
Authentication, encrypted storage, media import, vault gallery, albums, viewer, trash, settings, and encrypted backup.
