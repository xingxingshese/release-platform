import { defineStore } from "pinia";
import {
  type userType,
  store,
  router,
  resetRouter,
  routerArrays,
  storageLocal
} from "../utils";
import { type LoginResult, getLogin } from "@/api/user";
import { useMultiTagsStoreHook } from "./multiTags";
import {
  type DataInfo,
  setToken,
  removeToken,
  userKey,
  deriveRoles
} from "@/utils/auth";

/** 后端 JWT TTL 默认 8 小时（security.jwt.ttl:PT8H），预留 5 分钟缓冲 */
const TOKEN_TTL_MS = 8 * 3600 * 1000 - 5 * 60 * 1000;

export const useUserStore = defineStore("pure-user", {
  state: (): userType => ({
    // 头像
    avatar: storageLocal().getItem<DataInfo<number>>(userKey)?.avatar ?? "",
    // 用户名
    username: storageLocal().getItem<DataInfo<number>>(userKey)?.username ?? "",
    // 昵称
    nickname: storageLocal().getItem<DataInfo<number>>(userKey)?.nickname ?? "",
    // 用户ID
    userId: storageLocal().getItem<DataInfo<number>>(userKey)?.userId ?? 0,
    // 页面级别权限
    roles: storageLocal().getItem<DataInfo<number>>(userKey)?.roles ?? [],
    // 按钮级别权限
    permissions:
      storageLocal().getItem<DataInfo<number>>(userKey)?.permissions ?? [],
    // 是否勾选了登录页的免登录
    isRemembered: false,
    // 登录页的免登录存储几天，默认7天
    loginDay: 7
  }),
  actions: {
    /** 存储头像 */
    SET_AVATAR(avatar: string) {
      this.avatar = avatar;
    },
    /** 存储用户名 */
    SET_USERNAME(username: string) {
      this.username = username;
    },
    /** 存储昵称 */
    SET_NICKNAME(nickname: string) {
      this.nickname = nickname;
    },
    /** 存储用户ID */
    SET_USERID(userId: number) {
      this.userId = userId;
    },
    /** 存储角色 */
    SET_ROLES(roles: Array<string>) {
      this.roles = roles;
    },
    /** 存储按钮级别权限 */
    SET_PERMS(permissions: Array<string>) {
      this.permissions = permissions;
    },
    /** 存储是否勾选了登录页的免登录 */
    SET_ISREMEMBERED(bool: boolean) {
      this.isRemembered = bool;
    },
    /** 设置登录页的免登录存储几天 */
    SET_LOGINDAY(value: number) {
      this.loginDay = Number(value);
    },
    /** 登入（对接 POST /api/auth/login，HTTP 层已解包 ApiResponse） */
    async loginByUsername(data: { username: string; password: string }) {
      return new Promise<LoginResult & { success: boolean }>(
        (resolve, reject) => {
          getLogin(data)
            .then(res => {
              if (res?.token) {
                setToken({
                  accessToken: res.token,
                  expires: Date.now() + TOKEN_TTL_MS,
                  username: res.username,
                  nickname: res.username,
                  userId: res.userId,
                  roles: deriveRoles(res.permissions),
                  permissions: res.permissions
                } as DataInfo<number>);
                resolve({ ...res, success: true });
              } else {
                resolve({ success: false } as LoginResult & {
                  success: boolean;
                });
              }
            })
            .catch(error => {
              reject(error);
            });
        }
      );
    },
    /** 前端登出（后端无登出端点，JWT 无状态） */
    logOut() {
      this.username = "";
      this.userId = 0;
      this.roles = [];
      this.permissions = [];
      removeToken();
      useMultiTagsStoreHook().handleTags("equal", [...routerArrays]);
      resetRouter();
      router.push("/login");
    }
  }
});

export function useUserStoreHook() {
  return useUserStore(store);
}
