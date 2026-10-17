import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi, tokenStorage, setAuthErrorHandler } from '../api/filesApi.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  // Set up 401 interceptor callback
  useEffect(() => {
    setAuthErrorHandler(() => {
      setUser(null);
    });
  }, []);

  // Check token on initial load
  useEffect(() => {
    async function loadUser() {
      const token = tokenStorage.get();
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const profile = await authApi.me();
        setUser(profile);
      } catch (err) {
        tokenStorage.clear();
        setUser(null);
      } finally {
        setLoading(false);
      }
    }

    loadUser();
  }, []);

  const login = async (email, password) => {
    const data = await authApi.login(email, password);
    tokenStorage.set(data.token);
    setUser(data.user);
    return data;
  };

  const register = async (name, email, password) => {
    const data = await authApi.register(name, email, password);
    tokenStorage.set(data.token);
    setUser(data.user);
    return data;
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
