import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '@/services/api';
import type { User } from '@/services/api';

export const useAuth = () => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Development mode: auto-login if DEV_MODE is enabled
    const isDev = localStorage.getItem('DEV_MODE') === 'true' || import.meta.env.MODE === 'development';
    
    // Check for existing session
    const token = apiClient.getToken();
    const storedUser = localStorage.getItem('user');
    
    if (token && storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        apiClient.clearToken();
        localStorage.removeItem('user');
      }
    } else if (isDev && !token) {
      // Auto-login for development
      const devUser: User = {
        id: 'dev-user-123',
        username: 'developer',
        email: 'dev@example.com'
      };
      setUser(devUser);
      localStorage.setItem('user', JSON.stringify(devUser));
      apiClient.setToken('dev-token-12345');
    }
    setIsLoading(false);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.login(username, password);
      apiClient.setToken(response.token);
      setUser(response.user);
      localStorage.setItem('user', JSON.stringify(response.user));
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(async (username: string, email: string, password: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.register(username, email, password);
      apiClient.setToken(response.token);
      setUser(response.user);
      localStorage.setItem('user', JSON.stringify(response.user));
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    apiClient.clearToken();
    localStorage.removeItem('user');
    localStorage.removeItem('DEV_MODE');
    setUser(null);
  }, []);

  return {
    user,
    isAuthenticated: !!user,
    isLoading,
    error,
    login,
    register,
    logout,
  };
};
