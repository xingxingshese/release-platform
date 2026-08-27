import { http } from "@/utils/http";

/** 后端统一响应外壳（com.company.release.common.response.ApiResponse） */
export interface ApiResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
}

/** POST /api/auth/login 返回（AuthService.LoginResult） */
export interface LoginResult {
  token: string;
  userId: number;
  username: string;
  permissions: Array<string>;
}

/** 登录（HTTP 层已解包 ApiResponse，直接返回 LoginResult） */
export const getLogin = (data?: { username: string; password: string }) => {
  return http.request<LoginResult>("post", "/api/auth/login", { data });
};
