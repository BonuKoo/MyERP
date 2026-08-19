import { useState } from 'react';
import type { FormEvent } from 'react';
import type { AxiosError } from 'axios';
import { useMyEmployee } from '../hooks/useEmployees';
import { useAttendanceMutations, useMonthlySummary } from '../hooks/useAttendance';
import { useLeaveRequestMutations, useLeaveRequestsByEmployee } from '../hooks/useLeaveRequests';
import type { AttendanceResponse, ErrorResponse, LeaveStatus, LeaveType } from '../types/api';

const CURRENT_YEAR_MONTH = new Date().toISOString().slice(0, 7);

const LEAVE_STATUS_LABEL: Record<LeaveStatus, string> = {
  PENDING: '대기',
  APPROVED: '승인',
  REJECTED: '반려',
};

function formatTime(value: string | null): string {
  if (!value) return '-';
  return value.slice(11, 16);
}

export default function MyAttendancePage() {
  const myEmployeeQuery = useMyEmployee();
  const employeeId = myEmployeeQuery.data?.id;

  const [yearMonth, setYearMonth] = useState(CURRENT_YEAR_MONTH);
  const [todayRecord, setTodayRecord] = useState<AttendanceResponse | null>(null);
  const [leaveType, setLeaveType] = useState<LeaveType>('FULL_DAY');
  const [leaveStart, setLeaveStart] = useState(new Date().toISOString().slice(0, 10));
  const [leaveEnd, setLeaveEnd] = useState(new Date().toISOString().slice(0, 10));
  const [leaveReason, setLeaveReason] = useState('');

  const { clockInMutation, clockOutMutation } = useAttendanceMutations();
  const summaryQuery = useMonthlySummary(employeeId, yearMonth);
  const leaveRequestsQuery = useLeaveRequestsByEmployee(employeeId);
  const { applyMutation } = useLeaveRequestMutations(employeeId);

  function handleClockIn() {
    clockInMutation.mutate(undefined, { onSuccess: setTodayRecord });
  }

  function handleClockOut() {
    clockOutMutation.mutate(undefined, { onSuccess: setTodayRecord });
  }

  function handleApplyLeave(e: FormEvent) {
    e.preventDefault();
    applyMutation.mutate(
      { leaveType, startDate: leaveStart, endDate: leaveEnd, reason: leaveReason || undefined },
      { onSuccess: () => setLeaveReason('') },
    );
  }

  if (myEmployeeQuery.isLoading) return <p>불러오는 중...</p>;

  if (!myEmployeeQuery.data) {
    return (
      <div className="page">
        <h1>내 근태/휴가</h1>
        <p className="form-hint">
          이 계정에 연결된 사원 정보가 없습니다. 사원 등록 시 로그인 계정을 연결하면
          출퇴근·휴가 신청을 셀프서비스로 쓸 수 있습니다.
        </p>
      </div>
    );
  }

  const employee = myEmployeeQuery.data;
  const clockInError = clockInMutation.error as AxiosError<ErrorResponse> | undefined;
  const clockOutError = clockOutMutation.error as AxiosError<ErrorResponse> | undefined;

  return (
    <div className="page">
      <h1>내 근태/휴가</h1>
      <p>{employee.name}님</p>

      <h2>오늘 출퇴근</h2>
      <p>
        출근: {formatTime(todayRecord?.clockIn ?? null)} / 퇴근: {formatTime(todayRecord?.clockOut ?? null)}
      </p>
      <div className="form-actions">
        <button type="button" onClick={handleClockIn} disabled={clockInMutation.isPending}>
          출근
        </button>
        <button type="button" onClick={handleClockOut} disabled={clockOutMutation.isPending}>
          퇴근
        </button>
      </div>
      {clockInError && (
        <p className="error-message">{clockInError.response?.data?.message ?? '출근 처리에 실패했습니다.'}</p>
      )}
      {clockOutError && (
        <p className="error-message">{clockOutError.response?.data?.message ?? '퇴근 처리에 실패했습니다.'}</p>
      )}

      <h2>월별 근태 집계</h2>
      <label>
        조회 월
        <input type="month" value={yearMonth} onChange={(e) => setYearMonth(e.target.value)} />
      </label>
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

      <h2>휴가 신청</h2>
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
          신청
        </button>
      </form>
      {applyMutation.isError && (
        <p className="error-message">신청에 실패했습니다. 반차는 시작일과 종료일이 같아야 합니다.</p>
      )}

      <h2>내 휴가 신청 내역</h2>
      {leaveRequestsQuery.data && (
        <table>
          <thead>
            <tr>
              <th>종류</th>
              <th>기간</th>
              <th>일수</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {leaveRequestsQuery.data.map((leave) => (
              <tr key={leave.id}>
                <td>{leave.leaveType === 'FULL_DAY' ? '연차' : '반차'}</td>
                <td>{leave.startDate === leave.endDate ? leave.startDate : `${leave.startDate} ~ ${leave.endDate}`}</td>
                <td>{leave.leaveDays}</td>
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
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {leaveRequestsQuery.data?.length === 0 && <p className="category-list-empty">휴가 신청 내역이 없습니다.</p>}
    </div>
  );
}
