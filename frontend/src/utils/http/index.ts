import Axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type CustomParamsSerializer
} from "axios";
import type {
  PureHttpError,
  RequestMethods,
  PureHttpResponse,
  PureHttpRequestConfig
} from "./types.d";
import { stringify } from "qs";
import { getToken, formatToken } from "@/utils/auth";
import { useUserStoreHook } from "@/store/modules/user";
import { message } from "@/utils/message";

/** 后端统一响应外壳 */
interface BackendEnvelope<T = unknown> {
  code: string;
  message: string;
  data: T;
}

// 相关配置请参考：www.axios-js.com/zh-cn/docs/#axios-request-config-1
const defaultConfig: AxiosRequestConfig = {
  // 请求超时时间
  timeout: 15000,
  headers: {
    Accept: "application/json, text/plain, */*",
    "Content-Type": "application/json",
    "X-Requested-With": "XMLHttpRequest"
  },
  // 数组格式参数序列化（https://github.com/axios/axios/issues/5142）
  paramsSerializer: {
    serialize: stringify as unknown as CustomParamsSerializer
  }
};

class PureHttp {
  /** 防止重复登出跳转 */
  private static isLoggingOut = false;

  /** token 缺失或过期：直接登出回到登录页（后端无 refresh-token 端点） */
  private static handleUnauthorized() {
    if (PureHttp.isLoggingOut) return;
    PureHttp.isLoggingOut = true;
    message("登录已过期，请重新登录", { type: "warning" });
    useUserStoreHook().logOut();
    setTimeout(() => (PureHttp.isLoggingOut = false), 1000);
  }

  /** 保存当前`Axios`实例对象 */
  private static axiosInstance: AxiosInstance = Axios.create(defaultConfig);

  constructor() {
    this.httpInterceptorsRequest();
    this.httpInterceptorsResponse();
  }

  /** 请求拦截 */
  private httpInterceptorsRequest(): void {
    PureHttp.axiosInstance.interceptors.request.use(
      async (config: PureHttpRequestConfig): Promise<any> => {
        // 优先判断post/get等方法是否传入回调，否则执行初始化设置等回调
        if (typeof config.beforeRequestCallback === "function") {
          config.beforeRequestCallback(config);
          return config;
        }
        /** 请求白名单，放置一些不需要`token`的接口（通过设置请求白名单，防止`token`过期后再请求造成的死循环问题） */
        const whiteList = ["/api/auth/login"];
        return whiteList.some(url => config.url.endsWith(url))
          ? config
          : new Promise(resolve => {
              const data = getToken();
              if (data?.accessToken) {
                const now = new Date().getTime();
                const expired = parseInt(String(data.expires)) - now <= 0;
                if (expired) {
                  PureHttp.handleUnauthorized();
                  resolve(config);
                } else {
                  config.headers["Authorization"] = formatToken(
                    data.accessToken
                  );
                  resolve(config);
                }
              } else {
                resolve(config);
              }
            });
      },
      error => {
        return Promise.reject(error);
      }
    );
  }

  /** 响应拦截 */
  private httpInterceptorsResponse(): void {
    const instance = PureHttp.axiosInstance;
    instance.interceptors.response.use(
      async (response: PureHttpResponse) => {
        const $config = response.config;
        // 优先判断post/get等方法是否传入回调，否则执行初始化设置等回调
        if (typeof $config.beforeResponseCallback === "function") {
          $config.beforeResponseCallback(response);
          return response.data;
        }
        // 统一解包后端 ApiResponse{code,message,data}：code=OK 返回 data，否则报错
        const body = response.data as BackendEnvelope;
        if (
          body &&
          typeof body === "object" &&
          "code" in body &&
          "message" in body
        ) {
          if (body.code !== "OK") {
            message(body.message || "请求失败", { type: "error" });
            return Promise.reject(new Error(body.message || body.code));
          }
          return body.data as any;
        }
        return response.data;
      },
      (error: PureHttpError) => {
        const $error = error;
        $error.isCancelRequest = Axios.isCancel($error);
        const envelope = $error.response?.data as BackendEnvelope | undefined;
        const status = $error.response?.status;
        if (
          (status === 401 || status === 403) &&
          envelope?.code !== "PERMISSION_DENIED"
        ) {
          // 未登录 / token 过期（后端无自定义 EntryPoint 时未认证也返回 403）
          PureHttp.handleUnauthorized();
        } else if (!$error.isCancelRequest) {
          const msg =
            envelope?.message ?? $error.message ?? "网络异常，请稍后重试";
          message(msg, { type: "error" });
        }
        // 所有的响应异常 区分来源为取消请求/非取消请求
        return Promise.reject($error);
      }
    );
  }

  /** 通用请求工具函数 */
  public request<T>(
    method: RequestMethods,
    url: string,
    param?: AxiosRequestConfig,
    axiosConfig?: PureHttpRequestConfig
  ): Promise<T> {
    const config = {
      method,
      url,
      ...param,
      ...axiosConfig
    } as PureHttpRequestConfig;

    // 单独处理自定义请求/响应回调
    return new Promise((resolve, reject) => {
      PureHttp.axiosInstance
        .request(config)
        .then((response: undefined) => {
          resolve(response);
        })
        .catch(error => {
          reject(error);
        });
    });
  }

  /** 单独抽离的`post`工具函数 */
  public post<T, P>(
    url: string,
    params?: AxiosRequestConfig<P>,
    config?: PureHttpRequestConfig
  ): Promise<T> {
    return this.request<T>("post", url, params, config);
  }

  /** 单独抽离的`get`工具函数 */
  public get<T, P>(
    url: string,
    params?: AxiosRequestConfig<P>,
    config?: PureHttpRequestConfig
  ): Promise<T> {
    return this.request<T>("get", url, params, config);
  }
}

export const http = new PureHttp();
