import client from './client';
import type { LeaveRequestApplyRequest, LeaveRequestResponse } from '../types/api';

export async function applyLeave(
  employeeId: number,
  request: LeaveRequestApplyRequest,
): Promise<LeaveRequestResponse> {
  const { data } = await client.post<LeaveRequestResponse>(
    `/api/employees/${employeeId}/leave-requests`,
    request,
  );
  return data;
}

export async function fetchLeaveRequestsByEmployee(employeeId: number): Promise<LeaveRequestResponse[]> {
  const { data } = await client.get<LeaveRequestResponse[]>(`/api/employees/${employeeId}/leave-requests`);
  return data;
}

export async function approveLeaveRequest(id: number): Promise<void> {
  await client.patch(`/api/leave-requests/${id}/approve`);
}

export async function rejectLeaveRequest(id: number): Promise<void> {
  await client.patch(`/api/leave-requests/${id}/reject`);
}
