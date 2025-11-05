'use client';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Check, Copy, Pencil } from 'lucide-react';
import { useState, useCallback, useRef, useEffect } from 'react';

type UserMessageProps = {
  message: string;
  imageAttachment?: string;
};

export const UserMessage = ({ message, imageAttachment }: UserMessageProps) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [copied, setCopied] = useState(false);
  const [showExpandButton, setShowExpandButton] = useState(false);
  const messageRef = useRef<HTMLDivElement>(null);
  const maxHeight = 120;

  useEffect(() => {
    if (messageRef.current) {
      setShowExpandButton(messageRef.current.scrollHeight > maxHeight);
    }
  }, [message]);

  const handleCopy = useCallback(() => {
    if (messageRef.current) {
      navigator.clipboard.writeText(messageRef.current.innerText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  }, []);

  const toggleExpand = useCallback(() => setIsExpanded(prev => !prev), []);

  return (
    <div className="flex w-full flex-col items-end gap-2 pt-4">
      {imageAttachment && (
        <div className="max-w-[80%] overflow-hidden rounded-lg">
          <img src={imageAttachment} alt="Attachment" className="w-full" />
        </div>
      )}
      <div
        className={cn(
          'text-foreground bg-tertiary group relative max-w-[80%] overflow-hidden rounded-lg'
        )}
      >
        <div
          ref={messageRef}
          className="prose-sm relative px-3 py-1.5 font-normal"
          style={{
            maxHeight: isExpanded ? 'none' : maxHeight,
            transition: 'max-height 0.3s ease-in-out',
          }}
        >
          {message}
        </div>
        <div
          className={cn(
            'absolute bottom-0 left-0 right-0 hidden flex-col items-center group-hover:flex',
            showExpandButton && 'flex'
          )}
        >
          <div className="via-tertiary/85 to-tertiary flex w-full items-center justify-end gap-1 bg-gradient-to-b from-transparent p-1.5">
            {showExpandButton && (
              <Button
                variant="secondary"
                size="xs"
                rounded="full"
                className="pointer-events-auto relative z-10 px-4"
                onClick={toggleExpand}
              >
                {isExpanded ? 'Show less' : 'Show more'}
              </Button>
            )}
            <Button
              variant="bordered"
              size="icon-sm"
              onClick={handleCopy}
            >
              {copied ? (
                <Check className="h-3.5 w-3.5" />
              ) : (
                <Copy className="h-3.5 w-3.5" />
              )}
            </Button>
            <Button
              variant="bordered"
              size="icon-sm"
            >
              <Pencil className="h-3.5 w-3.5" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};
