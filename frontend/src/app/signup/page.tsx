'use client';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Flex } from '@/components/ui/flex';
import { useAuthContext } from '@/contexts/AuthContext';
import { Loader2 } from 'lucide-react';

interface SignUpPageProps {
  onSignInClick?: () => void;
}

export const SignUpPage = ({ onSignInClick }: SignUpPageProps) => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const { register, isLoading, error } = useAuthContext();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);

    // Validate password match
    if (password !== confirmPassword) {
      setLocalError('Passwords do not match');
      return;
    }

    // Validate password strength
    if (password.length < 6) {
      setLocalError('Password must be at least 6 characters');
      return;
    }

    await register(username, email, password);
  };

  const displayError = localError || error;

  return (
    <div className="bg-tertiary flex h-[100dvh] w-full items-center justify-center">
      <div className="w-full max-w-md px-8">
        <Flex direction="col" className="w-full gap-6">
          {/* Header */}
          <div className="flex flex-col items-center gap-2">
            <Flex
              direction="col"
              className="relative h-[60px] w-full items-center justify-center overflow-hidden"
            >
              <h1 className="from-muted-foreground/50 via-muted-foreground/40 to-muted-foreground/20 bg-gradient-to-r bg-clip-text text-center text-[32px] font-semibold tracking-tight text-transparent">
                Create account
              </h1>
            </Flex>
            <p className="text-muted-foreground text-sm">
              Get started with your learning journey
            </p>
          </div>

          {/* Form */}
          <div className="bg-background border-hard/50 shadow-subtle-sm w-full rounded-xl border p-6">
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <label htmlFor="username" className="text-sm font-medium text-foreground">
                  Username
                </label>
                <Input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Choose a username"
                  required
                  autoComplete="username"
                  disabled={isLoading}
                />
              </div>

              <div className="flex flex-col gap-2">
                <label htmlFor="email" className="text-sm font-medium text-foreground">
                  Email
                </label>
                <Input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter your email"
                  required
                  autoComplete="email"
                  disabled={isLoading}
                />
              </div>

              <div className="flex flex-col gap-2">
                <label htmlFor="password" className="text-sm font-medium text-foreground">
                  Password
                </label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Create a password"
                  required
                  autoComplete="new-password"
                  disabled={isLoading}
                />
              </div>

              <div className="flex flex-col gap-2">
                <label htmlFor="confirmPassword" className="text-sm font-medium text-foreground">
                  Confirm Password
                </label>
                <Input
                  id="confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Confirm your password"
                  required
                  autoComplete="new-password"
                  disabled={isLoading}
                />
              </div>

              {displayError && (
                <div className="bg-destructive/10 text-destructive rounded-lg border border-destructive/20 px-3 py-2 text-sm">
                  {displayError}
                </div>
              )}

              <Button
                type="submit"
                size="lg"
                className="w-full"
                disabled={
                  isLoading ||
                  !username.trim() ||
                  !email.trim() ||
                  !password.trim() ||
                  !confirmPassword.trim()
                }
              >
                {isLoading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Creating account...
                  </>
                ) : (
                  'Sign up'
                )}
              </Button>
            </form>
          </div>

          {/* Footer */}
          <div className="flex flex-col items-center gap-4">
            <div className="text-muted-foreground flex items-center gap-2 text-sm">
              <span>Already have an account?</span>
              <button
                type="button"
                onClick={onSignInClick}
                className="text-foreground hover:text-foreground/80 font-medium transition-colors"
              >
                Sign in
              </button>
            </div>

            <div className="flex items-center justify-center gap-4 text-xs text-muted-foreground">
              <a href="#" className="hover:text-foreground transition-colors opacity-50 hover:opacity-100">
                Terms
              </a>
              <a href="#" className="hover:text-foreground transition-colors opacity-50 hover:opacity-100">
                Privacy
              </a>
              <a href="#" className="hover:text-foreground transition-colors opacity-50 hover:opacity-100">
                Help
              </a>
            </div>
          </div>
        </Flex>
      </div>
    </div>
  );
};
