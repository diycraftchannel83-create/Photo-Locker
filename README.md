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
\n## Implemented foundation\n- Android Compose dashboard and first-launch PIN gate\n- PIN verifier stored as a Keystore-encrypted blob; PIN itself is never stored\n- Temporary lockout after failed PIN attempts\n- Biometric unlock where strong biometrics are supported\n- AES-256-GCM encrypted file storage using an Android Keystore key\n- Android Photo Picker import for images and videos\n- Room metadata database for vault items\n- Private vault list with image preview and Media3 video playback\n- Trash, restore, and confirmed permanent deletion\n- Custom albums, album assignment, album browsing, and safe album deletion\n- Security settings: change PIN after verifying current PIN, clear decrypted preview cache\n- Screenshot protection and decrypted cache cleanup when the app backgrounds\n- GitHub Actions debug build workflow\n\n## Important current limitations\n- This is still an early development build; no production security audit has been performed.\n- Build verification is pending because the current environment cannot access GitHub or download Gradle.\n- Recovery/backup, final import verification, and final security audit remain in development.\n- Import copies media into the vault; original gallery files are not deleted automatically.\n