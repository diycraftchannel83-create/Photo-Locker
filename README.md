# Photo Locker

Private Photo & Video Vault for Android by Wins Mediatama.

## Project status
Development / in progress. Backup export and restore are implemented in the feature branch, but build verification, automated testing, device testing, and security audit are still pending. Not production-ready.

## Stack
- Kotlin + Jetpack Compose + Material 3
- Room
- Android Keystore
- AES-256-GCM authenticated encryption
- Android Photo Picker
- Media3 video playback
- BiometricPrompt

## Security principles
- Vault media is encrypted before being persisted.
- Keys are protected using Android Keystore.
- No cloud upload is required for core functionality.
- Original media is not deleted automatically during import.
- Portable backups use a separate random 256-bit Recovery Key; the key is not stored in the app.

## Implemented foundation
- Android Compose dashboard and first-launch PIN gate
- Keystore-protected PIN verifier and temporary lockout
- Biometric unlock where supported
- AES-256-GCM encrypted vault storage
- Android Photo Picker import
- Room metadata, private gallery, viewer and Media3 video playback
- Trash, restore and permanent deletion
- Custom albums and album assignment
- Security settings and decrypted preview-cache cleanup
- Screenshot protection and background cache cleanup

## Portable backup/restore (feature branch)
- Recovery Key generation, show/hide and copy UI
- Encrypted backup export using Android Storage Access Framework
- Authenticated portable encryption envelope using AES-GCM and HKDF-SHA256
- Versioned ZIP archive and JSON manifest
- SHA-256 and byte-size checks for every media payload
- Path/duplicate-entry checks and archive limits
- Backup verification
- Restore picker and confirmation dialog
- Validate archive before extracting payloads into app-private staging
- Re-encrypt restored media using the receiving device's Android Keystore
- Import media and albums in a Room transaction
- Remap media/albums to fresh IDs; preserve album associations and trash status
- Cleanup partial encrypted files on import/database failure

## Remaining before production
- Build and CI verification (not yet confirmed)
- Automated tests for wrong key, corrupted backup, interruption, storage exhaustion, large media and malformed manifests
- Manual device testing on supported Android versions
- Crash-safe plaintext staging/cache cleanup review
- Recovery Key onboarding/confirmation and progress/cancel UX
- Full security audit

## Build
Debug build workflow: .github/workflows/android-build.yml
