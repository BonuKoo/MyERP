import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { login as loginApi } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import type { ErrorResponse } from '../types/api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: loginApi,
    onSuccess: (data) => {
      login(data.accessToken, {
        userId: data.userId,
        email: data.email,
        name: data.name,
        role: data.role,
      });
      navigate('/partners', { replace: true });
    },
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({ email, password });
  }

  return (
    <div className="auth-page">
      <h1>로그인</h1>
      <form onSubmit={handleSubmit}>
        <label>
          이메일
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? '로그인 중...' : '로그인'}
        </button>
        {mutation.isError && (
          <p className="error-message">
            {(mutation.error as AxiosError<ErrorResponse>).response?.data?.message ??
              '로그인에 실패했습니다.'}
          </p>
        )}
      </form>
      <p>
        계정이 없으신가요? <Link to="/signup">회원가입</Link>
      </p>
    </div>
  );
}
