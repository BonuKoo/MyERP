import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { AxiosError } from 'axios';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { fetchEmployee } from '../api/employee';
import { useEmployeeMutations } from '../hooks/useEmployees';
import { useDepartmentList } from '../hooks/useDepartments';
import { usePositionList } from '../hooks/usePositions';
import type { EmployeeRequest, ErrorResponse } from '../types/api';

const emptyForm: EmployeeRequest = {
  departmentId: 0,
  positionId: 0,
  name: '',
  phone: '',
  email: '',
  hireDate: new Date().toISOString().slice(0, 10),
};

export default function EmployeeFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = id !== undefined;
  const navigate = useNavigate();
  const { createMutation, updateMutation } = useEmployeeMutations();
  const departmentsQuery = useDepartmentList();
  const positionsQuery = usePositionList();

  const [form, setForm] = useState<EmployeeRequest>(emptyForm);
  const [companyUserId, setCompanyUserId] = useState('');

  const employeeQuery = useQuery({
    queryKey: ['employee', id],
    queryFn: () => fetchEmployee(Number(id)),
    enabled: isEdit,
  });

  useEffect(() => {
    if (employeeQuery.data) {
      const e = employeeQuery.data;
      setForm({
        departmentId: e.departmentId,
        positionId: e.positionId,
        name: e.name,
        phone: e.phone ?? '',
        email: e.email ?? '',
        hireDate: e.hireDate,
      });
      setCompanyUserId(e.companyUserId?.toString() ?? '');
    }
  }, [employeeQuery.data]);

  useEffect(() => {
    if (!isEdit && departmentsQuery.data?.content.length && form.departmentId === 0) {
      setForm((f) => ({ ...f, departmentId: departmentsQuery.data.content[0].id }));
    }
    if (!isEdit && positionsQuery.data?.content.length && form.positionId === 0) {
      setForm((f) => ({ ...f, positionId: positionsQuery.data.content[0].id }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [departmentsQuery.data, positionsQuery.data]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const request: EmployeeRequest = {
      ...form,
      companyUserId: companyUserId === '' ? undefined : Number(companyUserId),
    };
    if (isEdit) {
      updateMutation.mutate({ id: Number(id), request }, { onSuccess: () => navigate(`/employees/${id}`) });
    } else {
      createMutation.mutate(request, { onSuccess: (saved) => navigate(`/employees/${saved.id}`) });
    }
  }

  const pending = createMutation.isPending || updateMutation.isPending;
  const mutationError = (createMutation.error ?? updateMutation.error) as AxiosError<ErrorResponse> | undefined;

  return (
    <div className="page">
      <h1>{isEdit ? '사원 정보 수정' : '새 사원 등록'}</h1>
      <form onSubmit={handleSubmit}>
        <label>
          부서
          <select
            value={form.departmentId}
            onChange={(e) => setForm({ ...form, departmentId: Number(e.target.value) })}
            required
          >
            <option value={0} disabled>선택하세요</option>
            {departmentsQuery.data?.content.map((d) => (
              <option key={d.id} value={d.id}>{d.name}</option>
            ))}
          </select>
        </label>
        <label>
          직책
          <select
            value={form.positionId}
            onChange={(e) => setForm({ ...form, positionId: Number(e.target.value) })}
            required
          >
            <option value={0} disabled>선택하세요</option>
            {positionsQuery.data?.content.map((p) => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
        </label>
        <label>
          이름
          <input
            type="text"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
        </label>
        <label>
          연락처
          <input
            type="text"
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
          />
        </label>
        <label>
          이메일
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </label>
        <label>
          입사일
          <input
            type="date"
            value={form.hireDate}
            onChange={(e) => setForm({ ...form, hireDate: e.target.value })}
            required
          />
        </label>
        <label>
          연결할 로그인 계정 id (선택)
          <input
            type="number"
            min={1}
            value={companyUserId}
            onChange={(e) => setCompanyUserId(e.target.value)}
          />
          <span className="form-hint">
            이 사원이 직접 로그인해 근태/휴가를 셀프서비스로 쓸 계정의 id. 비워두면 계정 없이 인사정보만 관리된다.
          </span>
        </label>

        {mutationError && (
          <p className="error-message">
            {mutationError.response?.data?.message ?? '저장에 실패했습니다.'}
          </p>
        )}

        <div className="form-actions">
          <button type="submit" disabled={pending}>
            {pending ? '저장 중...' : '저장'}
          </button>
          <button type="button" onClick={() => navigate('/employees')}>
            취소
          </button>
        </div>
      </form>
    </div>
  );
}
