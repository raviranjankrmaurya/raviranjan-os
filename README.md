# Raviranjan.OS — Personal Developer Platform & Executive Admin OS

Production-grade developer operating system, live telemetry engine, and engineering portfolio for Raviranjan Kumar.

## 🚀 Key Features
- **Java 21 & Spring Boot 3**: High-concurrency backend architecture with virtual threads.
- **Neon Cloud PostgreSQL**: Cloud-native serverless relational database with HikariCP.
- **Live GitHub Scraping & Sync**: Authentic 144 contributions calendar, streaks, and repository telemetry.
- **5-Section Structured Skills Matrix**: Categorized across Languages, Backend, Databases, DevOps, and AI.
- **AI Chat Intelligence (RAG Engine)**: Gemini AI integration with user IP, location, and browser tracking.
- **Executive Admin CMS**: 15 modules with full in-place CRUD, Homepage Builder, live theme color customizer, and an "Are you sure?" delete confirmation modal.
- **Restricted Access Security**: Complete gate preventing unauthorized access to `/admin`.

---

## 🛠 Local Setup & Running

### Option A: Running with Docker Compose (Recommended)
```bash
# 1. Clone repository
git clone https://github.com/raviranjankrmaurya/raviranjan-os.git
cd raviranjan-os

# 2. Launch full stack (PostgreSQL + Redis + Backend + Frontend)
docker-compose up --build -d

# 3. Access applications:
# - Public Portfolio: http://localhost:3000
# - Executive Admin:  http://localhost:3000/admin (or press logo 3 times)
# - Spring Boot API:  http://localhost:8080/api/health
```

### Option B: Running Locally (Node.js & Maven)
```bash
# Frontend
cd frontend
npm install
npm start
# Runs on http://localhost:3000

# Backend (in separate terminal)
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

---

## 🌐 Deploying to Vercel & Render

1. **Frontend (Vercel)**:
   - Root directory: `frontend/`
   - Framework preset: `Other`
   - Rewrite rules are automatically handled by `frontend/vercel.json`.

2. **Backend (Render)**:
   - Root directory: `backend/`
   - Build command: `mvn clean package -DskipTests`
   - Start command: `java -jar target/*.jar`
   - Environment variables: Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` from your Neon DB console.

---

## 🔒 Secret Admin Access
- On public portfolio, click the **"R" logo 3 times** quickly, OR
- Open Command Palette (`Cmd+K`) and type `sudo admin` ↵.
