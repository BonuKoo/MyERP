import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useEmployeeDetail, useEmployeeMutations, useMyEmployee } from '../hooks/useEmployees';
import { useDepartmentList } from '../hooks/useDepartments';
import { usePositionList } from '../hooks/usePositions';
import { useMonthlySummary } from '../hooks/useAttendance';
import { useLeaveRequestMutations, useLeaveRequestsByEmployee } from '../hooks/useLeaveRequests';
import { useAuth } from '../auth/AuthContext';
import type { LeaveStatus, LeaveType } from '../types/api';

const CURRENT_YEAR_MONTH = new Date().toISOString().slice(0, 7);

const LEAVE_STATUS_LABEL: Record<LeaveStatus, string> = {
  PENDING: '대기',
  APPROVED: '승인',
  REJECTED: '반려',
};

export default function EmployeeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const employeeId = Number(id);
  const navigate = useNavigate();
  const { isOwner } = useAuth();

  const employeeQuery = useEmployeeDetail(employeeId);
  const departmentsQuery = useDepartmentList();
  const positionsQuery = usePositionList();
  const myEmployeeQuery = useMyEmployee();
  const { resignMutation } = useEmployeeMutations();

  const [resignDate, setResignDate] = useState(new Date().toISOString().slice(0, 10));
  const [showResignForm, setShowResignForm] = useState(false);
  const [yearMonth, setYearMonth] = useState(CURRENT_YEAR_MONTH);
  const [leaveType, setLeaveType] = useState<LeaveType>('FULL_DAY');
  const [leaveStart, setLeaveStart] = useState(new Date().toISOString().slice(0, 10));
  const [leaveEnd, setLeaveEnd] = useState(new Date().toISOString().slice(0, 10));
  const [leaveReason, setLeaveReason] = useState('');

  const isSelf = myEmployeeQuery.data?.id === employeeId;
  // STAFF가 타인의 근태/휴가를 조회하면 백엔드가 403을 준다. 그 요청 자체를
  // 보내지 않도록 여기서 미리 걸러 불필요한 에러 화면을 막는다.
  const canViewHrData = isOwner || isSelf;

  const summaryQuery = useMonthlySummary(canViewHrData ? employeeId : undefined, yearMonth);
  const leaveRequestsQuery = useLeaveRequestsByEmployee(canViewHrData ? employeeId : undefined);
  const { applyMutation, approveMutation, rejectMutation } = useLeaveRequestMutations(employeeId);

  function handleResign(e: FormEvent) {
    e.preventDefault();
    resignMutation.mutate(
      { id: employeeId, request: { resignationDate: resignDate } },
      { onSuccess: () => setShowResignForm(false) },
    );
  }

  function handleApplyLeave(e: FormEvent) {
    e.preventDefault();
    applyMutation.mutate(
      { leaveType, startDate: leaveStart, endDate: leaveEnd, reason: leaveReason || undefined },
      { onSuccess: () => setLeaveReason('') },
    );
  }

  if (employeeQuery.isLoading) return <p>불러오는 중...</p>;
  if (employeeQuery.isError || !employeeQuery.data) {
    return <p className="error-message">사원을 찾을 수 없습니다.</p>;
  }

  const employee = employeeQuery.data;
  const departmentName = departmentsQuery.data?.content.find((d) => d.id === employee.departmentId)?.name ?? '-';
  const positionName = positionsQuery.data?.content.find((p) => p.id === employee.positionId)?.name ?? '-';

  return (
    <div className="page">
      <div className="page-header">
        <h1>{employee.name}</h1>
        <button type="button" onClick={() => navigate('/employees')}>
          목록으로
        </button>
      </div>

      <p>부서: {departmentName}</p>
      <p>직책: {positionName}</p>
      <p>연락처: {employee.phone ?? '-'}</p>
      <p>이메일: {employee.email ?? '-'}</p>
      <p>입사일: {employee.hireDate}</p>
      <p>
        상태:{' '}
        <span className={employee.active ? 'badge badge-success' : 'badge badge-neutral'}>
          {employee.active ? '재직' : `퇴사 (${employee.resignationDate ?? '-'})`}
        </span>
      </p>

      <div className="form-actions">
        {isOwner && (
          <button type="button" onClick={() => navigate(`/employees/${employeeId}/edit`)}>
            정보 수정
          </button>
        )}
        {isOwner && employee.active && !showResignForm && (
          <button type="button" className="button-danger" onClick={() => setShowResignForm(true)}>
            퇴사 처리
          </button>
        )}
      </div>

      {showResignForm && (
        <form onSubmit={handleResign} className="inline-form">
          <label>
            퇴사일
            <input type="date" value={resignDate} onChange={(e) => setResignDate(e.target.value)} required />
          </label>
          <button type="submit" disabled={resignMutation.isPending}>
            확인
          </button>
          <button type="button" onClick={() => setShowResignForm(false)}>
            취소
          </button>
        </form>
      )}

      {canViewHrData && (
        <>
          <h2>근태 집계</h2>
          <label>
            조회 월
            <input type="month" value={yearMonth} onChange={(e) => setYearMonth(e.target.value)} />
          </label>
          {summaryQuery.isLoading && <p>불러오는 중...</p>}
          {summaryQuery.data && (
            <table>
              <thead>
                <tr>
                  <th>근무일수</th>
                  <th>지각(분)</th>
                  <th>조퇴(분)</th>
                  <th>초과근무(분)</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{summaryQuery.data.workDays}</td>
                  <td>{summaryQuery.data.totalLateMinutes}</td>
                  <td>{summaryQuery.data.totalEarlyLeaveMinutes}</td>
                  <td>{summaryQuery.data.totalOvertimeMinutes}</td>
                </tr>
              </tbody>
            </table>
          )}

          <h2>휴가</h2>
          {leaveRequestsQuery.data && (
            <table>
              <thead>
                <tr>
                  <th>종류</th>
                  <th>기간</th>
                  <th>일수</th>
                  <th>사유</th>
                  <th>상태</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {leaveRequestsQuery.data.map((leave) => (
                  <tr key={leave.id}>
                    <td>{leave.leaveType === 'FULL_DAY' ? '연차' : '반차'}</td>
                    <td>{leave.startDate === leave.endDate ? leave.startDate : `${leave.startDate} ~ ${leave.endDate}`}</td>
                    <td>{leave.leaveDays}</td>
                    <td>{leave.reason ?? '-'}</td>
                    <td>
                      <span
                        className={
                          leave.status === 'APPROVED'
                            ? 'badge badge-success'
                            : leave.status === 'REJECTED'
                              ? 'badge badge-danger'
                              : 'badge badge-neutral'
                        }
                      >
                        {LEAVE_STATUS_LABEL[leave.status]}
                      </span>
                    </td>
                    <td>
                      {isOwner && leave.status === 'PENDING' && (
                        <>
                          <button
                            type="button"
                            onClick={() => approveMutation.mutate(leave.id)}
                            disabled={approveMutation.isPending}
                          >
                            승인
                          </button>
                          <button
                            type="button"
                            className="button-danger"
                            onClick={() => rejectMutation.mutate(leave.id)}
                            disabled={rejectMutation.isPending}
                          >
                            반려
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {leaveRequestsQuery.data?.length === 0 && <p className="category-list-empty">휴가 신청 내역이 없습니다.</p>}

          {isSelf && (
            <form onSubmit={handleApplyLeave} className="inline-form">
              <select value={leaveType} onChange={(e) => setLeaveType(e.target.value as LeaveType)}>
                <option value="FULL_DAY">연차</option>
                <option value="HALF_DAY">반차</option>
              </select>
              <input type="date" value={leaveStart} onChange={(e) => setLeaveStart(e.target.value)} required />
              <input type="date" value={leaveEnd} onChange={(e) => setLeaveEnd(e.target.value)} required />
              <input
                type="text"
                placeholder="사유"
                value={leaveReason}
                onChange={(e) => setLeaveReason(e.target.value)}
              />
              <button type="submit" disabled={applyMutation.isPending}>
                휴가 신청
              </button>
              {applyMutation.isError && (
                <span className="error-message">신청에 실패했습니다. 반차는 시작일과 종료일이 같아야 합니다.</span>
              )}
            </form>
          )}
        </>
      )}
    </div>
  );
}
