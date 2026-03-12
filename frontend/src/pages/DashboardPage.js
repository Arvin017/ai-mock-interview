import { useState } from 'react';
import { RadarChart, Radar, PolarGrid, PolarAngleAxis, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Cell } from 'recharts';
import { useInterview } from '../context/InterviewContext';
import './DashboardPage.css';

function ScoreRing({ score, label, color, size = 90 }) {
  const r = size * 0.4, cx = size / 2, cy = size / 2;
  const circ = 2 * Math.PI * r;
  const dash = circ * (score / 100);
  return (
    <div className="score-ring-wrap">
      <svg width={size} height={size}>
        <circle cx={cx} cy={cy} r={r} fill="none" stroke="#1a1a2e" strokeWidth={size * 0.07} />
        <circle cx={cx} cy={cy} r={r} fill="none" stroke={color} strokeWidth={size * 0.07}
          strokeDasharray={`${dash} ${circ}`} strokeLinecap="round"
          style={{ transform: `rotate(-90deg)`, transformOrigin: `${cx}px ${cy}px`, transition: 'stroke-dasharray 1.4s cubic-bezier(.4,0,.2,1)' }} />
        <text x={cx} y={cy} textAnchor="middle" dominantBaseline="middle"
          fill={color} fontSize={size * 0.2} fontWeight="800" fontFamily="monospace">{score}</text>
      </svg>
      <span className="ring-label">{label}</span>
    </div>
  );
}

