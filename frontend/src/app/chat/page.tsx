'use client';
import { Flex } from '@/components/ui/flex';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { ArrowUp, Lightbulb, BookOpen, BarChart3, Pencil, HelpCircle, Globe, Paperclip } from 'lucide-react';
import { useState, useEffect, useRef } from 'react';

export const ChatPage = () => {
  const [greeting, setGreeting] = useState<string>('');
  const [message, setMessage] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const getTimeBasedGreeting = () => {
      const hour = new Date().getHours();
      if (hour >= 5 && hour < 12) {
        return 'Good morning';
      } else if (hour >= 12 && hour < 18) {
        return 'Good afternoon';
      } else {
        return 'Good evening';
      }
    };

    setGreeting(getTimeBasedGreeting());

    const interval = setInterval(() => {
      const newGreeting = getTimeBasedGreeting();
      if (newGreeting !== greeting) {
        setGreeting(newGreeting);
      }
    }, 60000);

    return () => clearInterval(interval);
  }, [greeting]);

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setMessage(e.target.value);
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = textareaRef.current.scrollHeight + 'px';
    }
  };

  return (
    <div className={cn(
      'bg-secondary w-full',
      'absolute inset-0 flex h-full w-full flex-col items-center justify-center'
    )}>
      <div className="mx-auto flex w-full max-w-3xl flex-col items-start justify-start px-8">
        <Flex
          items="start"
          justify="start"
          direction="col"
          className="w-full pb-4 h-full"
        >
          <div className="mb-4 flex w-full flex-col items-center gap-1">
            <Flex
              direction="col"
              className="relative h-[60px] w-full items-center justify-center overflow-hidden"
            >
              <h1 className="from-muted-foreground/50 via-muted-foreground/40 to-muted-foreground/20 bg-gradient-to-r bg-clip-text text-center text-[32px] font-semibold tracking-tight text-transparent">
                {greeting}
              </h1>
            </Flex>
          </div>

          <div className="w-full px-3">
            <Flex
              direction="col"
              className="bg-background border-hard/50 shadow-subtle-sm relative z-10 w-full rounded-xl border"
            >
              <div className="flex w-full flex-shrink-0 overflow-hidden rounded-lg">
                <div className="w-full">
                  <Flex className="flex w-full flex-row items-end gap-0">
                    <div className="flex-1 px-3 pt-3">
                      <textarea
                        ref={textareaRef}
                        value={message}
                        onChange={handleInput}
                        placeholder="Ask anything"
                        className="w-full resize-none border-none bg-transparent text-sm outline-none placeholder:text-muted-foreground focus-visible:outline-none min-h-[60px] max-h-[200px]"
                        rows={1}
                      />
                    </div>
                  </Flex>

                  <Flex
                    className="border-border w-full gap-0 border-t border-dashed px-2 py-2"
                    gap="none"
                    items="center"
                    justify="between"
                  >
                    <Flex gap="xs" items="center" className="shrink-0">
                      <Button variant="secondary" size="xs" rounded="lg">
                        <span className="text-xs">Gemini Flash 2.0</span>
                      </Button>
                      <Button variant="ghost" size="icon-xs">
                        <Globe className="h-3.5 w-3.5" />
                      </Button>
                      <Button variant="ghost" size="icon-xs">
                        <Paperclip className="h-3.5 w-3.5" />
                      </Button>
                    </Flex>

                    <Flex gap="md" items="center">
                      <Button
                        size="icon-xs"
                        rounded="lg"
                        className="bg-foreground text-background hover:opacity-90"
                      >
                        <ArrowUp className="h-3.5 w-3.5" />
                      </Button>
                    </Flex>
                  </Flex>
                </div>
              </div>
            </Flex>
          </div>

          {/* Example Prompts */}
          <div className="mt-6 flex w-full flex-wrap items-center justify-center gap-2 px-3">
            <Button variant="bordered" size="sm" rounded="full" className="text-xs">
              <HelpCircle className="h-3.5 w-3.5" />
              How to
            </Button>
            <Button variant="bordered" size="sm" rounded="full" className="text-xs">
              <Lightbulb className="h-3.5 w-3.5" />
              Explain Concepts
            </Button>
            <Button variant="bordered" size="sm" rounded="full" className="text-xs">
              <Pencil className="h-3.5 w-3.5" />
              Creative
            </Button>
            <Button variant="bordered" size="sm" rounded="full" className="text-xs">
              <BookOpen className="h-3.5 w-3.5" />
              Advice
            </Button>
            <Button variant="bordered" size="sm" rounded="full" className="text-xs">
              <BarChart3 className="h-3.5 w-3.5" />
              Analysis
            </Button>
          </div>

          {/* Footer */}
          <div className="absolute bottom-4 left-0 right-0 flex items-center justify-center gap-4 text-xs text-muted-foreground">
            <a href="#" className="hover:text-foreground transition-colors opacity-50 hover:opacity-100">Star us on GitHub</a>
            <a href="#" className="hover:text-foreground transition-colors opacity-50 hover:opacity-100">Changelog</a>
            <a href="#" className="hover:text-foreground transition-colors opacity-50 hover:opacity-100">Feedback</a>
            <a href="#" className="hover:text-foreground transition-colors opacity-50 hover:opacity-100">Terms</a>
            <a href="#" className="hover:text-foreground transition-colors opacity-50 hover:opacity-100">Privacy</a>
          </div>
        </Flex>
      </div>
    </div>
  );
};
