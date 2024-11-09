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

### 3. Get Render Deploy Hook URL

In Render:

1. Open your web service dashboard.
2. Go to Settings.
3. Find the "Deploy Hook" section.
4. Copy the deploy hook URL.

### 4. Add GitHub Secret

In GitHub:

1. Open the repository.
2. Go to `Settings`.
3. Open `Secrets and variables` -> `Actions`.
4. Add a repository secret:

```text
RENDER_DEPLOY_HOOK_URL
```

Paste the deploy hook URL as the value.

### 5. Trigger Deployment

Push to `main`:

```bash
git push origin main
```

GitHub Actions will:

1. Build the Maven project.
2. Build the Docker image.
3. If build succeeds, trigger Render deployment via the deploy hook.

### 6. Add Live Demo Link

After Render creates the public URL, update the top of `README.md`:

```md
**Live Demo:** https://your-render-url.onrender.com
```

Then commit and push:

```bash
git add README.md
git commit -m "docs: add live demo link"
git push origin main
```

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
