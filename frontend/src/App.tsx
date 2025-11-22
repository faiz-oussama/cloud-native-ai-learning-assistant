'use client';
import { Sidebar } from '@/components/Sidebar';
import { ChatPage } from '@/app/chat/page';
import { QuizPage } from '@/app/quiz/page';
import { SignInPage } from '@/app/signin/page';
import { SignUpPage } from '@/app/signup/page';
import { ModeToggle } from '@/components/ModeToggle';
import { useState, useEffect } from 'react';
import { useAuthContext } from '@/contexts/AuthContext';
import { ChatProvider } from '@/contexts/ChatContext';

type Mode = 'chat' | 'quiz';

function App() {
  const [isSidebarOpen] = useState(true);
  const [showSignUp, setShowSignUp] = useState(false);
  const [mode, setMode] = useState<Mode>('chat');
  const { isAuthenticated, isLoading, user } = useAuthContext();

  // Enable dev mode with Ctrl+Shift+D
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key === 'D') {
        e.preventDefault();
        const isDev = localStorage.getItem('DEV_MODE') === 'true';
        localStorage.setItem('DEV_MODE', (!isDev).toString());
        window.location.reload();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

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
              <div className="relative flex h-full flex-1 flex-row">
                <div className="flex w-full flex-col gap-2">
                  {/* Mode Toggle */}
                  <div className="from-secondary to-secondary/0 via-secondary/70 absolute left-0 right-0 top-0 z-40 flex flex-row items-center justify-center gap-1 bg-gradient-to-b p-2 pb-12">
                    <ModeToggle currentMode={mode} onModeChange={setMode} />
                  </div>
                  
                  {/* Sliding Container */}
                  <div className="relative flex-1">
                    <div 
                      className="flex h-full transition-transform duration-500 ease-in-out will-change-transform"
                      style={{ 
                        transform: mode === 'chat' ? 'translate3d(0, 0, 0)' : 'translate3d(-100%, 0, 0)',
                        width: '200%',
                        backfaceVisibility: 'hidden',
                        WebkitBackfaceVisibility: 'hidden'
                      }}
                    >
                      {/* Chat Mode */}
                      <div className={`w-1/2 h-full ${mode !== 'chat' ? 'pointer-events-none' : ''}`}>
                        <ChatPage />
                      </div>
                      {/* Quiz Mode */}
                      <div className={`w-1/2 h-full ${mode !== 'quiz' ? 'pointer-events-none' : ''}`}>
                        <QuizPage />
                      </div>
                    </div>
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