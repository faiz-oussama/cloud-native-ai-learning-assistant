import { useState, useCallback } from 'react';
import { apiClient } from '@/services/api';
import type { Quiz, QuizResult, QuizSubmission } from '@/services/api';

export const useQuiz = (userId: string) => {
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [result, setResult] = useState<QuizResult | null>(null);
  const [submissions, setSubmissions] = useState<QuizSubmission[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const createQuiz = useCallback(async (title: string, documentText: string) => {
    if (!userId) return null;
    
    setIsLoading(true);
    setError(null);
    try {
      const newQuiz = await apiClient.createQuiz(title, documentText, Number(userId));
      setQuiz(newQuiz);
      return newQuiz;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create quiz');
      return null;
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  const loadQuiz = useCallback(async (quizId: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const loadedQuiz = await apiClient.getQuiz(quizId);
      setQuiz(loadedQuiz);
      return loadedQuiz;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load quiz');
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const submitQuiz = useCallback(async (quizId: number, answers: Record<number, string>) => {
    if (!userId) return null;
    
    setIsSubmitting(true);
    setError(null);
    try {
      const quizResult = await apiClient.submitQuiz(quizId, Number(userId), answers);
      setResult(quizResult);
      return quizResult;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit quiz');
      return null;
    } finally {
      setIsSubmitting(false);
    }
  }, [userId]);

  const loadSubmissions = useCallback(async () => {
    if (!userId) return;
    
    setIsLoading(true);
    setError(null);
    try {
      const userSubmissions = await apiClient.getUserSubmissions(Number(userId));
      setSubmissions(userSubmissions);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load submissions');
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  const resetQuiz = useCallback(() => {
    setQuiz(null);
    setResult(null);
    setError(null);
  }, []);

  return {
    quiz,
    result,
    submissions,
    isLoading,
    isSubmitting,
    error,
    createQuiz,
    loadQuiz,
    submitQuiz,
    loadSubmissions,
    resetQuiz,
  };
};

