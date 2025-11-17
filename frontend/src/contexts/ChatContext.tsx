'use client';

import { createContext, useContext } from 'react';
import type { ReactNode } from 'react';
import { useChat as useChatHook } from '@/hooks/useChat';
import type { ChatSession, Message } from '@/services/api';

interface ChatContextType {
  sessions: ChatSession[];
  currentSession: ChatSession | null;
  messages: Message[];
  isLoading: boolean;
  isSending: boolean;
  error: string | null;
  loadSessions: () => Promise<void>;
  createSession: (documentIds: string[], title?: string) => Promise<ChatSession | null>;
  loadSession: (sessionId: string) => Promise<void>;
  sendMessage: (content: string) => Promise<void>;
  deleteSession: (sessionId: string) => Promise<void>;
}

const ChatContext = createContext<ChatContextType | undefined>(undefined);

export const ChatProvider = ({ children, userId }: { children: ReactNode; userId: string }) => {
  const chatHook = useChatHook(userId);

  return (
    <ChatContext.Provider value={chatHook}>
      {children}
    </ChatContext.Provider>
  );
};

export const useChatContext = () => {
  const context = useContext(ChatContext);
  if (context === undefined) {
    throw new Error('useChatContext must be used within a ChatProvider');
  }
  return context;
};