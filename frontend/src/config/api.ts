// API Configuration
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082';
const USER_SERVICE_URL = import.meta.env.VITE_USER_SERVICE_URL || 'http://localhost:8080';
const DOCUMENT_SERVICE_URL = import.meta.env.VITE_DOCUMENT_SERVICE_URL || 'http://localhost:8081';
const CHAT_SERVICE_URL = import.meta.env.VITE_CHAT_SERVICE_URL || 'http://localhost:8082';
const QUIZ_SERVICE_URL = import.meta.env.VITE_QUIZ_SERVICE_URL || 'http://localhost:8083';

export const API_ENDPOINTS = {
  // User Service
  users: {
    base: USER_SERVICE_URL,
    login: `${USER_SERVICE_URL}/api/users/login`,
    register: `${USER_SERVICE_URL}/api/users/register`,
    profile: `${USER_SERVICE_URL}/api/users/profile`,
  },
  
  // Document Service
  documents: {
    base: DOCUMENT_SERVICE_URL,
    upload: `${DOCUMENT_SERVICE_URL}/api/documents/upload`,
    list: `${DOCUMENT_SERVICE_URL}/api/documents`,
    download: (id: string) => `${DOCUMENT_SERVICE_URL}/api/documents/${id}/download`,
    delete: (id: string) => `${DOCUMENT_SERVICE_URL}/api/documents/${id}`,
  },
  
  // Chat Service
  chat: {
    base: CHAT_SERVICE_URL,
    sessions: `${CHAT_SERVICE_URL}/api/chat/sessions`,
    createSession: `${CHAT_SERVICE_URL}/api/chat/sessions`,
    sendMessage: `${CHAT_SERVICE_URL}/api/chat/messages`,
    userSessions: (userId: string) => `${CHAT_SERVICE_URL}/api/chat/sessions/user/${userId}`,
    sessionMessages: (sessionId: string) => `${CHAT_SERVICE_URL}/api/chat/sessions/${sessionId}/messages`,
  },
  
  // Quiz Service
  quiz: {
    base: QUIZ_SERVICE_URL,
    generate: `${QUIZ_SERVICE_URL}/api/quiz/generate`,
    submit: `${QUIZ_SERVICE_URL}/api/quiz/submit`,
    results: (quizId: string) => `${QUIZ_SERVICE_URL}/api/quiz/${quizId}/results`,
  },
};

export const config = {
  apiBaseUrl: API_BASE_URL,
  userServiceUrl: USER_SERVICE_URL,
  documentServiceUrl: DOCUMENT_SERVICE_URL,
  chatServiceUrl: CHAT_SERVICE_URL,
  quizServiceUrl: QUIZ_SERVICE_URL,
};

export default API_ENDPOINTS;
