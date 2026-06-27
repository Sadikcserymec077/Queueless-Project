import { createContext, useContext, useMemo, useState } from "react";
import { authApi, storage } from "../services/api.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(storage.getUser());

  const saveSession = (authResponse) => {
    storage.setToken(authResponse.token);
    storage.setUser(authResponse.user);
    setUser(authResponse.user);
  };

  const value = useMemo(() => ({
    user,
    isAuthenticated: Boolean(user),
    async login(payload) {
      const response = await authApi.login(payload);
      saveSession(response);
      return response.user;
    },
    async register(payload) {
      const response = await authApi.register(payload);
      saveSession(response);
      return response.user;
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
