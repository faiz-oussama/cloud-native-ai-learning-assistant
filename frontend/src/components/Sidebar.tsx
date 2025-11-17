'use client';
import { Flex } from './ui/flex';
import { Button } from './ui/button';
import { Plus, Search, Settings, ArrowLeft, ArrowRight, FileText } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useState, useEffect } from 'react';
import { useAuthContext } from '@/contexts/AuthContext';
import { useChatContext } from '@/contexts/ChatContext';
import { useDocuments } from '@/hooks/useDocuments';
import { AuthModal } from './AuthModal';

export const Sidebar = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [showDocumentSelector, setShowDocumentSelector] = useState(false);
  const [selectedDocuments, setSelectedDocuments] = useState<string[]>([]);
  
  const { user, isAuthenticated, logout } = useAuthContext();
  const { sessions, loadSessions, createSession, loadSession, currentSession } = useChatContext();
  const { documents, loadDocuments } = useDocuments(user?.id || '');

  useEffect(() => {
    if (user?.id) {
      loadSessions();
      loadDocuments();
    }
  }, [user?.id, loadSessions, loadDocuments]);

  const handleNewThread = async () => {
    if (!isAuthenticated) {
      setShowAuthModal(true);
      return;
    }
    
    if (documents.length === 0) {
      alert('Please upload a document first');
      return;
    }
    
    // Show document selector
    setShowDocumentSelector(true);
    setSelectedDocuments([]);
  };

  const handleDocumentToggle = (documentId: string) => {
    setSelectedDocuments(prev => {
      if (prev.includes(documentId)) {
        return prev.filter(id => id !== documentId);
      } else {
        return [...prev, documentId];
      }
    });
  };

  const handleCreateSession = async () => {
    if (selectedDocuments.length === 0) {
      alert('Please select at least one document');
      return;
    }
    
    // Create session with selected documents
    const documentTitles = documents
      .filter(doc => selectedDocuments.includes(doc.documentId))
      .map(doc => doc.fileName);
    
    const title = documentTitles.length > 1 
      ? `Chat about ${documentTitles.length} documents`
      : `Chat about ${documentTitles[0]}`;
      
    await createSession(selectedDocuments, title);
    setShowDocumentSelector(false);
    setSelectedDocuments([]);
  };

  const handleSessionClick = (sessionId: string) => {
    loadSession(sessionId);
  };

  return (
    <>
      <div
        className={cn(
          'relative bottom-0 left-0 top-0 z-[50] flex h-[100dvh] flex-shrink-0 flex-col py-2 transition-all duration-200',
          isSidebarOpen ? 'top-0 h-full w-[230px]' : 'w-[50px]'
        )}
      >
        <Flex direction="col" className="w-full flex-1 items-start overflow-hidden">
          <div className="mb-3 flex w-full flex-row items-center justify-between">
            <div className={cn(
              'flex h-8 w-full cursor-pointer items-center justify-start gap-1.5 px-4',
              !isSidebarOpen && 'justify-center px-0'
            )}>
              <div className="size-5 rounded-full bg-brand flex items-center justify-center">
                <span className="text-background text-xs font-bold">L</span>
              </div>
              {isSidebarOpen && (
                <p className="text-foreground text-lg font-bold tracking-wide">
                  llmchat
                </p>
              )}
            </div>
            {isSidebarOpen && (
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={() => setIsSidebarOpen(prev => !prev)}
                className="mr-2"
              >
                <ArrowLeft className="h-4 w-4" />
              </Button>
            )}
          </div>

          <Flex
            direction="col"
            className={cn(
              'w-full items-end px-3',
              !isSidebarOpen && 'items-center justify-center px-0'
            )}
            gap="xs"
          >
            <Button
              size={isSidebarOpen ? 'sm' : 'icon-sm'}
              variant="bordered"
              rounded="lg"
              className={cn(isSidebarOpen && 'relative w-full', 'justify-center')}
              onClick={handleNewThread}
            >
              <Plus className="h-4 w-4" />
              {isSidebarOpen && 'New Thread'}
            </Button>

            <Button
              size={isSidebarOpen ? 'sm' : 'icon-sm'}
              variant="bordered"
              rounded="lg"
              className={cn(
                isSidebarOpen && 'relative w-full',
                'text-muted-foreground justify-center px-2'
              )}
            >
              <Search className="h-3.5 w-3.5" />
              {isSidebarOpen && 'Search'}
              {isSidebarOpen && <div className="flex-1" />}
              {isSidebarOpen && (
                <div className="flex flex-row items-center gap-1">
                  <span className="bg-muted-foreground/10 text-muted-foreground flex h-5 items-center justify-center rounded-md px-1.5 text-xs">
                    ⌘K
                  </span>
                </div>
              )}
            </Button>
          </Flex>

          <Flex
            direction="col"
            gap="xs"
            className={cn(
              'border-hard mt-3 w-full justify-center border-t border-dashed px-3 py-2 flex-1 overflow-y-auto',
              !isSidebarOpen && 'items-center justify-center px-0'
            )}
          >
            {/* Sessions List */}
            <div className={cn('w-full', isSidebarOpen ? 'flex' : 'hidden')}>
              <Flex direction="col" items="start" className="w-full gap-0.5">
                <div className="text-muted-foreground/70 flex flex-row items-center gap-1 px-2 py-1 text-xs font-medium opacity-70">
                  Recent Chats
                </div>
                {sessions.length === 0 ? (
                  <div className="border-hard flex w-full flex-col items-center justify-center gap-2 rounded-lg border border-dashed p-2">
                    <p className="text-muted-foreground text-xs opacity-50">
                      No chat sessions
                    </p>
                  </div>
                ) : (
                  <div className="w-full space-y-1">
                    {sessions.map((session) => (
                      <Button
                        key={session.id}
                        variant={currentSession?.id === session.id ? 'secondary' : 'ghost'}
                        size="sm"
                        className="w-full justify-start text-xs truncate"
                        onClick={() => handleSessionClick(session.id)}
                      >
                        <FileText className="h-3.5 w-3.5 mr-1.5" />
                        {session.title}
                      </Button>
                    ))}
                  </div>
                )}
              </Flex>
            </div>

            {/* Documents List */}
            {isSidebarOpen && documents.length > 0 && (
              <div className="w-full mt-4">
                <Flex direction="col" items="start" className="w-full gap-0.5">
                  <div className="text-muted-foreground/70 flex flex-row items-center gap-1 px-2 py-1 text-xs font-medium opacity-70">
                    Documents ({documents.length})
                  </div>
                  <div className="w-full space-y-1">
                    {documents.slice(0, 3).map((doc) => (
                      <div
                        key={doc.documentId}
                        className="text-muted-foreground text-xs px-2 py-1 truncate flex items-center gap-1"
                      >
                        <FileText className="h-3 w-3" />
                        <span className="flex-1 truncate">{doc.fileName}</span>
                        <span className={cn(
                          'text-[10px] px-1 rounded',
                          doc.processingStatus === 'COMPLETED' && 'bg-green-500/20 text-green-500',
                          doc.processingStatus === 'PROCESSING' && 'bg-yellow-500/20 text-yellow-500',
                          doc.processingStatus === 'FAILED' && 'bg-red-500/20 text-red-500'
                        )}>
                          {doc.processingStatus}
                        </span>
                      </div>
                    ))}
                  </div>
                </Flex>
              </div>
            )}
          </Flex>

          <Flex
            className={cn(
              'from-tertiary via-tertiary/95 absolute bottom-0 mt-auto w-full items-center bg-gradient-to-t via-60% to-transparent p-2 pt-12',
              isSidebarOpen && 'items-start justify-between'
            )}
            gap="xs"
            direction="col"
          >
            {!isSidebarOpen && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsSidebarOpen(prev => !prev)}
                className="mx-auto"
              >
                <ArrowRight className="h-4 w-4" />
              </Button>
            )}
            {isSidebarOpen && (
              <div className="flex w-full flex-col gap-1.5 p-1">
                <Button
                  variant="bordered"
                  size="sm"
                  rounded="lg"
                >
                  <Settings className="h-3.5 w-3.5" />
                  Settings
                </Button>
                {isAuthenticated ? (
                  <Button size="sm" rounded="lg" onClick={logout}>
                    Logout ({user?.username})
                  </Button>
                ) : (
                  <Button size="sm" rounded="lg" onClick={() => setShowAuthModal(true)}>
                    Log in / Sign up
                  </Button>
                )}
              </div>
            )}
          </Flex>
        </Flex>
        
        <AuthModal isOpen={showAuthModal} onClose={() => setShowAuthModal(false)} />
      </div>

      {/* Document Selector Modal */}
      {showDocumentSelector && (
        <div className="fixed inset-0 bg-black/50 z-[100] flex items-center justify-center p-4">
          <div className="bg-background rounded-lg w-full max-w-md">
            <div className="p-4 border-b">
              <h3 className="text-lg font-semibold">Select Documents</h3>
              <p className="text-muted-foreground text-sm">Choose documents to start chatting with</p>
            </div>
            <div className="max-h-60 overflow-y-auto p-4">
              {documents.map((doc) => (
                <div
                  key={doc.documentId}
                  className={cn(
                    "flex items-center p-2 rounded cursor-pointer mb-2",
                    selectedDocuments.includes(doc.documentId) 
                      ? "bg-primary/10 border border-primary/20" 
                      : "hover:bg-muted"
                  )}
                  onClick={() => handleDocumentToggle(doc.documentId)}
                >
                  <div className="flex items-center flex-1">
                    <FileText className="h-4 w-4 mr-2" />
                    <div className="flex-1">
                      <div className="font-medium text-sm">{doc.fileName}</div>
                      <div className="text-xs text-muted-foreground capitalize">{doc.processingStatus}</div>
                    </div>
                  </div>
                  {selectedDocuments.includes(doc.documentId) && (
                    <div className="w-5 h-5 rounded-full bg-primary flex items-center justify-center">
                      <div className="w-2 h-2 rounded-full bg-background"></div>
                    </div>
                  )}
                  {doc.processingStatus !== 'COMPLETED' && (
                    <span className="text-xs text-muted-foreground ml-2">Not ready</span>
                  )}
                </div>
              ))}
            </div>
            <div className="p-4 border-t flex justify-end gap-2">
              <Button variant="ghost" onClick={() => setShowDocumentSelector(false)}>
                Cancel
              </Button>
              <Button 
                onClick={handleCreateSession}
                disabled={selectedDocuments.length === 0}
              >
                Create Session
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};