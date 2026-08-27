/**
 * 本平台仅使用前端静态路由（src/router/modules/*），
 * 后端无动态路由端点，返回空列表以保持 pure-admin 初始化流程兼容。
 */
type Result = {
  success: boolean;
  data: Array<any>;
};

export const getAsyncRoutes = (): Promise<Result> => {
  return Promise.resolve({ success: true, data: [] });
};
