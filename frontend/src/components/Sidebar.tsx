'use client';
import { Flex } from './ui/flex';
import { Button } from './ui/button';
import { Plus, Search, Settings, ArrowLeft, ArrowRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useState } from 'react';

export const Sidebar = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  return (
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
            'border-hard mt-3 w-full justify-center border-t border-dashed px-3 py-2',
            !isSidebarOpen && 'items-center justify-center px-0'
          )}
        >
          {/* Pinned section */}
          <div className={cn('w-full', isSidebarOpen ? 'flex' : 'hidden')}>
            <Flex direction="col" items="start" className="w-full gap-0.5">
              <div className="text-muted-foreground/70 flex flex-row items-center gap-1 px-2 py-1 text-xs font-medium opacity-70">
                Pinned
              </div>
              <div className="border-hard flex w-full flex-col items-center justify-center gap-2 rounded-lg border border-dashed p-2">
                <p className="text-muted-foreground text-xs opacity-50">
                  No pinned threads
                </p>
              </div>
            </Flex>
          </div>
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
              <Button size="sm" rounded="lg">
                Log in / Sign up
              </Button>
            </div>
          )}
        </Flex>
      </Flex>
    </div>
  );
};
