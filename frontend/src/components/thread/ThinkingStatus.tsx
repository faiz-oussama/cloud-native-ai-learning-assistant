'use client';
import { motion } from 'framer-motion';

const loadingCircle = {
  display: 'block',
  width: '0.2rem',
  height: '0.2rem',
  overflow: 'hidden',
  marginLeft: '0.1rem',
  marginRight: '0.1rem',
  backgroundColor: 'currentColor',
  borderRadius: '30%',
};

export const ThinkingStatus = ({ text = 'Thinking' }: { text?: string }) => {
  return (
    <div className="flex items-center gap-2 text-muted-foreground text-sm">
      <span>{text}</span>
      <div className="flex items-center" style={{ width: '1.2rem', height: '1.2rem' }}>
        <motion.span
          style={loadingCircle}
          className="shrink-0"
          animate={{ y: [0, -4, 0] }}
          transition={{
            duration: 0.2,
            repeat: Infinity,
            repeatDelay: 0.8,
            ease: 'easeInOut',
          }}
        />
        <motion.span
          style={loadingCircle}
          className="shrink-0"
          animate={{ y: [0, -4, 0] }}
          transition={{
            duration: 0.2,
            repeat: Infinity,
            repeatDelay: 0.8,
            ease: 'easeInOut',
            delay: 0.2,
          }}
        />
        <motion.span
          style={loadingCircle}
          className="shrink-0"
          animate={{ y: [0, -4, 0] }}
          transition={{
            duration: 0.2,
            repeat: Infinity,
            repeatDelay: 0.8,
            ease: 'easeInOut',
            delay: 0.4,
          }}
        />
      </div>
    </div>
  );
};

export const StepStatus = ({ status }: { status: 'PENDING' | 'COMPLETED' | 'ERROR' | 'QUEUED' }) => {
  switch (status) {
    case 'PENDING':
      return (
        <span className="relative flex size-3 items-center justify-center">
          <span className="bg-brand/50 absolute inline-flex h-full w-full animate-ping rounded-full opacity-75"></span>
          <span className="bg-brand relative inline-flex size-1 rounded-full"></span>
        </span>
      );
    case 'COMPLETED':
      return (
        <span className="relative flex size-3 items-center justify-center">
          <span className="relative flex size-1">
            <span className="bg-brand relative inline-flex size-1 rounded-full"></span>
          </span>
        </span>
      );
    case 'ERROR':
      return (
        <span className="relative flex size-3 items-center justify-center">
          <span className="relative flex size-1">
            <span className="relative inline-flex size-1 rounded-full bg-rose-400"></span>
          </span>
        </span>
      );
    default:
      return (
        <span className="relative flex size-3 items-center justify-center">
          <span className="relative flex size-1">
            <span className="bg-tertiary relative inline-flex size-1 rounded-full"></span>
          </span>
        </span>
      );
  }
};
