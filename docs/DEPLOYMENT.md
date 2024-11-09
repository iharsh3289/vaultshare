# Deployment Guide

This guide covers the recommended deployment path for Render using the included `render.yaml` manifest.

## Recommended: Render Free Instance

Render can deploy this project directly from GitHub using the included `render.yaml` file and the existing Dockerfile.

### 1. Push to GitHub

```bash
git push origin main
```

### 2. Connect the Repository on Render

In Render:

1. Create or sign in to your Render account.
2. Click **New** -> **Web Service**.
3. Connect your GitHub repository.
4. Select the `main` branch.
5. Choose **Docker** as the environment.
6. Use the default root directory and enable auto deploy for pushes to `main`.

### 3. Use the Included `render.yaml`

Render will detect the `render.yaml` file at the repository root and use it to configure the service.

The manifest includes:

- Docker runtime
- environment variables for `PORT`, `VAULTSHARE_DATA_DIR`, `VAULTSHARE_UPLOAD_DIR`, and password settings
- a persistent disk mounted at `/app/storage`

### 4. Set Environment Variables in Render

Configure the following environment variables in the Render service settings:

```text
PORT=8080
VAULTSHARE_DATA_DIR=/app/storage/data
VAULTSHARE_UPLOAD_DIR=/app/storage/uploads
VAULTSHARE_ENABLE_PASSWORD=true
VAULTSHARE_PASSWORD=<your-secret-password>
```

If you want the service to run without site-level password protection, set:

```text
VAULTSHARE_ENABLE_PASSWORD=false
```

### 5. Deploy and Verify

After connecting the repo and enabling auto deploy:

1. Push to `main`.
2. Render will build the Docker image and deploy the service.
3. Verify the public URL in the Render dashboard.

### 6. Add the Render URL to `README.md`

After deployment, update the top of `README.md`:

```md
**Live Demo:** https://your-render-url.onrender.com
```

Then commit and push the change.

## CI/CD Pipeline

The repository already includes GitHub Actions at `.github/workflows/ci-cd.yml`.

That workflow:

- builds the Spring Boot app with Maven
- builds the Docker image
- verifies every PR and `main` push

Render deployment is handled by Render via the GitHub repo connection and `render.yaml` manifest. This means the project has both a GitHub build pipeline and Render auto-deploy for production.

## Render Notes

Render free instances are suitable for demos and hobby projects. Render may sleep idle services on the free tier, and attached disk storage should still be treated as demo persistence.

For production use, consider external object storage and a managed database for critical data.
