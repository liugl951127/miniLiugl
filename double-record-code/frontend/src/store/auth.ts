/**
 * 认证状态管理(Pinia)
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { login as loginApi, getCurrentUser, type LoginRequest, type LoginResponse } from '@/api/auth';

export const useAuthStore = defineStore('auth', () => {
  // ============================================================
  // 状态
  // ============================================================
  const token = ref<string>(localStorage.getItem('auth_token') || '');
  const refreshToken = ref<string>(localStorage.getItem('refresh_token') || '');
  const user = ref<LoginResponse['user'] | null>(null);
  const expiresAt = ref<number>(0);
  const loading = ref(false);

  // ============================================================
  // 计算
  // ============================================================
  const isLoggedIn = computed(() => !!token.value && !isExpired.value);
  const isExpired = computed(() => expiresAt.value > 0 && Date.now() >= expiresAt.value);
  const userName = computed(() => user.value?.name || '');
  const userRole = computed(() => user.value?.roleCode || '');
  const permissions = computed(() => user.value?.permissions || []);
  const hasPermission = (perm: string) => permissions.value.includes(perm);

  // ============================================================
  // 操作
  // ============================================================

  /**
   * 登录
   */
  async function login(req: LoginRequest) {
    loading.value = true;
    try {
      const res = await loginApi(req);
      setAuth(res);
      return res;
    } finally {
      loading.value = false;
    }
  }

  /**
   * 设置认证信息
   */
  function setAuth(data: LoginResponse) {
    token.value = data.token;
    refreshToken.value = data.refreshToken;
    user.value = data.user;
    expiresAt.value = Date.now() + data.expiresIn * 1000;

    localStorage.setItem('auth_token', data.token);
    localStorage.setItem('refresh_token', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(data.user));
  }

  /**
   * 退出登录
   */
  async function logout() {
    try {
      // 调用后端 logout
    } catch (e) {
      // 忽略错误
    } finally {
      clearAuth();
    }
  }

  /**
   * 清除认证信息
   */
  function clearAuth() {
    token.value = '';
    refreshToken.value = '';
    user.value = null;
    expiresAt.value = 0;

    localStorage.removeItem('auth_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user');
  }

  /**
   * 加载本地存储的用户信息
   */
  function loadLocalUser() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        user.value = JSON.parse(userStr);
      } catch {
        // ignore
      }
    }
  }

  /**
   * 刷新当前用户信息
   */
  async function refreshUser() {
    if (!token.value) return;
    try {
      const u = await getCurrentUser();
      user.value = u;
      localStorage.setItem('user', JSON.stringify(u));
    } catch (e) {
      // token 失效
      clearAuth();
    }
  }

  // 初始化
  loadLocalUser();

  return {
    token,
    refreshToken,
    user,
    expiresAt,
    loading,
    isLoggedIn,
    isExpired,
    userName,
    userRole,
    permissions,
    hasPermission,
    login,
    logout,
    clearAuth,
    refreshUser,
    setAuth,
  };
});
