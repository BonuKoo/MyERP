import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCalculateSalaries, useSalaryList, useSalarySetting, useSalarySettingMutation } from '../hooks/useSalary';
import { useEmployeeList } from '../hooks/useEmployees';

const CURRENT_YEAR_MONTH = new Date().toISOString().slice(0, 7);
const PAGE_SIZE = 20;

export default function SalaryPage() {
  const settingQuery = useSalarySetting();
  const settingMutation = useSalarySettingMutation();
  const calculateMutation = useCalculateSalaries();

  const [dailyWageInput, setDailyWageInput] = useState('');
  const [yearMonth, setYearMonth] = useState(CURRENT_YEAR_MONTH);
  const [page, setPage] = useState(0);

  const salariesQuery = useSalaryList(yearMonth, page, PAGE_SIZE);
  // 사원 이름 표시용 — 재직 여부와 무관하게 전부 필요하므로 큰 사이즈로 한 번에 가져온다.
  const employeesQuery = useEmployeeList(0, 1000, {});
  const employeeNameById = new Map(employeesQuery.data?.content.map((e) => [e.id, e.name]));

  function handleUpdateSetting(e: FormEvent) {
    e.preventDefault();
    const value = Number(dailyWageInput);
    if (!value || value <= 0) return;
    settingMutation.mutate({ dailyWage: value }, { onSuccess: () => setDailyWageInput('') });
  }

  function handleCalculate() {
    calculateMutation.mutate(yearMonth);
  }

  const data = salariesQuery.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.totalCount / PAGE_SIZE)) : 1;

  return (
    <div className="page">
      <h1>급여 관리</h1>

      <h2>급여 설정</h2>
      <p>현재 일당: {settingQuery.data?.dailyWage.toLocaleString() ?? '...'}</p>
      <form onSubmit={handleUpdateSetting} className="inline-form">
        <input
          type="number"
          min={0.01}
          step={1}
          placeholder="새 일당"
          value={dailyWageInput}
          onChange={(e) => setDailyWageInput(e.target.value)}
        />
        <button type="submit" disabled={settingMutation.isPending}>
          변경
        </button>
      </form>

      <h2>급여 계산 및 조회</h2>
      <label>
        대상 월
        <input type="month" value={yearMonth} onChange={(e) => { setYearMonth(e.target.value); setPage(0); }} />
      </label>
      <div className="form-actions">
        <button type="button" onClick={handleCalculate} disabled={calculateMutation.isPending}>
          {calculateMutation.isPending ? '계산 중...' : '이 달 급여 계산 실행'}
        </button>
      </div>
      <p className="form-hint">재직중인 전 직원의 급여를 다시 계산해 저장한다. 이미 계산된 달이면 덮어쓴다.</p>
      {calculateMutation.isError && <p className="error-message">급여 계산에 실패했습니다.</p>}

      {salariesQuery.isLoading && <p>불러오는 중...</p>}
      {data && (
        <>
          <table>
            <thead>
              <tr>
                <th>사원</th>
                <th>근무일수</th>
                <th>기본급</th>
                <th>직책수당</th>
                <th>초과수당</th>
                <th>총지급액</th>
                <th>공제합계</th>
                <th>실수령액</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((salary) => (
                <tr key={salary.id}>
                  <td>{employeeNameById.get(salary.employeeId) ?? `#${salary.employeeId}`}</td>
                  <td>{salary.workDays}</td>
                  <td>{salary.basePay.toLocaleString()}</td>
                  <td>{salary.positionAllowance.toLocaleString()}</td>
                  <td>{salary.overtimePay.toLocaleString()}</td>
                  <td>{salary.grossPay.toLocaleString()}</td>
                  <td>{salary.totalDeduction.toLocaleString()}</td>
                  <td>{salary.netPay.toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {data.content.length === 0 && (
            <p className="category-list-empty">해당 월에 계산된 급여가 없습니다.</p>
          )}

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
        </>
      )}
    </div>
  );
}
