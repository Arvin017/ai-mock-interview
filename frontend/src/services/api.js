import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'https://ai-mock-interview-g9dz.onrender.com/api/interview';

const api = axios.create({
  baseURL: API_BASE,
  timeout: 120000,
});

api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err.response?.data?.message || err.message || 'Something went wrong';
    return Promise.reject(new Error(msg));
  }
);

export const interviewAPI = {
  // Upload resume file
  uploadResume: (file) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/upload-resume', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  // Start interview session
  startSession: ({ candidateName, domain, resumeText }) =>
    api.post('/start', { candidateName, domain, resumeText }),

  // Submit an answer
  submitAnswer: ({ sessionId, questionNumber, answer, timeTakenSeconds }) =>
    api.post('/submit-answer', { sessionId, questionNumber, answer, timeTakenSeconds }),

  // Complete session and get dashboard
  completeSession: (sessionId) =>
    api.post(`/complete/${sessionId}`),

  // Get dashboard data
  getDashboard: (sessionId) =>
    api.get(`/dashboard/${sessionId}`),

  // Health check
  health: () => api.get('/health'),
};
