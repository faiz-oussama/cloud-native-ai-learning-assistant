import { useState, useCallback } from 'react';
import { apiClient } from '@/services/api';
import type { ChatSession, Message, ChatMessageResponse } from '@/services/api';

export const useChat = (userId: string) => {
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [currentSession, setCurrentSession] = useState<ChatSession | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadSessions = useCallback(async () => {
    if (!userId) return;
    
    setIsLoading(true);
    setError(null);
    try {
      const data = await apiClient.getUserSessions(userId);
      setSessions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load sessions');
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  const createSession = useCallback(async (documentId: string, title?: string) => {
    if (!userId) return null;
    
    setIsLoading(true);
    setError(null);
    try {
      const session = await apiClient.createChatSession(userId, documentId, title);
      setSessions(prev => [session, ...prev]);
      setCurrentSession(session);
      setMessages(session.messages || []);
      return session;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create session');
      return null;
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  const loadSession = useCallback(async (sessionId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const sessionMessages = await apiClient.getSessionMessages(sessionId);
      const session = sessions.find(s => s.id === sessionId);
      if (session) {
        setCurrentSession(session);
        setMessages(sessionMessages);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load session');
    } finally {
      setIsLoading(false);
    }
  }, [sessions]);

  const sendMessage = useCallback(async (content: string) => {
    if (!currentSession || !userId) return;
    
    setIsSending(true);
    setError(null);
    
    // Optimistically add user message
    const userMessage: Message = { role: 'user', content };
    setMessages(prev => [...prev, userMessage]);
    
    try {
      const response: ChatMessageResponse = await apiClient.sendMessage(
        currentSession.id,
        content,
        userId
      );
      
      // Replace optimistic message with server response
      setMessages(prev => {
        const filtered = prev.filter(m => m !== userMessage);
        return [...filtered, response.userMessage, response.assistantMessage];
      });
      
      // Update session in list
      setSessions(prev => 
        prev.map(s => s.id === currentSession.id 
          ? { ...s, updatedAt: new Date().toISOString() }
          : s
        )
      );
    } catch (err) {
      // Remove optimistic message on error
      setMessages(prev => prev.filter(m => m !== userMessage));
      setError(err instanceof Error ? err.message : 'Failed to send message');
    } finally {
      setIsSending(false);
    }
  }, [currentSession, userId]);

  const deleteSession = useCallback(async (sessionId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      await apiClient.deleteSession(sessionId);
      setSessions(prev => prev.filter(s => s.id !== sessionId));
      if (currentSession?.id === sessionId) {
        setCurrentSession(null);
        setMessages([]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete session');
    } finally {
      setIsLoading(false);
    }
  }, [currentSession]);

  return {
    sessions,
    currentSession,
    messages,
    isLoading,
    isSending,
    error,
    loadSessions,
    createSession,
    loadSession,
    sendMessage,
    deleteSession,
  };
};
