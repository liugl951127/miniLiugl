/**
 * 认证与权限 API
 */

import request from '../utils/request';

export interface LoginRequest {
  userNo: string;
  password: string;
  channel: string;
  deviceId?: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: {
    userId: number;
    userNo: string;
    name: string;
    roleCode: string;
    branchId: number;
    branchName: string;
    permissions: string[];
  };
  expiresIn: number;
}

export const login = (data: LoginRequest) =>
  request.post<LoginResponse>('/api/auth/login', data);

export const logout = () =>
  request.post<{ ok: boolean }>('/api/auth/logout');

export const refreshToken = (refreshToken: string) =>
  request.post<{ token: string; expiresIn: number }>('/api/auth/refresh', { refreshToken });

export const getCurrentUser = () =>
  request.get<LoginResponse['user']>('/api/auth/me');

export const changePassword = (oldPassword: string, newPassword: string) =>
  request.post<{ ok: boolean }>('/api/auth/password', { oldPassword, newPassword });
