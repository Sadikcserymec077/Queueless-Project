import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { authApi, storage } from "../services/api.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(storage.getUser());

  const saveSession = (authResponse) => {
    storage.setToken(authResponse.token);
    storage.setUser(authResponse.user);
    setUser(authResponse.user);
  };

  useEffect(() => {
    async function loadUser() {
      if (storage.getToken()) {
        try {
          const freshUser = await authApi.me();
          storage.setUser(freshUser);
          setUser(freshUser);
        } catch (err) {
          // If token is invalid or expired (401/403), clear session
          if (err.response && (err.response.status === 401 || err.response.status === 403)) {
            storage.removeToken();
            storage.removeUser();
            setUser(null);
          } else {
            console.error("Failed to load user profile:", err);
          }
        }
      }
    }
    loadUser();
  }, []);

  const value = useMemo(() => ({
    user,
    isAuthenticated: Boolean(user),
    async login(payload) {
      const response = await authApi.login(payload);
      saveSession(response);
      return response.user;
    },
    async loginWithGoogle(accessToken) {
      const response = await authApi.loginWithGoogle({ token: accessToken });
      saveSession(response);
      return response.user;
    },
    async register(payload) {
      await authApi.register(payload);
      // Auto-login since email verification is disabled
      const loginResponse = await authApi.login({ email: payload.email, password: payload.password });
      saveSession(loginResponse);
      return loginResponse.user;
    },
    logout() {
      storage.removeToken();
      storage.removeUser();
      setUser(null);
    }
  }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
