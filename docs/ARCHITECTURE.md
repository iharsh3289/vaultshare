# Architecture Notes

VaultShare is organized as a small layered Spring Boot application. The controller handles HTTP concerns, while storage, authentication, encryption, and metadata operations live in dedicated services.

## Request Flow

```text
Browser / curl
  |
  v
VaultShareController
  |
  +-- AuthService
  +-- StorageService
  +-- CryptoService
  +-- DataRepository
  +-- TemplateService
```

## Core Components

- `VaultShareController`: upload, text paste, authentication, download, and health endpoints.
- `AuthService`: site-level session authentication using HTTP-only cookies.
- `StorageService`: writes single files, ZIP bundles, and text snippets to disk.
- `CryptoService`: derives keys with PBKDF2 and encrypts protected shares with AES/CTR.
- `DataRepository`: stores metadata in SQLite.
- `ExpirationService`: periodically deletes expired metadata and files.
- `TemplateService`: renders lightweight HTML responses for paste, auth, and not-found views.

## Persistence Model

SQLite stores metadata:

- share id
- content type
- filename
- file path
- burn-after-open flag
- expiration timestamp
- password/encryption salts and hashes

Uploaded content is stored on disk under the configured upload directory.

## Security Notes

- Site-level password can be enabled with `VAULTSHARE_ENABLE_PASSWORD=true`.
- Per-link passwords protect individual shares.
- Link passwords are not stored directly; derived hashes and salts are stored.
- Protected content is encrypted before being written to disk.
- Runtime secrets should be passed as environment variables, not committed.

## Production Upgrade Path

For production hardening, the next improvements would be:

- S3-compatible object storage for uploaded content
- Managed Postgres for metadata
- Rate limiting
- Audit logging
- Antivirus scanning for uploads
- Signed URLs for download paths
