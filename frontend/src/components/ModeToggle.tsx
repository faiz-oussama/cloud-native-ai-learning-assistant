'use client';
import { cn } from '@/lib/utils';
import { MessageSquare, Trophy } from 'lucide-react';

type Mode = 'chat' | 'quiz';

interface ModeToggleProps {
  currentMode: Mode;
  onModeChange: (mode: Mode) => void;
}

export const ModeToggle = ({ currentMode, onModeChange }: ModeToggleProps) => {
  return (
    <div className="inline-flex items-center rounded-full bg-tertiary p-1 gap-1">
      <button
        onClick={() => onModeChange('chat')}
        className={cn(
          'flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all duration-200',
          currentMode === 'chat'
            ? 'bg-background text-foreground shadow-sm'
            : 'text-muted-foreground hover:text-foreground'
        )}
      >
        <MessageSquare className="h-4 w-4" />
        <span>Chat</span>
      </button>
      <button
        onClick={() => onModeChange('quiz')}
        className={cn(
          'flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all duration-200',
          currentMode === 'quiz'
            ? 'bg-background text-foreground shadow-sm'
            : 'text-muted-foreground hover:text-foreground'
        )}
      >
        <Trophy className="h-4 w-4" />
        <span>Quiz</span>
      </button>
    </div>
  );
};
