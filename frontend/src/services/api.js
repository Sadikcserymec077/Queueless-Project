import axios from "axios";

export const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081/api";
export const SOCKET_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, "");

const TOKEN_KEY = "ql_token";
const USER_KEY = "ql_user";

export const storage = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  setToken: (token) => localStorage.setItem(TOKEN_KEY, token),
  removeToken: () => localStorage.removeItem(TOKEN_KEY),
  getUser: () => {
    const value = localStorage.getItem(USER_KEY);
    return value ? JSON.parse(value) : null;
  },
  setUser: (user) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  removeUser: () => localStorage.removeItem(USER_KEY)
};

export const client = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" }
});

client.interceptors.request.use((config) => {
  const token = storage.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function unwrap(request) {
  const response = await request;
  return response.data.data;
}

export const authApi = {
  login: (payload) => unwrap(client.post("/auth/login", payload)),
  register: (payload) => unwrap(client.post("/auth/register", payload)),
  me: () => unwrap(client.get("/users/me")),
  verifyEmail: (token) => unwrap(client.post(`/auth/verify-email?token=${token}`)),
  forgotPassword: (payload) => unwrap(client.post("/auth/forgot-password", payload)),
  resetPassword: (payload) => unwrap(client.post("/auth/reset-password", payload))
};

export const organizationsApi = {
  search: (params = {}) => unwrap(client.get("/organizations", { params })),
  create: (payload) => unwrap(client.post("/organizations", payload)),
  update: (id, payload) => unwrap(client.put(`/organizations/${id}`, payload)),
  remove: (id) => unwrap(client.delete(`/organizations/${id}`)),
  approve: (id) => unwrap(client.put(`/organizations/${id}/approve`)),
  reject: (id) => unwrap(client.put(`/organizations/${id}/reject`))
};

export const organizationUsersApi = {
  add: (payload) => unwrap(client.post("/organizations/users/add", payload)),
  list: () => unwrap(client.get("/organizations/users")),
  remove: (targetUserEmail) => unwrap(client.delete(`/organizations/users/remove?targetUserEmail=${targetUserEmail}`))
};

export const countersApi = {
  byOrganization: (organizationId) => unwrap(client.get(`/counters/organization/${organizationId}`)),
  create: (payload) => unwrap(client.post("/counters", payload)),
  update: (id, payload) => unwrap(client.put(`/counters/${id}`, payload)),
  disable: (id) => unwrap(client.patch(`/counters/${id}/disable`))
};

export const tokensApi = {
  book: (counterId) => unwrap(client.post("/tokens", { counterId })),
  active: () => unwrap(client.get("/tokens/me/active")),
  status: (id) => unwrap(client.get(`/tokens/${id}/status`)),
  cancel: (id) => unwrap(client.patch(`/tokens/${id}/cancel`)),
  history: (params = {}) => unwrap(client.get("/users/history", { params })),
  adminSearch: (params = {}) => unwrap(client.get("/tokens/admin/search", { params })),
  counterQueue: (counterId) => unwrap(client.get(`/tokens/counters/${counterId}/queue`)),
  callNext: (counterId) => unwrap(client.post(`/tokens/counters/${counterId}/call-next`)),
  complete: (id) => unwrap(client.patch(`/tokens/${id}/complete`)),
  skip: (id) => unwrap(client.patch(`/tokens/${id}/skip`)),
  requeue: (id) => unwrap(client.patch(`/tokens/${id}/requeue`)),
  requestCancel: (id) => unwrap(client.post(`/tokens/${id}/request-cancel`)),
  verifyQr: (qrPayload) => unwrap(client.post("/tokens/verify-qr", { qrPayload }))
};

export const notificationsApi = {
  list: (params = {}) => unwrap(client.get("/notifications", { params })),
  markRead: (id) => unwrap(client.patch(`/notifications/${id}/read`))
};

export const analyticsApi = {
  dashboard: () => unwrap(client.get("/analytics/dashboard"))
};
