import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Layout() {
  const { isAuthenticated, user, logout } = useAuth();
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
            <Link to="/partners">거래처</Link>
            <Link to="/categories">카테고리</Link>
            <Link to="/items">품목</Link>
            <Link to="/purchases">매입</Link>
            <Link to="/company-info">회사정보</Link>
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
