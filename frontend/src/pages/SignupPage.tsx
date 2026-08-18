import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { errorMessageOf } from '../api/client';
import { register as registerApi } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import type { UserRole } from '../types/api';

export default function SignupPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [role, setRole] = useState<UserRole>('STAFF');
  const navigate = useNavigate();
  const { isAuthenticated, isOwner } = useAuth();

  const mutation = useMutation({
    mutationFn: registerApi,
    // 로그인한 OWNER가 직원 계정을 만든 경우엔 로그인 화면으로 보낼 이유가 없다.
    onSuccess: () => {
      navigate(isOwner ? '/partners' : '/login', { replace: true });
    },
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({ email, password, name, role });
  }

  return (
    <div className="auth-page">
      <h1>{isOwner ? '사용자 등록' : '회원가입'}</h1>
      {/*
        계정 생성은 사업주(OWNER)만 할 수 있다. 다만 최초 사용자가 한 명도 없을 때는
        최초 OWNER를 만들 수 있도록 서버가 예외적으로 허용하므로, 화면 자체는 막지 않고
        서버가 403을 주면 안내한다(어느 쪽인지는 서버만 알 수 있다).
      */}
      {!isAuthenticated && (
        <p className="form-hint">
          계정 생성은 사업주만 할 수 있습니다. 최초 사용자를 만드는 경우가 아니라면
          관리자에게 계정 생성을 요청하세요.
        </p>
      )}
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
          <p className="error-message">{errorMessageOf(mutation.error, '회원가입에 실패했습니다.')}</p>
        )}
      </form>
      <p>
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </div>
  );
}
