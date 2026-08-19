import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Layout() {
  const { isAuthenticated, isOwner, user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/partners" className="brand">
          MyERP
        </Link>
        {isAuthenticated && (
          <nav>
            <NavLink to="/partners">거래처</NavLink>
            <NavLink to="/categories">카테고리</NavLink>
            <NavLink to="/items">품목</NavLink>
            <NavLink to="/purchases">매입</NavLink>
            <NavLink to="/sales">매출</NavLink>
            <NavLink to="/payments">결제</NavLink>
            <NavLink to="/company-info">회사정보</NavLink>
            <NavLink to="/organization">조직</NavLink>
            <NavLink to="/employees">사원</NavLink>
            <NavLink to="/my-attendance">내근태</NavLink>
            {/* 급여 목록 조회 자체가 OWNER 전용(백엔드도 403으로 막는다) */}
            {isOwner && <NavLink to="/salary">급여</NavLink>}
            {/* 계정 생성은 OWNER만 가능하므로 진입점도 OWNER에게만 보인다 */}
            {isOwner && <NavLink to="/signup">사용자 등록</NavLink>}
            <span className="user-info">
              {user?.name} ({user?.role})
            </span>
            <button type="button" onClick={handleLogout}>
              로그아웃
            </button>
          </nav>
        )}
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}
