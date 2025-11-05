'use client';
import { cn } from '@/lib/utils';
import { BookOpen } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';

type AIMessageProps = {
  content: string;
  isGenerating?: boolean;
  isCompleted?: boolean;
};

export const AIMessage = ({ content, isGenerating, isCompleted }: AIMessageProps) => {
  return (
    <div className="w-full">
      <div className={cn('flex w-full flex-col items-start gap-3 pt-4')}>
        <div className="text-muted-foreground flex flex-row items-center gap-1.5 text-xs font-medium">
          <BookOpen size={16} strokeWidth={2} />
          Answer
        </div>

        {!content && !isCompleted && (
          <div className="flex w-full flex-col items-start gap-2 opacity-10">
            <Skeleton className="bg-muted-foreground/40 mb-2 h-4 !w-[100px] rounded-sm" />
            <Skeleton className="w-full h-3 rounded-sm bg-gradient-to-r from-muted-foreground/40 to-transparent" />
            <Skeleton className="w-[70%] h-3 rounded-sm bg-gradient-to-r from-muted-foreground/40 to-transparent" />
            <Skeleton className="w-[50%] h-3 rounded-sm bg-gradient-to-r from-muted-foreground/40 to-transparent" />
          </div>
        )}

        {content && (
          <div className="w-full">
            <div className="prose prose-sm min-w-full">
              <div className="text-foreground font-normal leading-[1.65rem]">
                {content}
                {isGenerating && (
                  <span className="inline-flex ml-1">
                    <span className="animate-pulse">▊</span>
                  </span>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
