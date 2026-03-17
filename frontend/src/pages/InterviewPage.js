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

    const [answer,     setAnswer]     = useState('');
    const [timer,      setTimer]      = useState(TIMER_DEFAULT);
    const [timerOn,    setTimerOn]    = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [wsStatus,   setWsStatus]   = useState('ready');
    const startTimeRef = useRef(null);
    const timerRef     = useRef(null);
    const textareaRef  = useRef(null);

    const totalQ = questions.length;
    const isLast = currentQ === totalQ - 1;

    useWebSocket(session?.sessionId, (event) => {
        if (event.status === 'evaluating') setWsStatus('evaluating');
    });

    useEffect(() => {
        if (!question) return;
        const limit = question.timeLimitSeconds || TIMER_DEFAULT;
        setTimer(limit);
        setTimerOn(true);
        setAnswer('');
        setWsStatus('ready');
        startTimeRef.current = Date.now();
        textareaRef.current?.focus();
    }, [currentQ]);

    useEffect(() => {
        if (!timerOn) return;
        if (timer <= 0) { handleSubmit(true); return; }
        timerRef.current = setTimeout(() => setTimer(t => t - 1), 1000);
        return () => clearTimeout(timerRef.current);
    }, [timer, timerOn]);

    const handleSubmit = async (timedOut = false, skipped = false) => {
        if (submitting) return;
        clearTimeout(timerRef.current);
        setTimerOn(false);
        if (timedOut && !answer.trim()) toast('⏰ Time up! Moving on...');
        if (skipped) toast('⏭ Question skipped');
        setSubmitting(true);
        const timeTaken = Math.floor((Date.now() - startTimeRef.current) / 1000);
        const finalAnswer = skipped ? '(Skipped)' : answer || '(No answer provided)';

        try {
            const feedback = await interviewAPI.submitAnswer({
                sessionId: session.sessionId,
                questionNumber: currentQ + 1,
                answer: finalAnswer,
                timeTakenSeconds: timeTaken,
            });
            feedback.questionText = question.question;
            feedback.timeTaken    = timeTaken;
            dispatch({ type: 'SAVE_ANSWER', payload: { questionNumber: currentQ + 1, answer: finalAnswer, timeTaken } });
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

    // FEEDBACK PANEL
    if (state.stage === 'feedback') {
        const fb = feedbacks[feedbacks.length - 1];
        const sc = fb.score;
        const grade = sc >= 85 ? 'A' : sc >= 70 ? 'B' : sc >= 55 ? 'C' : 'D';
        const gradeLabel = { A: 'Excellent!', B: 'Good Job', C: 'Average', D: 'Needs Work' }[grade];

        return (
            <div className="interview-page">
                <div className="progress-bar-wrap">
                    <div className="progress-bar-inner" style={{ width: `${((currentQ + 1) / totalQ) * 100}%` }} />
                    <span className="progress-label">Q{currentQ + 1} of {totalQ}</span>
                </div>

                <div className="feedback-panel">
                    <div className="fb-header">
                        <div className="fb-score-ring-wrap">
                            <svg width="90" height="90" viewBox="0 0 90 90">
                                <circle cx="45" cy="45" r="38" fill="none" stroke="#1a1a2e" strokeWidth="7" />
                                <circle cx="45" cy="45" r="38" fill="none" stroke={scoreColor(sc)} strokeWidth="7"
                                        strokeDasharray={`${2 * Math.PI * 38 * sc / 100} ${2 * Math.PI * 38}`}
                                        strokeLinecap="round"
                                        style={{ transform: 'rotate(-90deg)', transformOrigin: '45px 45px', transition: 'stroke-dasharray 1.2s ease' }} />
                                <text x="45" y="45" textAnchor="middle" dominantBaseline="middle"
                                      fill={scoreColor(sc)} fontSize="18" fontWeight="800" fontFamily="monospace">{sc}</text>
                            </svg>
                            <div>
                                <div className="fb-grade" style={{ color: scoreColor(sc) }}>{grade} — {gradeLabel}</div>
                                <div className="fb-q-label">Question {currentQ + 1} of {totalQ}</div>
                                {fb.timeTaken && <div className="fb-time">⏱ Answered in {fb.timeTaken}s</div>}
                            </div>
                        </div>
                    </div>

                    {fb.questionText && (
                        <div className="fb-section question-asked">
                            <div className="section-tag purple">❓ QUESTION</div>
                            <p>{fb.questionText}</p>
                        </div>
                    )}

                    <div className="fb-section your-answer">
                        <div className="section-tag">YOUR ANSWER</div>
                        <p>{fb.userAnswer}</p>
                    </div>

                    <div className="fb-section feedback-text">
                        <div className="section-tag">AI FEEDBACK</div>
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
                        {isLast ? '🏁 View Full Results' : 'Next Question →'}
                    </button>
                </div>
            </div>
        );
    }

    // INTERVIEW PANEL
    if (!question) return null;
    const timeLimit = question.timeLimitSeconds || TIMER_DEFAULT;
    const pct = (timer / timeLimit) * 100;
    const timerColor = timer > timeLimit * 0.5 ? '#00e5a0' : timer > timeLimit * 0.2 ? '#ffd166' : '#ff6b6b';
    const sz = 64, r2 = 26, circ = 2 * Math.PI * r2;

    return (
        <div className="interview-page">
            <div className="progress-bar-wrap">
                <div className="progress-bar-inner" style={{ width: `${(currentQ / totalQ) * 100}%` }} />
                <span className="progress-label">Q{currentQ + 1} of {totalQ}</span>
            </div>

            <div className="interview-layout">
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
                    {feedbacks.length > 0 && (
                        <div className="prev-scores">
                            <div className="prev-scores-title">Previous Scores</div>
                            {feedbacks.map((f, i) => (
                                <div key={i} className="prev-score-item">
                                    <span>Q{f.questionNumber}</span>
                                    <div className="mini-bar"><div style={{ width: `${f.score}%`, background: scoreColor(f.score) }} /></div>
                                    <span style={{ color: scoreColor(f.score) }}>{f.score}</span>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="answer-panel">
                    <div className="timer-area">
                        <svg width={sz} height={sz} viewBox={`0 0 ${sz} ${sz}`}>
                            <circle cx={sz/2} cy={sz/2} r={r2} fill="none" stroke="#1a1a2e" strokeWidth="5" />
                            <circle cx={sz/2} cy={sz/2} r={r2} fill="none" stroke={timerColor} strokeWidth="5"
                                    strokeDasharray={`${circ * pct / 100} ${circ}`} strokeLinecap="round"
                                    style={{ transform: `rotate(-90deg)`, transformOrigin: `${sz/2}px ${sz/2}px`, transition: 'stroke-dasharray 1s linear, stroke 0.3s' }} />
                            <text x={sz/2} y={sz/2} textAnchor="middle" dominantBaseline="middle"
                                  fill={timerColor} fontSize="14" fontWeight="700" fontFamily="monospace">{timer}</text>
                        </svg>
                        <div className="timer-info">
                            <span className="timer-label" style={{ color: timerColor }}>{timer > 0 ? `${timer}s left` : "Time's up!"}</span>
                            <span className="timer-sublabel">{timeLimit}s total</span>
                        </div>
                        <button className="skip-btn" onClick={() => handleSubmit(false, true)} disabled={submitting}>⏭ Skip</button>
                    </div>

                    <textarea ref={textareaRef} className="answer-textarea" value={answer}
                              onChange={e => setAnswer(e.target.value)}
                              placeholder="Type your answer here... Be detailed and specific. Use examples from your experience."
                              rows={12} />

                    <div className="answer-footer">
            <span className="char-count">
              {answer.length} chars
                {answer.length > 0 && answer.length < 50 && <span className="char-hint"> · write more for better score</span>}
            </span>
                        <button className="submit-btn" onClick={() => handleSubmit(false)} disabled={submitting}>
                            {submitting ? <><span className="spinner sm" /> Evaluating...</> : <>Submit Answer ✓</>}
                        </button>
                    </div>
                    {wsStatus === 'evaluating' && <div className="ws-status">🤖 AI is evaluating your answer in real-time...</div>}
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
