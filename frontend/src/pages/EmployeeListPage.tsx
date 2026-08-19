import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useEmployeeList } from '../hooks/useEmployees';
import { useDepartmentList } from '../hooks/useDepartments';
import { usePositionList } from '../hooks/usePositions';
import { useAuth } from '../auth/AuthContext';

const PAGE_SIZE = 20;

export default function EmployeeListPage() {
  const [page, setPage] = useState(0);
  const [departmentId, setDepartmentId] = useState<number | ''>('');
  const [positionId, setPositionId] = useState<number | ''>('');
  const [name, setName] = useState('');
  const { isOwner } = useAuth();

  const departmentsQuery = useDepartmentList();
  const positionsQuery = usePositionList();
  const employeesQuery = useEmployeeList(page, PAGE_SIZE, {
    departmentId: departmentId === '' ? undefined : departmentId,
    positionId: positionId === '' ? undefined : positionId,
    name: name || undefined,
  });

  const departmentNameById = new Map(departmentsQuery.data?.content.map((d) => [d.id, d.name]));
  const positionNameById = new Map(positionsQuery.data?.content.map((p) => [p.id, p.name]));

  if (employeesQuery.isLoading) return <p>불러오는 중...</p>;
  if (employeesQuery.isError) {
    return <p className="error-message">사원 목록을 불러오지 못했습니다.</p>;
  }

  const data = employeesQuery.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.totalCount / PAGE_SIZE)) : 1;

  return (
    <div className="page">
      <div className="page-header">
        <h1>사원 관리</h1>
        {isOwner && (
          <Link to="/employees/new">
            <button type="button">새 사원 등록</button>
          </Link>
        )}
      </div>

      <form className="inline-form" onSubmit={(e) => e.preventDefault()}>
        <select
          value={departmentId}
          onChange={(e) => { setDepartmentId(e.target.value === '' ? '' : Number(e.target.value)); setPage(0); }}
        >
          <option value="">전체 부서</option>
          {departmentsQuery.data?.content.map((d) => (
            <option key={d.id} value={d.id}>{d.name}</option>
          ))}
        </select>
        <select
          value={positionId}
          onChange={(e) => { setPositionId(e.target.value === '' ? '' : Number(e.target.value)); setPage(0); }}
        >
          <option value="">전체 직책</option>
          {positionsQuery.data?.content.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>
        <input
          type="text"
          placeholder="이름 검색"
          value={name}
          onChange={(e) => { setName(e.target.value); setPage(0); }}
        />
      </form>

      <table>
        <thead>
          <tr>
            <th>이름</th>
            <th>부서</th>
            <th>직책</th>
            <th>입사일</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          {data?.content.map((employee) => (
            <tr key={employee.id}>
              <td>
                <Link to={`/employees/${employee.id}`}>{employee.name}</Link>
              </td>
              <td>{departmentNameById.get(employee.departmentId) ?? '-'}</td>
              <td>{positionNameById.get(employee.positionId) ?? '-'}</td>
              <td>{employee.hireDate}</td>
              <td>
                <span className={employee.active ? 'badge badge-success' : 'badge badge-neutral'}>
                  {employee.active ? '재직' : '퇴사'}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {data?.content.length === 0 && <p className="category-list-empty">조건에 맞는 사원이 없습니다.</p>}

      <div className="pagination">
        <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
          이전
        </button>
        <span>
          {page + 1} / {totalPages}
        </span>
        <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
          다음
        </button>
      </div>
    </div>
  );
}
