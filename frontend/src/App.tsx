'use client';
import { Sidebar } from './components/Sidebar';
import { ChatPage } from './app/chat/page';
import { useState } from 'react';

function App() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  return (
    <div className="bg-tertiary flex h-[100dvh] w-full flex-row overflow-hidden">
      <div className="hidden lg:flex">
        {isSidebarOpen && <Sidebar />}
      </div>

      <div className="flex flex-1 overflow-hidden">
        <div className="flex w-full py-1 pr-1">
          <div className="relative flex flex-1 flex-row h-[calc(99dvh)] border border-border rounded-sm bg-secondary w-full overflow-hidden shadow-sm">
            <div className="relative flex h-full w-0 flex-1 flex-row">
              <div className="flex w-full flex-col gap-2 overflow-y-auto">
                <div className="from-secondary to-secondary/0 via-secondary/70 absolute left-0 right-0 top-0 z-40 flex flex-row items-center justify-center gap-1 bg-gradient-to-b p-2 pb-12"></div>
                <ChatPage />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
