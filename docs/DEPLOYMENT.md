# Deployment Guide

This guide covers the recommended free deployment path for a public demo.

## Recommended: Koyeb Free Instance

Koyeb can deploy this project directly from GitHub using the included Dockerfile.

### 1. Push to GitHub

```bash
git push origin main
git push origin feature/koyeb-cicd docs/project-polish
```

### 2. Create a Koyeb API Token

In Koyeb:

1. Open account or organization settings.
2. Go to API access tokens.
3. Create a token.

### 3. Add GitHub Secret

In GitHub:

1. Open the repository.
2. Go to `Settings`.
3. Open `Secrets and variables` -> `Actions`.
4. Add a repository secret:

```text
KOYEB_API_TOKEN
```

### 4. Trigger Deployment

Push to `main`:

```bash
git push origin main
```

GitHub Actions will:

1. Build the Maven project.
2. Build the Docker image.
3. Deploy the app to Koyeb.

### 5. Add Live Demo Link

After Koyeb creates the public URL, update the top of `README.md`:

```md
**Live Demo:** https://your-koyeb-url.koyeb.app
```

Then commit and push:

```bash
git add README.md
git commit -m "docs: add live demo link"
git push origin main
```

## Koyeb Settings

The GitHub Actions workflow uses:

```text
Builder: Docker
Port: 8080
Route: /
Health check: /healthz
Region: fra
Instance type: free
```

Runtime environment:

```env
PORT=8080
VAULTSHARE_DATA_DIR=/app/data
VAULTSHARE_UPLOAD_DIR=/app/uploads
VAULTSHARE_ENABLE_PASSWORD=false
```

## Free Tier Notes

The free deployment is intended for demos. Free instances may scale to zero after inactivity, and local disk should not be treated as durable production storage.

For production, use external object storage and a managed database.
