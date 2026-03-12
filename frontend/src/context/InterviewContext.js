import { createContext, useContext, useReducer } from 'react';

const InterviewContext = createContext(null);

const initialState = {
  stage: 'upload', // upload | interview | feedback | dashboard
  session: null,   // { sessionId, candidateName, domain, questions }
  currentQ: 0,
  answers: [],     // [{ questionNumber, answer, timeTaken }]
  feedbacks: [],   // FeedbackResponse[]
  dashboard: null, // DashboardResponse
  resumeText: '',
  loading: false,
  error: null,
};

function reducer(state, action) {
  switch (action.type) {
    case 'SET_LOADING':    return { ...state, loading: action.payload, error: null };
    case 'SET_ERROR':      return { ...state, error: action.payload, loading: false };
    case 'SET_RESUME':     return { ...state, resumeText: action.payload };
    case 'START_SESSION':  return { ...state, session: action.payload, stage: 'interview', currentQ: 0, answers: [], feedbacks: [], loading: false };
    case 'SAVE_ANSWER':    return { ...state, answers: [...state.answers, action.payload] };
    case 'ADD_FEEDBACK':   return { ...state, feedbacks: [...state.feedbacks, action.payload], stage: 'feedback' };
    case 'NEXT_QUESTION':  return { ...state, currentQ: state.currentQ + 1, stage: 'interview' };
    case 'SET_DASHBOARD':  return { ...state, dashboard: action.payload, stage: 'dashboard', loading: false };
    case 'RESET':          return { ...initialState };
    default:               return state;
  }
}

export function InterviewProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);
  return (
    <InterviewContext.Provider value={{ state, dispatch }}>
      {children}
    </InterviewContext.Provider>
  );
}

export function useInterview() {
  const ctx = useContext(InterviewContext);
  if (!ctx) throw new Error('useInterview must be inside InterviewProvider');
  return ctx;
}
