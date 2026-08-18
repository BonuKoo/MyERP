import axios from 'axios';

export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const TOKEN_KEY = 'accessToken';
export const USER_KEY = 'user';

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status;
    if (status === 401 && localStorage.getItem(TOKEN_KEY)) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    // 403은 401과 다르게 다뤄야 한다. 로그인은 유효하고 권한만 모자란 것이므로
    // 토큰을 지우거나 로그인 화면으로 보내면 안 된다(멀쩡한 세션이 끊긴다).
    // 각 화면이 에러 메시지로 처리하도록 그대로 흘려보낸다.
    return Promise.reject(err);
  },
);

/** 서버가 내려준 메시지를 우선 쓰고, 없으면 상태코드에 맞는 기본 문구를 준다. */
export function errorMessageOf(error: unknown, fallback: string): string {
  const err = error as { response?: { status?: number; data?: { message?: string } } };
  const serverMessage = err?.response?.data?.message;
  if (serverMessage) return serverMessage;
  if (err?.response?.status === 403) return '이 작업을 수행할 권한이 없습니다.';
  return fallback;
}

export default client;
