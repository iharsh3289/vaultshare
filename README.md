# VaultShare

VaultShare is a Java 21 and Spring Boot based ephemeral content sharing service for files, paste snippets, and short links. It stores metadata in SQLite and uploaded content on disk, with Docker-first deployment support for local, VPS, and PaaS environments.

## Features

- Upload files, multiple-file ZIP bundles, or text snippets
- Optional password protection for the site and individual links
- Burn-after-open links
- Expiring links
- URL shortener behavior for text uploads that contain a valid `http://` or `https://` URL
- SQLite persistence
- Docker and Docker Compose ready
- Render deploy blueprint included

## Run With Docker

```bash
cp .env.example .env
docker compose up --build
```

Open `http://localhost:8080`.

To enable site login, edit `.env`:

```env
VAULTSHARE_ENABLE_PASSWORD=true
VAULTSHARE_PASSWORD=use-a-long-password
```

## Run Locally

Java 21 and Maven are required.

```bash
mvn spring-boot:run
```

The app uses Docker volumes in Compose. When you run it directly on your machine, these local folders are used by default:

- `data/` for `settings.json` and `database.db`
- `uploads/` for uploaded content

## Configuration

You can configure runtime paths and authentication with environment variables:

```env
PORT=8080
VAULTSHARE_DATA_DIR=data
VAULTSHARE_UPLOAD_DIR=uploads
VAULTSHARE_ENABLE_PASSWORD=false
VAULTSHARE_PASSWORD=change-this-before-deploy
```

App limits are stored in `data/settings.json`:

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

## Deploy

### CI/CD

GitHub Actions runs Maven packaging and Docker image builds on every pull request and push to `main`.

When the `KOYEB_API_TOKEN` repository secret is configured, pushes to `main` also deploy the Dockerized service to Koyeb.

### Docker Host

```bash
docker build -t vaultshare .
docker run -p 8080:8080 \
  -e VAULTSHARE_ENABLE_PASSWORD=true \
  -e VAULTSHARE_PASSWORD=use-a-long-password \
  -v vaultshare-data:/app/data \
  -v vaultshare-uploads:/app/uploads \
  vaultshare
```

### Render

This repo includes `render.yaml`. Create a new Render Blueprint from the repository, then set `VAULTSHARE_PASSWORD` in the service environment.

### Koyeb Free Deployment

For a no-cost recruiter demo, deploy the Dockerfile-backed service on Koyeb's free instance and connect it to this GitHub repository.

Required service settings:

- Builder: Dockerfile
- Port: `8080`
- Route: `/`
- Branch: `main`

Recommended environment variables:

```env
PORT=8080
VAULTSHARE_DATA_DIR=data
VAULTSHARE_UPLOAD_DIR=uploads
VAULTSHARE_ENABLE_PASSWORD=false
VAULTSHARE_PASSWORD=change-this-before-deploy
```

To enable GitHub Actions deployment, create a Koyeb API token and add it to GitHub as:

```text
KOYEB_API_TOKEN
```

## Project Structure

```text
src/main/java/com/vaultshare      Java Spring Boot backend
src/main/resources/static       Frontend assets
src/main/resources/templates    HTML templates rendered by the backend
src/main/resources/data         Default settings template
data                            Runtime settings and SQLite database
uploads                         Runtime uploads
```
