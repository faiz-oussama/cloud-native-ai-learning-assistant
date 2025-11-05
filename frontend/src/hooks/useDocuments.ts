import { useState, useCallback } from 'react';
import { apiClient } from '@/services/api';
import type { Document } from '@/services/api';

export const useDocuments = (userId: string) => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadDocuments = useCallback(async () => {
    if (!userId) return;
    
    setIsLoading(true);
    setError(null);
    try {
      const data = await apiClient.getDocuments(userId);
      setDocuments(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load documents');
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  const uploadDocument = useCallback(async (file: File) => {
    if (!userId) return null;
    
    setIsUploading(true);
    setUploadProgress(0);
    setError(null);
    
    try {
      // Simulate progress for better UX
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => Math.min(prev + 10, 90));
      }, 200);
      
      const document = await apiClient.uploadDocument(file, userId);
      
      clearInterval(progressInterval);
      setUploadProgress(100);
      
      setDocuments(prev => [document, ...prev]);
      
      setTimeout(() => {
        setUploadProgress(0);
        setIsUploading(false);
      }, 500);
      
      return document;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
      setIsUploading(false);
      setUploadProgress(0);
      return null;
    }
  }, [userId]);

  const deleteDocument = useCallback(async (documentId: string) => {
    setIsLoading(true);
    setError(null);
    try {
      await apiClient.deleteDocument(documentId);
      setDocuments(prev => prev.filter(d => d.id !== documentId));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete document');
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    documents,
    isUploading,
    uploadProgress,
    isLoading,
    error,
    loadDocuments,
    uploadDocument,
    deleteDocument,
  };
};
