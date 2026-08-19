import client from './client';
import type { AttendanceResponse, AttendanceSummaryResponse } from '../types/api';

export async function clockIn(): Promise<AttendanceResponse> {
  const { data } = await client.post<AttendanceResponse>('/api/attendances/clock-in');
  return data;
}

export async function clockOut(): Promise<AttendanceResponse> {
  const { data } = await client.post<AttendanceResponse>('/api/attendances/clock-out');
  return data;
}

export async function fetchMonthlySummary(
  employeeId: number,
  yearMonth: string,
): Promise<AttendanceSummaryResponse> {
  const { data } = await client.get<AttendanceSummaryResponse>(`/api/employees/${employeeId}/attendances`, {
    params: { yearMonth },
  });
  return data;
}
