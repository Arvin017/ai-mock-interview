import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import toast from 'react-hot-toast';
import { interviewAPI } from '../services/api';
import { useInterview } from '../context/InterviewContext';
import './UploadPage.css';

const DOMAINS = [
  { id: 'swe',      label: 'Software Engineering', icon: '⚙️' },
  { id: 'data',     label: 'Data Science',          icon: '📊' },
  { id: 'pm',       label: 'Product Management',    icon: '🧩' },
  { id: 'devops',   label: 'DevOps / Cloud',         icon: '☁️' },
  { id: 'frontend', label: 'Frontend Dev',           icon: '🎨' },
  { id: 'backend',  label: 'Backend Dev',            icon: '🔧' },
];

export default function UploadPage() {
  const { state, dispatch } = useInterview();
  const [name, setName]       = useState('');
  const [domain, setDomain]   = useState('swe');
  const [file, setFile]       = useState(null);
  const [resumeText, setResumeText] = useState('');
  const [uploading, setUploading]   = useState(false);
  const [starting, setStarting]     = useState(false);

  const onDrop = useCallback(async (accepted) => {
    const f = accepted[0];
    if (!f) return;
    setFile(f);
    setUploading(true);
    try {
      const res = await interviewAPI.uploadResume(f);
      setResumeText(res.text);
      dispatch({ type: 'SET_RESUME', payload: res.text });
      toast.success(`Resume parsed! ${res.charCount} characters extracted.`);
    } catch (err) {
      toast.error('Failed to parse resume: ' + err.message);
    } finally {
      setUploading(false);
    }
  }, [dispatch]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/pdf': ['.pdf'], 'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'], 'text/plain': ['.txt'] },
    maxFiles: 1,
    maxSize: 10 * 1024 * 1024,
  });

  const handleStart = async () => {
    if (!name.trim()) { toast.error('Please enter your name'); return; }
    if (!resumeText && !file) { toast.error('Please upload your resume'); return; }
    setStarting(true);
    dispatch({ type: 'SET_LOADING', payload: true });
    try {
      const session = await interviewAPI.startSession({ candidateName: name.trim(), domain, resumeText });
      dispatch({ type: 'START_SESSION', payload: session });
      toast.success('Interview started! Good luck 🚀');
    } catch (err) {
      dispatch({ type: 'SET_ERROR', payload: err.message });
      toast.error('Failed to start: ' + err.message);
    } finally {
      setStarting(false);
    }
  };

  return (
    <div className="upload-page">
      <div className="upload-hero">
        <div className="hero-badge">AI POWERED</div>
        <h1 className="hero-title">Mock Interview<br /><span>Platform</span></h1>
        <p className="hero-sub">Upload your resume, get personalized questions,<br />receive real-time AI feedback</p>
      </div>

      <div className="upload-card">
        {/* Name */}
        <div className="form-group">
          <label>Your Name</label>
          <input
            className="form-input"
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="e.g. Rahul Sharma"
          />
        </div>

        {/* Domain */}
        <div className="form-group">
          <label>Interview Domain</label>
          <div className="domain-grid">
            {DOMAINS.map(d => (
              <button
                key={d.id}
                className={`domain-btn ${domain === d.id ? 'active' : ''}`}
                onClick={() => setDomain(d.id)}
              >
                <span className="domain-icon">{d.icon}</span>
                <span className="domain-label">{d.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Resume Upload */}
        <div className="form-group">
          <label>Resume Upload</label>
          <div {...getRootProps()} className={`dropzone ${isDragActive ? 'drag-active' : ''} ${file ? 'has-file' : ''}`}>
            <input {...getInputProps()} />
            {uploading ? (
              <div className="dz-content">
                <div className="spinner" />
                <p>Parsing resume with AI...</p>
              </div>
            ) : file ? (
              <div className="dz-content">
                <div className="dz-icon success">✓</div>
                <p className="dz-filename">{file.name}</p>
                <p className="dz-hint">Drop another file to replace</p>
              </div>
            ) : (
              <div className="dz-content">
                <div className="dz-icon">📄</div>
                <p>Drag & drop your resume here</p>
                <p className="dz-hint">PDF, DOCX, or TXT · max 10MB</p>
              </div>
            )}
          </div>
        </div>

        {/* Manual paste */}
        {!file && (
          <div className="form-group">
            <label>Or paste resume text</label>
            <textarea
              className="form-textarea"
              value={resumeText}
              onChange={e => { setResumeText(e.target.value); dispatch({ type: 'SET_RESUME', payload: e.target.value }); }}
              placeholder="Paste your resume content here..."
              rows={6}
            />
          </div>
        )}

        <button
          className="start-btn"
          onClick={handleStart}
          disabled={starting || uploading}
        >
          {starting ? (
            <><span className="spinner sm" /> Generating Questions...</>
          ) : (
            <><span>🚀</span> Start Interview</>
          )}
        </button>
      </div>
    </div>
  );
}
