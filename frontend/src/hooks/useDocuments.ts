import { useState, useCallback } from 'react';
import { apiClient } from '@/services/api';
import type { Document } from '@/services/api';

export interface UseDocumentsReturn {
  documents: Document[];
  isUploading: boolean;
  uploadProgress: number;
  isLoading: boolean;
  error: string | null;
  loadDocuments: () => Promise<void>;
  uploadDocument: (file: File) => Promise<Document | null>;
  deleteDocument: (documentId: string) => Promise<void>;
}

export const useDocuments = (userId: string): UseDocumentsReturn => {
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

      // Poll for document processing status
      const pollStatus = async (docId: string, attempts = 0): Promise<Document | null> => {
        if (attempts > 20) { // Max 20 attempts (40 seconds)
          console.warn('Document processing timeout');
          return document;
        }

        try {
          const status = await apiClient.checkDocumentStatus(docId);

          if (status.processingStatus === 'COMPLETED') {
            // Update document in list
            setDocuments(prev =>
              prev.map(d => d.documentId === docId ? status : d)
            );
            return status;
          } else if (status.processingStatus === 'FAILED') {
            setError('Document processing failed');
            return null;
          }

          // Still processing, poll again
          await new Promise(resolve => setTimeout(resolve, 2000));
          return pollStatus(docId, attempts + 1);
        } catch (err) {
          console.error('Failed to check document status:', err);
          return document;
        }
      };

      // Poll for document processing status in background
      // This polling happens separately from session creation
      pollStatus(document.documentId).then(completedDoc => {
        if (completedDoc) {
          console.log('Document processing completed:', completedDoc);
        }
      });

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
      setDocuments(prev => prev.filter(d => d.documentId !== documentId));
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
