'use client';
import { Flex } from '@/components/ui/flex';
import { Button } from '@/components/ui/button';
import { ArrowUp, Lightbulb, BookOpen, BarChart3, Pencil, HelpCircle, Globe, Paperclip } from 'lucide-react';
import { useState, useEffect, useRef } from 'react';
import { useChatContext } from '@/contexts/ChatContext';
import { useDocuments } from '@/hooks/useDocuments';
import { useAuthContext } from '@/contexts/AuthContext';
import { AIMessage } from '@/components/thread/AIMessage';
import { UserMessage } from '@/components/thread/UserMessage';
import { ThinkingStatus } from '@/components/thread/ThinkingStatus';

export const ChatPage = () => {
  const [greeting, setGreeting] = useState<string>('');
  const [message, setMessage] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const { user } = useAuthContext();
  const { messages, currentSession, isSending, sendMessage, createSession } = useChatContext();
  const { uploadDocument, isUploading, uploadProgress, loadDocuments } = useDocuments(user?.id || '') as any;

  useEffect(() => {
    if (user?.id) {
      loadDocuments();
    }
  }, [user?.id, loadDocuments]);

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

  const handleSend = async () => {
    if (!message.trim() || isSending || !currentSession) return;

    const msg = message;
    setMessage('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }

    await sendMessage(msg);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const uploadedDoc = await uploadDocument(file);
      if (uploadedDoc && user?.id) {
        console.log('Document uploaded successfully:', uploadedDoc.documentId);

        // Automatically create a chat session with the uploaded document
        if (!currentSession) {
          await createSession(
            [uploadedDoc.documentId],
            `Chat about ${uploadedDoc.fileName}`
          );
        }
      }
    } catch (err) {
      console.error('Upload failed:', err);
    } finally {
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const hasMessages = messages.length > 0;

  return (
    <div className="bg-secondary w-full h-full flex flex-col pt-16">
      <div className="mx-auto flex w-full max-w-3xl flex-col flex-1 items-start justify-start px-8 py-8 gap-4">
        {/* Messages View */}
        {hasMessages && (
          <div className="flex-1 w-full overflow-y-auto py-4 space-y-4">
            {messages.map((msg, idx) => (
              msg.role === 'user' ? (
                <UserMessage key={idx} message={msg.content} />
              ) : (
                <AIMessage key={idx} content={msg.content} isCompleted />
              )
            ))}
            {isSending && (
              <div className="py-4">
                <ThinkingStatus text="Thinking" />
                <AIMessage content="" isGenerating />
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        )}

        {/* Empty State */}
        {!hasMessages && (
          <Flex
            items="center"
            justify="center"
            direction="col"
            className="w-full flex-1"
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
          </Flex>
        )}

        {/* Input Area - Always at bottom */}
        <div className="w-full px-3">
          {isUploading && (
            <div className="mb-2 text-xs text-muted-foreground">
              Uploading... {uploadProgress}%
            </div>
          )}
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
                      onKeyDown={handleKeyDown}
                      placeholder={currentSession ? "Ask anything about your document..." : "Upload a document or start a new chat"}
                      disabled={!currentSession || isSending}
                      className="w-full resize-none border-none bg-transparent text-sm outline-none placeholder:text-muted-foreground focus-visible:outline-none min-h-[60px] max-h-[200px] disabled:opacity-50"
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
                      <span className="text-xs">GPT-4</span>
                    </Button>
                    <Button variant="ghost" size="icon-xs">
                      <Globe className="h-3.5 w-3.5" />
                    </Button>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept=".pdf,.doc,.docx,.txt"
                      onChange={handleFileUpload}
                      className="hidden"
                    />
                    <Button
                      variant="ghost"
                      size="icon-xs"
                      onClick={() => fileInputRef.current?.click()}
                      disabled={isUploading}
                      title={isUploading ? 'Uploading document...' : 'Upload document'}
                    >
                      <Paperclip className="h-3.5 w-3.5" />
                    </Button>
                  </Flex>

                  <Flex gap="md" items="center">
                    <Button
                      size="icon-xs"
                      rounded="lg"
                      className="bg-foreground text-background hover:opacity-90 disabled:opacity-50"
                      onClick={handleSend}
                      disabled={!message.trim() || isSending || !currentSession}
                    >
                      <ArrowUp className="h-3.5 w-3.5" />
                    </Button>
                  </Flex>
                </Flex>
              </div>
            </div>
          </Flex>
        </div>
      </div>
    </div>
  );
};
