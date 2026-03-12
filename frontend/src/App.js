import { Toaster } from 'react-hot-toast';
import { InterviewProvider, useInterview } from './context/InterviewContext';
import UploadPage    from './pages/UploadPage';
import InterviewPage from './pages/InterviewPage';
import DashboardPage from './pages/DashboardPage';
import './App.css';

function AppInner() {
  const { state } = useInterview();
  const { stage, loading } = state;

  if (loading) {
    return (
      <div className="global-loading">
        <div className="loading-content">
          <div className="spinner xl" />
          <p>AI is generating your personalized questions...</p>
          <p className="loading-sub">Analyzing your resume and crafting questions 🤖</p>
        </div>
      </div>
    );
  }

  if (stage === 'dashboard') return <DashboardPage />;
  if (stage === 'interview' || stage === 'feedback') return <InterviewPage />;
  return <UploadPage />;
}

export default function App() {
  return (
    <InterviewProvider>
      <Toaster position="top-right" toastOptions={{
        style: { background: '#1a1a2e', color: '#fff', border: '1px solid #2a2a3e' },
        success: { iconTheme: { primary: '#00e5a0', secondary: '#1a1a2e' } },
        error:   { iconTheme: { primary: '#ff6b6b', secondary: '#1a1a2e' } },
      }} />
      <AppInner />
    </InterviewProvider>
  );
}
