import API_ENDPOINTS from '@/config/api';

// Types
export interface User {
  id: string;
  username: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Document {
  documentId: string;
  userId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  processingStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  uploadedAt: string;
  processedAt?: string;
  extractedText?: string; // Full text content of the document
}

export interface ChatSession {
  id: string;
  userId: string;
  documentIds: string[];
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: Message[];
  // Handle potential snake_case from backend
  document_ids?: string[];
}

export interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp?: string;
}

export interface ChatMessageRequest {
  sessionId: string;
  message: string;
}

export interface ChatMessageResponse {
  sessionId: string;
  userMessage: Message;
  assistantMessage: Message;
}

// Quiz Types
export interface Question {
  id: number;
  questionText: string;
  options: string[];
  correctAnswer: string;
}

export interface Quiz {
  id: number;
  title: string;
  questions: Question[];
  documentContext?: string;
}

export interface QuizSubmission {
  id: number;
  quiz: Quiz;
  userId: number;
  answers: Record<number, string>;
  score: number;
}

export interface Feedback {
  questionId: number;
  yourAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
  explanation: string;
}

export interface QuizResult {
  quizId: number;
  submissionId: number;
  correctAnswers: number;
  totalQuestions: number;
  score: number;
  feedback: Feedback[];
}

// API Client
class APIClient {
  private token: string | null = null;

  setToken(token: string) {
    this.token = token;
    localStorage.setItem('auth_token', token);
  }

  getToken(): string | null {
    if (!this.token) {
      this.token = localStorage.getItem('auth_token');
    }
    return this.token;
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('auth_token');
  }

  private async request<T>(url: string, options: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP ${response.status}`);
    }

    return response.json();
  }

  // Auth APIs
  async login(username: string, password: string): Promise<AuthResponse> {
    return this.request<AuthResponse>(API_ENDPOINTS.users.login, {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
  }

  async register(username: string, email: string, password: string): Promise<AuthResponse> {
    return this.request<AuthResponse>(API_ENDPOINTS.users.register, {
      method: 'POST',
      body: JSON.stringify({ username, email, password }),
    });
  }

  // Document APIs
  async uploadDocument(file: File, userId: string): Promise<Document> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', userId);

    const response = await fetch(API_ENDPOINTS.documents.upload, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
      },
      body: formData,
    });

    if (!response.ok) {
      throw new Error('Upload failed');
    }

    return response.json();
  }

  async getDocuments(userId: string): Promise<Document[]> {
    return this.request<Document[]>(API_ENDPOINTS.documents.userDocuments(userId));
  }

  async getDocument(documentId: string): Promise<Document> {
    return this.request<Document>(`${API_ENDPOINTS.documents.base}/api/documents/${documentId}`);
  }

  async deleteDocument(documentId: string): Promise<void> {
    await this.request(API_ENDPOINTS.documents.delete(documentId), {
      method: 'DELETE',
    });
  }

  async checkDocumentStatus(documentId: string): Promise<Document> {
    return this.request<Document>(`${API_ENDPOINTS.documents.base}/api/documents/${documentId}/status`);
  }

  // Chat APIs
  async createChatSession(userId: string, documentIds: string[], title?: string): Promise<ChatSession> {
    return this.request<ChatSession>(API_ENDPOINTS.chat.createSession, {
      method: 'POST',
      body: JSON.stringify({ userId, documentIds, title }),
    });
  }

  async getUserSessions(userId: string): Promise<ChatSession[]> {
    return this.request<ChatSession[]>(API_ENDPOINTS.chat.userSessions(userId));
  }

  async sendMessage(sessionId: string, message: string, userId: string): Promise<ChatMessageResponse> {
    const url = `${API_ENDPOINTS.chat.sendMessage}?userId=${encodeURIComponent(userId)}`;
    return this.request<ChatMessageResponse>(url, {
      method: 'POST',
      body: JSON.stringify({ sessionId, message }),
    });
  }

  async getSessionMessages(sessionId: string, userId: string): Promise<Message[]> {
    const url = `${API_ENDPOINTS.chat.sessions}/${sessionId}?userId=${encodeURIComponent(userId)}`;
    const session = await this.request<ChatSession>(url);
    return session.messages || [];
  }

  async deleteSession(sessionId: string, userId: string): Promise<void> {
    const url = `${API_ENDPOINTS.chat.sessions}/${sessionId}?userId=${encodeURIComponent(userId)}`;
    await this.request(url, {
      method: 'DELETE',
    });
  }

  // Quiz APIs
  async createQuiz(
    title: string,
    userId: number,
    options: { documentId?: string; documentText?: string }
  ): Promise<Quiz> {
    if (!options.documentId && !options.documentText) {
      throw new Error('Either documentId or documentText must be provided');
    }

    return this.request<Quiz>(API_ENDPOINTS.quiz.create, {
      method: 'POST',
      body: JSON.stringify({
        title,
        userId,
        documentId: options.documentId,
        documentText: options.documentText
      }),
    });
  }

  async getQuiz(quizId: number): Promise<Quiz> {
    return this.request<Quiz>(API_ENDPOINTS.quiz.getQuiz(quizId));
  }

  async submitQuiz(quizId: number, userId: number, answers: Record<number, string>): Promise<QuizResult> {
    return this.request<QuizResult>(API_ENDPOINTS.quiz.submit(quizId), {
      method: 'POST',
      body: JSON.stringify({ userId, answers }),
    });
  }

  async getUserSubmissions(userId: number): Promise<QuizSubmission[]> {
    return this.request<QuizSubmission[]>(API_ENDPOINTS.quiz.userSubmissions(userId));
  }

  async getTestQuiz(): Promise<Quiz> {
    return this.request<Quiz>(API_ENDPOINTS.quiz.test);
  }
}

export const apiClient = new APIClient();
