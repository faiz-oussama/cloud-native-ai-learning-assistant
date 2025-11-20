// API Configuration
// Use production URLs if in production, otherwise localhost
const isProduction = window.location.hostname !== 'localhost';

const PROD_BASE_DOMAIN = 'niceplant-c464d163.swedencentral.azurecontainerapps.io';

const API_BASE_URL = isProduction 
  ? `https://chat-service.${PROD_BASE_DOMAIN}` 
  : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082');
  
const USER_SERVICE_URL = isProduction 
  ? `https://user-service.${PROD_BASE_DOMAIN}` 
  : (import.meta.env.VITE_USER_SERVICE_URL || 'http://localhost:8080');
  
const DOCUMENT_SERVICE_URL = isProduction 
  ? `https://document-service.${PROD_BASE_DOMAIN}` 
  : (import.meta.env.VITE_DOCUMENT_SERVICE_URL || 'http://localhost:8081');
  
const CHAT_SERVICE_URL = isProduction 
  ? `https://chat-service.${PROD_BASE_DOMAIN}` 
  : (import.meta.env.VITE_CHAT_SERVICE_URL || 'http://localhost:8082');
  
const QUIZ_SERVICE_URL = isProduction 
  ? `https://quiz-service.${PROD_BASE_DOMAIN}` 
  : (import.meta.env.VITE_QUIZ_SERVICE_URL || 'http://localhost:8083');

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
    userDocuments: (userId: string) => `${DOCUMENT_SERVICE_URL}/api/documents/user/${userId}`,
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
  },
  
  // Quiz Service
  quiz: {
    base: QUIZ_SERVICE_URL,
    create: `${QUIZ_SERVICE_URL}/api/quizzes`,
    getQuiz: (quizId: number) => `${QUIZ_SERVICE_URL}/api/quizzes/${quizId}`,
    submit: (quizId: number) => `${QUIZ_SERVICE_URL}/api/quizzes/${quizId}/submit`,
    userSubmissions: (userId: number) => `${QUIZ_SERVICE_URL}/api/quizzes/submissions/user/${userId}`,
    test: `${QUIZ_SERVICE_URL}/api/quizzes/test`,
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