export default function DashboardPage() {
  const { state, dispatch } = useInterview();
  const { dashboard } = state;
  const [activeQ, setActiveQ] = useState(null);

  if (!dashboard) return <div className="dashboard-loading"><div className="spinner lg" /><p>Loading results...</p></div>;

  const d = dashboard;

  const radarData = [
    { subject: 'Technical',      value: d.technicalScore       || 0 },
    { subject: 'Communication',  value: d.communicationScore   || 0 },
    { subject: 'Confidence',     value: d.confidenceScore      || 0 },
    { subject: 'Problem Solving',value: d.problemSolvingScore  || 0 },
  ];

  const barData = d.questionFeedbacks?.map(qf => ({
    name: `Q${qf.questionNumber}`,
    score: qf.score,
  })) || [];

  const overall = Math.round(d.overallScore || 0);
  const overallColor = overall >= 75 ? '#00e5a0' : overall >= 50 ? '#ffd166' : '#ff6b6b';
  const grade = overall >= 85 ? 'A' : overall >= 70 ? 'B' : overall >= 55 ? 'C' : 'D';
  const gradeLabel = { A: 'Excellent', B: 'Good', C: 'Average', D: 'Needs Work' }[grade];

  return (
    <div className="dashboard-page">
      {/* Header */}
      <div className="dash-header">
        <div>
          <div className="dash-badge">INTERVIEW RESULTS</div>
          <h1 className="dash-title">{d.candidateName}</h1>
          <p className="dash-sub">{d.domain?.toUpperCase()} · {new Date(d.completedAt).toLocaleDateString()}</p>
        </div>
        <div className="grade-badge" style={{ borderColor: overallColor, color: overallColor }}>
          <div className="grade-letter">{grade}</div>
          <div className="grade-label">{gradeLabel}</div>
        </div>
      </div>

      {/* Score Cards Row */}
      <div className="score-cards">
        <div className="score-card main">
          <ScoreRing score={overall} label="Overall" color={overallColor} size={110} />
        </div>
        <div className="score-card">
          <ScoreRing score={Math.round(d.technicalScore || 0)} label="Technical" color="#7c6af7" size={80} />
        </div>
        <div className="score-card">
          <ScoreRing score={Math.round(d.communicationScore || 0)} label="Comms" color="#06b6d4" size={80} />
        </div>
        <div className="score-card">
          <ScoreRing score={Math.round(d.confidenceScore || 0)} label="Confidence" color="#f59e0b" size={80} />
        </div>
        <div className="score-card">
          <ScoreRing score={Math.round(d.problemSolvingScore || 0)} label="Problem Solving" color="#ec4899" size={80} />
        </div>
      </div>

      {/* Charts Row */}
      <div className="charts-row">
        {/* Radar */}
        <div className="chart-card">
          <h3>Skill Radar</h3>
          <ResponsiveContainer width="100%" height={220}>
            <RadarChart data={radarData}>
              <PolarGrid stroke="#2a2a3e" />
              <PolarAngleAxis dataKey="subject" tick={{ fill: '#8892a4', fontSize: 12 }} />
              <Radar dataKey="value" stroke="#00e5a0" fill="#00e5a0" fillOpacity={0.2} />
            </RadarChart>
          </ResponsiveContainer>
        </div>

        {/* Bar */}
        <div className="chart-card">
          <h3>Score Per Question</h3>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={barData} barCategoryGap="30%">
              <XAxis dataKey="name" tick={{ fill: '#8892a4', fontSize: 12 }} axisLine={false} tickLine={false} />
              <YAxis domain={[0, 100]} tick={{ fill: '#8892a4', fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ background: '#12122a', border: '1px solid #2a2a3e', borderRadius: 8, color: '#fff' }}
                cursor={{ fill: 'rgba(255,255,255,0.04)' }}
              />
              <Bar dataKey="score" radius={[6, 6, 0, 0]}>
                {barData.map((entry, i) => (
                  <Cell key={i} fill={entry.score >= 75 ? '#00e5a0' : entry.score >= 50 ? '#ffd166' : '#ff6b6b'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* AI Summary */}
      <div className="ai-summary-card">
        <div className="ai-summary-icon">🤖</div>
        <div>
          <div className="ai-summary-label">AI OVERALL ASSESSMENT</div>
          <p>{d.overallFeedback}</p>
        </div>
      </div>

      {/* Weak Areas & Strengths */}
      <div className="analysis-row">
        <div className="analysis-card weak">
          <h3>⚠️ Areas to Improve</h3>
          <ul>
            {(d.weakAreas || []).map((w, i) => (
              <li key={i}><span className="dot red" />{w}</li>
            ))}
          </ul>
        </div>
        <div className="analysis-card strong">
          <h3>⭐ Your Strengths</h3>
          <ul>
            {(d.strengths || []).map((s, i) => (
              <li key={i}><span className="dot green" />{s}</li>
            ))}
          </ul>
        </div>
      </div>

      {/* Per-Question Breakdown */}
      <div className="q-breakdown">
        <h3>Question Breakdown</h3>
        {d.questionFeedbacks?.map((qf, i) => (
          <div key={i} className={`q-item ${activeQ === i ? 'open' : ''}`}>
            <div className="q-item-header" onClick={() => setActiveQ(activeQ === i ? null : i)}>
              <div className="q-item-left">
                <span className="q-num">Q{qf.questionNumber}</span>
                <span className="q-text-preview">{qf.question}</span>
              </div>
              <div className="q-item-right">
                <div className="q-score-bar">
                  <div style={{ width: `${qf.score}%`, background: qf.score >= 75 ? '#00e5a0' : qf.score >= 50 ? '#ffd166' : '#ff6b6b' }} />
                </div>
                <span style={{ color: qf.score >= 75 ? '#00e5a0' : qf.score >= 50 ? '#ffd166' : '#ff6b6b', fontWeight: 700 }}>
                  {qf.score}
                </span>
                <span className="expand-icon">{activeQ === i ? '▲' : '▼'}</span>
              </div>
            </div>
            {activeQ === i && (
              <div className="q-item-body">
                <div className="q-detail-section">
                  <div className="q-detail-label">Your Answer</div>
                  <p>{qf.userAnswer}</p>
                </div>
                <div className="q-detail-section">
                  <div className="q-detail-label">AI Feedback</div>
                  <p>{qf.feedback}</p>
                </div>
                <div className="q-detail-section ideal">
                  <div className="q-detail-label">Ideal Answer</div>
                  <p>{qf.modelAnswer}</p>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Actions */}
      <div className="dash-actions">
        <button className="action-btn primary" onClick={() => dispatch({ type: 'RESET' })}>
          🔁 Start New Interview
        </button>
        <button className="action-btn secondary" onClick={() => window.print()}>
          🖨️ Print Report
        </button>
      </div>
    </div>
  );
}
