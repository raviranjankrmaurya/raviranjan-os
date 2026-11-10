---
title: Raviranjan OS
emoji: 🚀
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
---

# Raviranjan.OS — Personal Developer Operating System & Headless CMS

> **A production-grade developer platform combining public technical portfolio, headless personal CMS, architectural decision records (ADRs), real-time external telemetry (GitHub, LeetCode, GFG), Contact Intelligence & AI Abuse Moderation, and a grounded RAG assistant.**

## 1. Quick Start (Single Command)

### Prerequisites
* Docker Engine & Docker Compose (v2.0+)

### Start Full Stack:
```bash
docker compose up --build -d
```

### Access Endpoints:
* **Public Developer OS**: `http://localhost:3000` (or `http://localhost:8080/index.html`)
* **Admin Operating System**: `http://localhost:3000/admin` (or `http://localhost:8080/admin_cms.html`)
* **Spring Boot REST APIs**: `http://localhost:8080`
* **OpenAPI 3.0 / Swagger UI**: `http://localhost:8080/swagger-ui.html`

## 2. 100% Free Production Deployment

### Option 1: Render.com (Free Web Service)
1. Sign up on [render.com](https://render.com) using GitHub (no credit card required).
2. Create **New +** -> **Web Service**.
3. Select your repository.
4. Settings:
   * **Root Directory**: `backend` (or leave empty to use root `Dockerfile`)
   * **Runtime**: `Docker`
   * **Instance Type**: `Free`
5. Add Environment Variables (from your `config.env`):
   * `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
   * `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PASSWORD`, `SPRING_DATA_REDIS_SSL_ENABLED`
   * `JWT_SECRET`, `GEMINI_API_KEY`
   * `GITHUB_USERNAME`, `LEETCODE_USERNAME`, `GFG_USERNAME`
6. Click **Create Web Service**. Your entire portfolio, admin CMS, and APIs will be live at `https://<your-service>.onrender.com`.

### Option 2: Hugging Face Spaces (16 GB RAM Free Docker Space)
1. Create a free account on [huggingface.co](https://huggingface.co).
2. Click **New Space** -> Space Name: `raviranjan-os` -> **Space SDK: Docker** (Blank).
3. Under **Space Settings** -> **Variables and Secrets**:
   * Add Secrets: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PASSWORD`, `JWT_SECRET`, `GEMINI_API_KEY`.
4. Push this repository to your Hugging Face Space git remote or connect your GitHub repo.
5. Hugging Face builds the root `Dockerfile` and runs it on port 7860 with 16 GB RAM!

## 3. Default Credentials & Stealth Admin Access

* **Secret Shortcut Trigger**: Press **`Ctrl + Shift + A`** anywhere on the public website (or tap top-left 'R' logo 3 times on mobile).
* **Default Username**: `raviranjankr` (or `raviranjan`)
* **Default Password**: `Admin@123456`
* **Assigned Role**: `ROLE_SUPER_ADMIN`