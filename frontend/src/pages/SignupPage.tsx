import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { register as registerApi } from '../api/auth';
import type { ErrorResponse, UserRole } from '../types/api';

export default function SignupPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [role, setRole] = useState<UserRole>('STAFF');
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: registerApi,
    onSuccess: () => {
      navigate('/login', { replace: true });
    },
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({ email, password, name, role });
  }

  return (
    <div className="auth-page">
      <h1>회원가입</h1>
      <form onSubmit={handleSubmit}>
        <label>
          이메일
          <input
            type="email"
            maxLength={100}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label>
          비밀번호 (8~100자, 영문 대/소문자·숫자·특수문자 각 1자 이상 포함)
          <input
            type="password"
            minLength={8}
            maxLength={100}
            pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}"
            title="8~100자이며 영문 대문자, 소문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다."
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <label>
          이름
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          권한
          <select value={role} onChange={(e) => setRole(e.target.value as UserRole)}>
            <option value="STAFF">직원 (STAFF)</option>
            <option value="OWNER">사업주 (OWNER)</option>
          </select>
        </label>
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? '가입 중...' : '가입하기'}
        </button>
        {mutation.isError && (
          <p className="error-message">
            {(mutation.error as AxiosError<ErrorResponse>).response?.data?.message ??
              '회원가입에 실패했습니다.'}
          </p>
        )}
      </form>
      <p>
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </div>
  );
}
