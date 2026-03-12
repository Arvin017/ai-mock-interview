import { useState, useEffect, useRef } from 'react';
import toast from 'react-hot-toast';
import { interviewAPI } from '../services/api';
import { useInterview } from '../context/InterviewContext';
import { useWebSocket } from '../hooks/useWebSocket';
import './InterviewPage.css';

const TIMER_DEFAULT = 90;

export default function InterviewPage() {
  const { state, dispatch } = useInterview();
  const { session, currentQ, feedbacks } = state;
  const questions = session?.questions || [];
  const question   = questions[currentQ];

  const [answer,    setAnswer]    = useState('');
  const [timer,     setTimer]     = useState(TIMER_DEFAULT);
  const [timerOn,   setTimerOn]   = useState(false);
  const [submitting,setSubmitting]= useState(false);
  const [wsStatus,  setWsStatus]  = useState('connecting');
  const startTimeRef = useRef(null);
  const timerRef     = useRef(null);
  const textareaRef  = useRef(null);

  const totalQ = questions.length;
  const isLast = currentQ === totalQ - 1;

  // WS: listen for real-time feedback events
  useWebSocket(session?.sessionId, (event) => {
    if (event.status === 'evaluating') {
      setWsStatus('evaluating');
    }
  });

  // Start timer when question loads
  useEffect(() => {
    if (!question) return;
    const limit = question.timeLimitSeconds || TIMER_DEFAULT;
    setTimer(limit);
    setTimerOn(true);
    setAnswer('');
    startTimeRef.current = Date.now();
    setWsStatus('ready');
    textareaRef.current?.focus();
  }, [currentQ]);

  // Countdown
  useEffect(() => {
    if (!timerOn) return;
    if (timer <= 0) { handleSubmit(true); return; }
    timerRef.current = setTimeout(() => setTimer(t => t - 1), 1000);
    return () => clearTimeout(timerRef.current);
  }, [timer, timerOn]);

  const handleSubmit = async (timedOut = false) => {
    if (submitting) return;
    clearTimeout(timerRef.current);
    setTimerOn(false);
    if (timedOut && !answer.trim()) {
      toast('⏰ Time up! Moving on...');
    }
    setSubmitting(true);
    const timeTaken = Math.floor((Date.now() - startTimeRef.current) / 1000);

    try {
      const feedback = await interviewAPI.submitAnswer({
        sessionId: session.sessionId,
        questionNumber: currentQ + 1,
        answer: answer || '(No answer provided)',
        timeTakenSeconds: timeTaken,
      });
      dispatch({ type: 'SAVE_ANSWER', payload: { questionNumber: currentQ + 1, answer, timeTaken } });
      dispatch({ type: 'ADD_FEEDBACK', payload: feedback });
    } catch (err) {
      toast.error('Evaluation failed: ' + err.message);
      setTimerOn(true);
    } finally {
      setSubmitting(false);
    }
  };

  const handleNext = async () => {
    if (isLast) {
      // Complete session
      dispatch({ type: 'SET_LOADING', payload: true });
      try {
        const dashboard = await interviewAPI.completeSession(session.sessionId);
        dispatch({ type: 'SET_DASHBOARD', payload: dashboard });
      } catch (err) {
        toast.error('Failed to complete: ' + err.message);
        dispatch({ type: 'SET_LOADING', payload: false });
      }
    } else {
      dispatch({ type: 'NEXT_QUESTION' });
    }
  };

  // FEEDBACK PANEL (shown after answering)
  if (state.stage === 'feedback') {
    const fb = feedbacks[feedbacks.length - 1];
    return (
      <div className="interview-page">
        <div className="progress-bar-wrap">
          <div className="progress-bar-inner" style={{ width: `${((currentQ + 1) / totalQ) * 100}%` }} />
          <span className="progress-label">Q{currentQ + 1} of {totalQ}</span>
        </div>

        <div className="feedback-panel">
          <div className="fb-score-badge" style={{ background: scoreColor(fb.score) }}>
            {fb.score}<span>/100</span>
          </div>
          <h2 className="fb-title">AI Feedback</h2>

          <div className="fb-section your-answer">
            <div className="section-tag">YOUR ANSWER</div>
            <p>{fb.userAnswer}</p>
          </div>

          <div className="fb-section feedback">
            <div className="section-tag">FEEDBACK</div>
            <p>{fb.feedback}</p>
          </div>

          <div className="fb-row">
            <div className="fb-section positives">
              <div className="section-tag green">✓ STRENGTHS</div>
              <ul>{fb.positives?.map((p, i) => <li key={i}>{p}</li>)}</ul>
            </div>
            <div className="fb-section improvements">
              <div className="section-tag orange">↑ IMPROVE</div>
              <ul>{fb.improvements?.map((p, i) => <li key={i}>{p}</li>)}</ul>
            </div>
          </div>

          <div className="fb-section model-answer">
            <div className="section-tag blue">💡 IDEAL ANSWER</div>
            <p>{fb.modelAnswer}</p>
          </div>

          <button className="next-btn" onClick={handleNext}>
            {isLast ? '🏁 View Full Results' : `Next Question →`}
          </button>
        </div>
      </div>
    );
  }

  if (!question) return null;
  const pct = (timer / (question.timeLimitSeconds || TIMER_DEFAULT)) * 100;
  const timerColor = timer > 30 ? '#00e5a0' : timer > 10 ? '#ffd166' : '#ff6b6b';

  return (
    <div className="interview-page">
      {/* Progress */}
      <div className="progress-bar-wrap">
        <div className="progress-bar-inner" style={{ width: `${(currentQ / totalQ) * 100}%` }} />
        <span className="progress-label">Q{currentQ + 1} of {totalQ}</span>
      </div>

      <div className="interview-layout">
        {/* Left: Question */}
        <div className="question-card">
          <div className="q-meta">
            <span className={`q-type-badge ${question.type}`}>{question.type}</span>
            <span className={`q-diff-badge ${question.difficulty}`}>{question.difficulty}</span>
            <span className="q-number">#{currentQ + 1}</span>
          </div>

          <h2 className="question-text">{question.question}</h2>

          <div className="q-tips">
            <p>💡 <strong>Tips:</strong> Structure your answer clearly. Use specific examples. STAR method works great for behavioral questions.</p>
          </div>

          {/* Previous feedbacks sidebar */}
          {feedbacks.length > 0 && (
            <div className="prev-scores">
              <div className="prev-scores-title">Previous Scores</div>
              {feedbacks.map((f, i) => (
                <div key={i} className="prev-score-item">
                  <span>Q{f.questionNumber}</span>
                  <div className="mini-bar">
                    <div style={{ width: `${f.score}%`, background: scoreColor(f.score) }} />
                  </div>
                  <span style={{ color: scoreColor(f.score) }}>{f.score}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right: Answer panel */}
        <div className="answer-panel">
          {/* Timer */}
          <div className="timer-area">
            <svg className="timer-ring" viewBox="0 0 80 80">
              <circle cx="40" cy="40" r="34" fill="none" stroke="#1a1a2e" strokeWidth="6" />
              <circle cx="40" cy="40" r="34" fill="none" stroke={timerColor} strokeWidth="6"
                strokeDasharray={`${2 * Math.PI * 34 * pct / 100} ${2 * Math.PI * 34}`}
                strokeLinecap="round"
                style={{ transform: 'rotate(-90deg)', transformOrigin: '40px 40px', transition: 'stroke-dasharray 1s linear, stroke 0.3s' }} />
              <text x="40" y="40" textAnchor="middle" dominantBaseline="middle"
                fill={timerColor} fontSize="16" fontWeight="700" fontFamily="monospace">{timer}</text>
            </svg>
            <span className="timer-label">{timer > 0 ? 'seconds left' : "time's up!"}</span>
          </div>

          <textarea
            ref={textareaRef}
            className="answer-textarea"
            value={answer}
            onChange={e => setAnswer(e.target.value)}
            placeholder="Type your answer here... Be detailed and specific. Use examples from your experience."
            rows={12}
          />

          <div className="answer-footer">
            <span className="char-count">{answer.length} chars</span>
            <button
              className="submit-btn"
              onClick={() => handleSubmit(false)}
              disabled={submitting}
            >
              {submitting ? (
                <><span className="spinner sm" /> Evaluating...</>
              ) : (
                <>Submit Answer ✓</>
              )}
            </button>
          </div>

          {wsStatus === 'evaluating' && (
            <div className="ws-status">🤖 AI is evaluating your answer in real-time...</div>
          )}
        </div>
      </div>
    </div>
  );
}

function scoreColor(score) {
  if (score >= 75) return '#00e5a0';
  if (score >= 50) return '#ffd166';
  return '#ff6b6b';
}
