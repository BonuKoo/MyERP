import { useState } from 'react';
import type { FormEvent } from 'react';
import { useDepartmentList, useDepartmentMutations } from '../hooks/useDepartments';
import { usePositionList, usePositionMutations } from '../hooks/usePositions';
import { useAuth } from '../auth/AuthContext';
import type { DepartmentResponse, PositionResponse } from '../types/api';

function DepartmentSection() {
  const { isOwner } = useAuth();
  const departmentsQuery = useDepartmentList();
  const { createMutation, updateMutation, deactivateMutation } = useDepartmentMutations();

  const [newName, setNewName] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');

  function startEdit(department: DepartmentResponse) {
    setEditingId(department.id);
    setEditName(department.name);
  }

  function handleCreate(e: FormEvent) {
    e.preventDefault();
    if (!newName.trim()) return;
    createMutation.mutate({ name: newName }, { onSuccess: () => setNewName('') });
  }

  function handleSaveEdit(id: number) {
    if (!editName.trim()) return;
    updateMutation.mutate({ id, request: { name: editName } }, { onSuccess: () => setEditingId(null) });
  }

  return (
    <section>
      <h2>부서</h2>
      <table>
        <thead>
          <tr>
            <th>이름</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {departmentsQuery.data?.content.map((department) => (
            <tr key={department.id}>
              {editingId === department.id ? (
                <td>
                  <input type="text" value={editName} onChange={(e) => setEditName(e.target.value)} />
                </td>
              ) : (
                <td>{department.name}</td>
              )}
              <td>
                <span className={department.active ? 'badge badge-success' : 'badge badge-neutral'}>
                  {department.active ? '활성' : '비활성'}
                </span>
              </td>
              <td>
                {isOwner && editingId === department.id && (
                  <>
                    <button type="button" onClick={() => handleSaveEdit(department.id)} disabled={updateMutation.isPending}>
                      저장
                    </button>
                    <button type="button" onClick={() => setEditingId(null)}>
                      취소
                    </button>
                  </>
                )}
                {isOwner && editingId !== department.id && (
                  <>
                    <button type="button" className="button-success" onClick={() => startEdit(department)}>
                      수정
                    </button>
                    {department.active && (
                      <button
                        type="button"
                        className="button-danger"
                        onClick={() => deactivateMutation.mutate(department.id)}
                        disabled={deactivateMutation.isPending}
                      >
                        비활성화
                      </button>
                    )}
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {departmentsQuery.data?.content.length === 0 && (
        <p className="category-list-empty">등록된 부서가 없습니다.</p>
      )}
      {isOwner && (
        <form onSubmit={handleCreate} className="inline-form">
          <input
            type="text"
            placeholder="새 부서명"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
          />
          <button type="submit" disabled={createMutation.isPending}>
            추가
          </button>
        </form>
      )}
    </section>
  );
}

function PositionSection() {
  const { isOwner } = useAuth();
  const positionsQuery = usePositionList();
  const { createMutation, updateMutation, deactivateMutation } = usePositionMutations();

  const [newName, setNewName] = useState('');
  const [newAllowance, setNewAllowance] = useState(0);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [editAllowance, setEditAllowance] = useState(0);

  function startEdit(position: PositionResponse) {
    setEditingId(position.id);
    setEditName(position.name);
    setEditAllowance(position.allowance);
  }

  function handleCreate(e: FormEvent) {
    e.preventDefault();
    if (!newName.trim()) return;
    createMutation.mutate(
      { name: newName, allowance: newAllowance },
      { onSuccess: () => { setNewName(''); setNewAllowance(0); } },
    );
  }

  function handleSaveEdit(id: number) {
    if (!editName.trim()) return;
    updateMutation.mutate(
      { id, request: { name: editName, allowance: editAllowance } },
      { onSuccess: () => setEditingId(null) },
    );
  }

  return (
    <section>
      <h2>직책</h2>
      <table>
        <thead>
          <tr>
            <th>이름</th>
            <th>직책수당</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {positionsQuery.data?.content.map((position) => (
            <tr key={position.id}>
              {editingId === position.id ? (
                <>
                  <td>
                    <input type="text" value={editName} onChange={(e) => setEditName(e.target.value)} />
                  </td>
                  <td>
                    <input
                      type="number"
                      min={0}
                      value={editAllowance}
                      onChange={(e) => setEditAllowance(Number(e.target.value))}
                    />
                  </td>
                </>
              ) : (
                <>
                  <td>{position.name}</td>
                  <td>{position.allowance.toLocaleString()}</td>
                </>
              )}
              <td>
                <span className={position.active ? 'badge badge-success' : 'badge badge-neutral'}>
                  {position.active ? '활성' : '비활성'}
                </span>
              </td>
              <td>
                {isOwner && editingId === position.id && (
                  <>
                    <button type="button" onClick={() => handleSaveEdit(position.id)} disabled={updateMutation.isPending}>
                      저장
                    </button>
                    <button type="button" onClick={() => setEditingId(null)}>
                      취소
                    </button>
                  </>
                )}
                {isOwner && editingId !== position.id && (
                  <>
                    <button type="button" className="button-success" onClick={() => startEdit(position)}>
                      수정
                    </button>
                    {position.active && (
                      <button
                        type="button"
                        className="button-danger"
                        onClick={() => deactivateMutation.mutate(position.id)}
                        disabled={deactivateMutation.isPending}
                      >
                        비활성화
                      </button>
                    )}
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {positionsQuery.data?.content.length === 0 && (
        <p className="category-list-empty">등록된 직책이 없습니다.</p>
      )}
      {isOwner && (
        <form onSubmit={handleCreate} className="inline-form">
          <input
            type="text"
            placeholder="새 직책명"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
          />
          <input
            type="number"
            min={0}
            placeholder="직책수당"
            value={newAllowance}
            onChange={(e) => setNewAllowance(Number(e.target.value))}
          />
          <button type="submit" disabled={createMutation.isPending}>
            추가
          </button>
        </form>
      )}
    </section>
  );
}

export default function OrganizationPage() {
  return (
    <div className="page">
      <h1>조직 관리</h1>
      <div className="category-columns">
        <DepartmentSection />
        <PositionSection />
      </div>
    </div>
  );
}
