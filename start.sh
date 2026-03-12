#!/bin/bash
# ─────────────────────────────────────────────────────────
# AI Mock Interview Platform — Quick Start Script
# Usage: chmod +x start.sh && ./start.sh
# ─────────────────────────────────────────────────────────

echo ""
echo "🤖 AI Mock Interview Platform"
echo "================================"
echo ""

# Check ANTHROPIC_API_KEY
if [ -z "$ANTHROPIC_API_KEY" ]; then
  echo "⚠️  ANTHROPIC_API_KEY not set!"
  echo "   Set it with: export ANTHROPIC_API_KEY=sk-ant-your-key"
  echo ""
  read -p "   Enter API key now (or press Enter to skip): " key
  if [ -n "$key" ]; then
    export ANTHROPIC_API_KEY="$key"
  fi
fi

echo ""
echo "▶ Starting Spring Boot backend on port 8080..."
cd backend
mvn spring-boot:run -q &
BACKEND_PID=$!
echo "  Backend PID: $BACKEND_PID"

echo ""
echo "⏳ Waiting for backend to start..."
sleep 12

echo ""
echo "▶ Starting React frontend on port 3000..."
cd ../frontend
npm install --silent
npm start &
FRONTEND_PID=$!

echo ""
echo "✅ Both services started!"
echo ""
echo "   Frontend: http://localhost:3000"
echo "   Backend:  http://localhost:8080"
echo "   API docs: See README.md"
echo ""
echo "Press Ctrl+C to stop both services"
echo ""

# Cleanup on exit
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; echo 'Stopped.'" EXIT
wait
