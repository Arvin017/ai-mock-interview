# 🤖 AI Mock Interview Platform

A full-stack placement project featuring:
- **Resume upload** (PDF / DOCX / TXT) with AI-powered parsing
- **Personalized question generation** based on your resume + domain
- **Real-time AI feedback** after each answer via WebSocket
- **Score dashboard** with radar charts, bar charts, weak area analysis

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2 (Java 17) |
| AI | Anthropic Claude API (claude-haiku) |
| WebSocket | STOMP over SockJS |
| Database | H2 in-memory (swap to PostgreSQL for prod) |
| Frontend | React 18 + Recharts + Framer Motion |
| File Parsing | Apache PDFBox + Apache POI |

---

## ⚙️ Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+
- Anthropic API key (get at https://console.anthropic.com)

---

### 1. Backend

```bash
cd backend

# Set your API key
export ANTHROPIC_API_KEY=sk-ant-your-key-here

# Build and run
mvn spring-boot:run
```

Backend starts at **http://localhost:8080**

H2 console at **http://localhost:8080/h2-console** (URL: `jdbc:h2:mem:interviewdb`)

---

### 2. Frontend

```bash
cd frontend

npm install

# Optional: set backend URL (default: http://localhost:8080)
# REACT_APP_API_URL=http://localhost:8080/api/interview

npm start
```

Frontend starts at **http://localhost:3000**

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/interview/upload-resume` | Upload PDF/DOCX/TXT resume |
| POST | `/api/interview/start` | Start session, get 5 AI questions |
| POST | `/api/interview/submit-answer` | Submit answer, get AI feedback |
| POST | `/api/interview/complete/{id}` | Complete session, get full analysis |
| GET  | `/api/interview/dashboard/{id}` | Fetch session dashboard |
| GET  | `/api/interview/health` | Health check |

### WebSocket
- Connect to: `ws://localhost:8080/ws` (SockJS)
- Subscribe to: `/topic/feedback/{sessionId}` for real-time feedback events

---

## 📁 Project Structure

```
ai-mock-interview/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/interview/
│       ├── AiMockInterviewApplication.java
│       ├── config/
│       │   ├── WebSocketConfig.java
│       │   └── CorsConfig.java
│       ├── controller/
│       │   ├── InterviewController.java
│       │   └── WebSocketController.java
│       ├── service/
│       │   ├── AiService.java          ← Anthropic API calls
│       │   ├── InterviewService.java   ← Core interview logic
│       │   └── ResumeParserService.java
│       ├── model/
│       │   ├── InterviewSession.java
│       │   ├── QuestionAnswer.java
│       │   └── Repositories.java
│       └── dto/
│           └── InterviewDTOs.java
│
└── frontend/
    └── src/
        ├── App.js
        ├── index.js
        ├── context/InterviewContext.js
        ├── hooks/useWebSocket.js
        ├── services/api.js
        └── pages/
            ├── UploadPage.js      ← Resume upload + domain select
            ├── InterviewPage.js   ← Question + answer + feedback
            └── DashboardPage.js   ← Scores, charts, analysis
```

---

## 🎯 Features

### Upload Screen
- Drag & drop resume (PDF, DOCX, TXT)
- Or paste text directly
- Select interview domain (SWE, Data Science, PM, DevOps, Frontend)

### Interview Screen
- Live countdown timer per question
- Question type badges (Technical / Behavioral / Situational)
- Previous scores shown in sidebar
- Real-time WebSocket status indicator

### Feedback Panel (after each answer)
- Score 0–100
- AI strengths & improvement suggestions
- Model ideal answer
- Expandable per-question breakdown

### Dashboard
- Overall grade (A/B/C/D)
- 4-axis skill scoring (Technical, Communication, Confidence, Problem Solving)
- Radar chart + bar chart
- AI overall assessment paragraph
- Weak areas & strengths list
- Full per-question accordion breakdown
- Print report button

---

## 🔧 Switching to OpenAI

In `application.properties`, you can configure OpenAI instead:

```properties
openai.api.key=sk-your-openai-key
openai.model=gpt-4o-mini
```

Then update `AiService.java` to call OpenAI's `/v1/chat/completions` endpoint.

---

## 🚀 Production Deployment

1. Replace H2 with PostgreSQL:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/interviewdb
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

2. Build frontend for production:
```bash
cd frontend && npm run build
```

3. Serve React build from Spring Boot by copying `build/` to `backend/src/main/resources/static/`

---


## Live Backend API

Swagger UI:
https://ai-mock-interview-g9dz.onrender.com/swagger-ui/index.html

Base API:
https://ai-mock-interview-g9dz.onrender.com

Note: Backend is deployed on Render free tier. 
The first request may take 30–60 seconds to wake the server.
## 📝 License
MIT — Free to use for placement projects and portfolios.
