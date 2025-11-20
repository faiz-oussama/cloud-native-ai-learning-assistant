'use client';
import { Sidebar } from '@/components/Sidebar';
import { ChatPage } from '@/app/chat/page';
import { QuizPage } from '@/app/quiz/page';
import { SignInPage } from '@/app/signin/page';
import { SignUpPage } from '@/app/signup/page';
import { useState } from 'react';
import { useAuthContext } from '@/contexts/AuthContext';
import { ChatProvider } from '@/contexts/ChatContext';
import { Button } from '@/components/ui/button';
import { MessageSquare, Brain } from 'lucide-react';

function App() {
  const [isSidebarOpen] = useState(true);
  const [showSignUp, setShowSignUp] = useState(false);
  const [mode, setMode] = useState<'chat' | 'quiz'>('chat');
  const { isAuthenticated, isLoading, user } = useAuthContext();

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div className="bg-tertiary flex h-[100dvh] w-full items-center justify-center">
        <div className="text-muted-foreground text-sm">Loading...</div>
      </div>
    );
  }

  // Show auth pages if not authenticated
  if (!isAuthenticated) {
    return showSignUp ? (
      <SignUpPage onSignInClick={() => setShowSignUp(false)} />
    ) : (
      <SignInPage onSignUpClick={() => setShowSignUp(true)} />
    );
  }

  // Show main app if authenticated
  return (
    <ChatProvider userId={user?.id || ''}>
      <div className="bg-tertiary flex h-[100dvh] w-full flex-row overflow-hidden">
        <div className="hidden lg:flex">
          {isSidebarOpen && <Sidebar />}
        </div>

        <div className="flex flex-1 overflow-hidden">
          <div className="flex w-full py-1 pr-1">
            <div className="relative flex flex-1 flex-row h-[calc(99dvh)] border border-border rounded-sm bg-secondary w-full overflow-hidden shadow-sm">
              <div className="relative flex h-full w-0 flex-1 flex-row">
                <div className="flex w-full flex-col gap-2 overflow-y-auto">
                  {/* Mode Toggle Header */}
                  <div className="from-secondary to-secondary/0 via-secondary/70 absolute left-0 right-0 top-0 z-40 flex flex-row items-center justify-center gap-1 bg-gradient-to-b p-2 pb-12">
                    <div className="bg-background/80 backdrop-blur-sm border border-border rounded-lg p-1 flex gap-1">
                      <Button
                        variant={mode === 'chat' ? 'default' : 'ghost'}
                        size="sm"
                        onClick={() => setMode('chat')}
                        className="gap-2"
                      >
                        <MessageSquare className="w-4 h-4" />
                        Chat
                      </Button>
                      <Button
                        variant={mode === 'quiz' ? 'default' : 'ghost'}
                        size="sm"
                        onClick={() => setMode('quiz')}
                        className="gap-2"
                      >
                        <Brain className="w-4 h-4" />
                        Quiz
                      </Button>
                    </div>
                  </div>

                  {/* Content */}
                  <div className="pt-16">
                    {mode === 'chat' ? <ChatPage /> : <QuizPage />}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </ChatProvider>
  );
}

export default App;