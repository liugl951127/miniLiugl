/**
 * HTTP 请求封装(基于 axios)
 *
 * <p>增强特性:
 * <ul>
 *   <li>JWT Token 自动注入
 *   <li>Trace ID 全链路追踪
 *   <li>401 自动跳登录
 *   <li>网络错误统一处理
 *   <li>请求/响应/错误日志
 *   <li>上传进度回调
 * </ul>
 */
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
}

export class DualRecordError extends Error {
  constructor(
    public code: number,
    message: string,
    public traceId?: string,
  ) {
    super(message);
    this.name = 'DualRecordError';
  }
}

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000,
  withCredentials: false,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ============================================================
// 请求拦截器
// ============================================================
instance.interceptors.request.use(
  (config) => {
    // JWT 注入
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // Trace ID(全链路追踪)
    const traceId = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
    config.headers['X-Trace-Id'] = traceId;
    // 时间戳(防重放)
    config.headers['X-Timestamp'] = String(Date.now());
    return config;
  },
  (error) => Promise.reject(error),
);

// ============================================================
// 响应拦截器
// ============================================================
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const data = response.data;
    // 业务成功(code === 200)
    if (data.code === 200) {
      return response;
    }
    // 业务失败
    handleBusinessError(data);
    return Promise.reject(new DualRecordError(data.code, data.message || '业务错误'));
  },
  (error) => {
    return Promise.reject(handleHttpError(error));
  },
);

function handleBusinessError(data: ApiResponse<unknown>) {
  // 401:未授权
  if (data.code === 401) {
    ElMessage.error('登录已过期,请重新登录');
    localStorage.removeItem('auth_token');
    localStorage.removeItem('refresh_token');
    setTimeout(() => {
      window.location.href = '/login';
    }, 500);
    return;
  }
  // 403:无权限
  if (data.code === 403) {
    ElMessage.error('没有访问权限');
    return;
  }
  // 429:限流
  if (data.code === 429) {
    ElMessage.warning('操作过于频繁,请稍后再试');
    return;
  }
  // 5xx:服务器错误
  if (data.code >= 500) {
    ElMessage.error(`服务器错误: ${data.message}`);
    return;
  }
  // 其他业务错误
  ElMessage.error(data.message || '操作失败');
}

function handleHttpError(error: unknown): DualRecordError {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const e = error as any;
  if (e.response) {
    const { status, data } = e.response;
    const message = (data && data.message) || `HTTP ${status} 错误`;
    if (status === 401) {
      ElMessage.error('未授权,请登录');
      localStorage.removeItem('auth_token');
      setTimeout(() => (window.location.href = '/login'), 500);
    } else if (status === 429) {
      ElMessage.warning('请求过于频繁');
    } else if (status >= 500) {
      ElMessage.error('服务暂时不可用,请稍后重试');
    } else {
      ElMessage.error(message);
    }
    return new DualRecordError(status, message);
  }
  if (e.code === 'ECONNABORTED') {
    ElMessage.error('请求超时,请检查网络');
    return new DualRecordError(1001, '请求超时');
  }
  if (e.message?.includes('Network Error')) {
    ElMessage.error('网络错误,请检查连接');
    return new DualRecordError(1002, '网络错误');
  }
  ElMessage.error(e.message || '未知错误');
  return new DualRecordError(1000, e.message || '未知错误');
}

// ============================================================
// 通用方法
// ============================================================
export function get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
  return instance.get<ApiResponse<T>>(url, { params, ...config }).then((r) => r.data.data);
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return instance.post<ApiResponse<T>>(url, data, config).then((r) => r.data.data);
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return instance.put<ApiResponse<T>>(url, data, config).then((r) => r.data.data);
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return instance.delete<ApiResponse<T>>(url, config).then((r) => r.data.data);
}

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

export function download(url: string, filename?: string): Promise<void> {
  return instance
    .get(url, { responseType: 'blob' })
    .then((r) => {
      const blobUrl = window.URL.createObjectURL(r.data as Blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = filename || `download-${Date.now()}`;
      a.click();
      window.URL.revokeObjectURL(blobUrl);
    });
}

export default instance;
