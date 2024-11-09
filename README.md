# VaultShare

[![CI/CD](https://github.com/iharsh3289/vaultshare/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/iharsh3289/vaultshare/actions/workflows/ci-cd.yml)

VaultShare is a Dockerized Java 21 Spring Boot service for ephemeral file, paste, and short-link sharing. It supports expiring resources, burn-after-open links, password-protected shares, SQLite metadata persistence, and GitHub Actions based CI/CD.

**Live Demo:** Add the Koyeb URL here after first deploy  
**Repository:** https://github.com/iharsh3289/vaultshare

## Why This Project

VaultShare is designed as a compact production-style backend project: clear service boundaries, Docker-first deployment, health checks, CI/CD automation, runtime configuration, and persistent metadata. It is intentionally small enough to review quickly, but complete enough to demonstrate backend ownership.

## Features

- Upload files, multiple-file ZIP bundles, or text snippets
- Generate short, shareable links
- Optional password protection for individual links
- Optional site-level upload protection
- Burn-after-open links
- Expiring links with scheduled cleanup
- URL shortener behavior for valid `http://` or `https://` text uploads
- SQLite-backed metadata storage
- Docker, Docker Compose, Koyeb, and Render deployment support

## Tech Stack

- Java 21
- Spring Boot 3
- SQLite
- Maven
- Docker
- GitHub Actions
- Koyeb free web service deployment

## Architecture

```text
Client
  |
  v
Spring Boot Controller
  |
  +-- AuthService          site/session authentication
  +-- StorageService       file, zip, and text persistence
  +-- CryptoService        PBKDF2 + AES/CTR protected shares
  +-- DataRepository       SQLite metadata access
  +-- ExpirationService    scheduled cleanup
  +-- TemplateService      server-rendered HTML responses
```

More detail: [Architecture Notes](docs/ARCHITECTURE.md)

## Run Locally

Java 21 and Maven are required.

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

## Run With Docker

```bash
cp .env.example .env
docker compose up --build
```

Open:

```text
http://localhost:8080
```

## Configuration

Runtime configuration:

```env
PORT=8080
VAULTSHARE_DATA_DIR=data
VAULTSHARE_UPLOAD_DIR=uploads
VAULTSHARE_ENABLE_PASSWORD=false
VAULTSHARE_PASSWORD=change-this-before-deploy
```

Application limits are stored in `data/settings.json`:

- `fileSizeLimitMB`
- `textSizeLimitMB`
- `streamSizeLimitKB`
- `streamThrottleMS`
- `pbkdf2Iterations`
- `cmdUploadDefaultDurationMinute`
- `enablePassword`
- `password`

Prefer environment variables for production passwords instead of committing secrets.

## Curl Upload

```bash
curl -F file=@pom.xml -F duration=10 -F pass=123 -F burn=true http://localhost:8080
```

If site login is enabled:

```bash
curl -F file=@file.txt -F auth=your-site-password http://localhost:8080
```

## CI/CD

GitHub Actions runs on pull requests and pushes to `main`:

- Builds the Spring Boot application with Maven
- Builds the Docker image
- Deploys to Koyeb when `KOYEB_API_TOKEN` is configured

Workflow: [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml)

## Deployment

Recommended free public demo deployment: **Koyeb free instance**.

Quick setup:

1. Push this repository to GitHub.
2. Create a Koyeb API token.
3. Add it to GitHub Actions secrets as `KOYEB_API_TOKEN`.
4. Push to `main`.
5. Copy the generated Koyeb public URL into the **Live Demo** line at the top of this README.

Full guide: [Deployment Guide](docs/DEPLOYMENT.md)

## Project Structure

```text
src/main/java/com/vaultshare      Java Spring Boot backend
src/main/resources/static         Frontend assets
src/main/resources/templates      HTML templates rendered by the backend
src/main/resources/data           Default settings template
data                              Runtime settings and SQLite database
uploads                           Runtime uploads
docs                              Architecture and deployment notes
```

## Notes

Koyeb free instances are suitable for demos and hobby usage. Free deployments can scale to zero and do not provide durable attached volumes, so uploaded files should be treated as demo data. For production use, the next step would be external object storage and managed database persistence.
