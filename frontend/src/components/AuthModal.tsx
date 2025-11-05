'use client';
import { useState } from 'react';
import { Button } from './ui/button';
import { useAuthContext } from '@/contexts/AuthContext';
import { X } from 'lucide-react';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AuthModal = ({ isOpen, onClose }: AuthModalProps) => {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login, register, error, isLoading } = useAuthContext();

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const success = isLogin
      ? await login(username, password)
      : await register(username, email, password);
    
    if (success) {
      onClose();
      setUsername('');
      setEmail('');
      setPassword('');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-background border-border relative w-full max-w-md rounded-lg border p-6 shadow-lg">
        <button
          onClick={onClose}
          className="text-muted-foreground hover:text-foreground absolute right-4 top-4"
        >
          <X className="h-5 w-5" />
        </button>
        
        <h2 className="mb-4 text-xl font-semibold">
          {isLogin ? 'Login' : 'Sign Up'}
        </h2>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="border-border bg-secondary w-full rounded-md border px-3 py-2 text-sm outline-none focus:ring-1 focus:ring-brand"
              required
            />
          </div>
          
          {!isLogin && (
            <div>
              <label className="mb-1 block text-sm">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="border-border bg-secondary w-full rounded-md border px-3 py-2 text-sm outline-none focus:ring-1 focus:ring-brand"
                required
              />
            </div>
          )}
          
          <div>
            <label className="mb-1 block text-sm">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="border-border bg-secondary w-full rounded-md border px-3 py-2 text-sm outline-none focus:ring-1 focus:ring-brand"
              required
            />
          </div>
          
          {error && (
            <div className="text-sm text-red-500">{error}</div>
          )}
          
          <Button
            type="submit"
            className="w-full"
            disabled={isLoading}
          >
            {isLoading ? 'Please wait...' : isLogin ? 'Login' : 'Sign Up'}
          </Button>
          
          <button
            type="button"
            onClick={() => setIsLogin(!isLogin)}
            className="text-brand hover:text-brand/80 w-full text-center text-sm"
          >
            {isLogin ? "Don't have an account? Sign up" : 'Already have an account? Login'}
          </button>
        </form>
      </div>
    </div>
  );
};
