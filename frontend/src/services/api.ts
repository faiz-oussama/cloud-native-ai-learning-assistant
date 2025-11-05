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
  id: string;
  userId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  processingStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  uploadedAt: string;
  processedAt?: string;
}

export interface ChatSession {
  id: string;
  userId: string;
  documentId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: Message[];
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
    return this.request<Document[]>(`${API_ENDPOINTS.documents.list}?userId=${userId}`);
  }

  async deleteDocument(documentId: string): Promise<void> {
    await this.request(API_ENDPOINTS.documents.delete(documentId), {
      method: 'DELETE',
    });
  }

  // Chat APIs
  async createChatSession(userId: string, documentId: string, title?: string): Promise<ChatSession> {
    return this.request<ChatSession>(API_ENDPOINTS.chat.createSession, {
      method: 'POST',
      body: JSON.stringify({ userId, documentId, title }),
    });
  }

  async getUserSessions(userId: string): Promise<ChatSession[]> {
    return this.request<ChatSession[]>(API_ENDPOINTS.chat.userSessions(userId));
  }

  async sendMessage(sessionId: string, message: string, userId: string): Promise<ChatMessageResponse> {
    return this.request<ChatMessageResponse>(API_ENDPOINTS.chat.sendMessage, {
      method: 'POST',
      body: JSON.stringify({ sessionId, message }),
      headers: {
        'X-User-Id': userId,
      },
    });
  }

  async getSessionMessages(sessionId: string): Promise<Message[]> {
    const session = await this.request<ChatSession>(
      API_ENDPOINTS.chat.sessionMessages(sessionId)
    );
    return session.messages || [];
  }

  async deleteSession(sessionId: string): Promise<void> {
    await this.request(`${API_ENDPOINTS.chat.sessions}/${sessionId}`, {
      method: 'DELETE',
    });
  }
}

export const apiClient = new APIClient();
