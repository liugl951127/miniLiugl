/**
 * HTTP 请求封装(基于 axios)
 */
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse, DualRecordError } from '@/types';

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    // 从 localStorage 读取 token
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // 注入 traceId
    const traceId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    config.headers['X-Trace-Id'] = traceId;
    return config;
  },
  (error) => Promise.reject(error),
);

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const data = response.data;
    if (data.code !== 0) {
      // 业务错误
      const error = new DualRecordError(data.code, data.message, data.traceId);
      // 401 未授权
      if (data.code === 401) {
        localStorage.removeItem('auth_token');
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }
    return response;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      const message = data?.message || `HTTP ${status} 错误`;
      return Promise.reject(new DualRecordError(status, message));
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new DualRecordError(1001, '请求超时,请检查网络'));
    }
    return Promise.reject(new DualRecordError(1000, error.message || '网络错误'));
  },
);

/**
 * 通用 GET 请求
 */
export function get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
  return instance.get<ApiResponse<T>>(url, { params, ...config }).then((r) => r.data.data);
}

/**
 * 通用 POST 请求
 */
export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return instance.post<ApiResponse<T>>(url, data, config).then((r) => r.data.data);
}

/**
 * 文件上传(支持进度)
 */
export function upload<T>(
  url: string,
  formData: FormData,
  onProgress?: (percent: number) => void,
): Promise<T> {
  return instance
    .post<ApiResponse<T>>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded * 100) / e.total));
        }
      },
    })
    .then((r) => r.data.data);
}

export default instance;
