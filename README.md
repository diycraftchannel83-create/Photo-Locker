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
- GitHub Actions debug build workflow
- Portable backup crypto envelope using a random recovery key and HKDF-SHA256
- ZIP archive writer for media streams and JSON manifest, encrypted before export
- Recovery Key generation, display/hide and clipboard copy UI\n- Encrypted backup export through Android Storage Access Framework\n- Backup verification: authenticated decrypt, manifest/path checks, SHA-256 and payload-size validation\n- ZIP entry, media-count, manifest, individual file and total expanded-size limits

## Current limitations
- Early development; production security audit not performed.
- Build verification remains pending.
- Backup engine is not yet wired to Room, SAF UI, recovery-key onboarding, or transactional restore.
- Restore must stage and validate all payloads before committing vault files and metadata.
- Import does not automatically delete original gallery files.
