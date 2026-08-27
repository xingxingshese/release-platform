const Layout = () => import("@/layout/index.vue");

/** 管理员配置中心：配置版本 + 字段级 diff */
export default [
  {
    path: "/admin",
    name: "Admin",
    component: Layout,
    redirect: "/admin/config",
    meta: {
      icon: "ep/setting",
      title: "管理员配置",
      rank: 40
    },
    children: [
      {
        path: "/admin/config",
        name: "AdminConfig",
        component: () => import("@/views/admin/config.vue"),
        meta: { title: "配置中心" }
      }
    ]
  }
] satisfies Array<RouteConfigsTable>;
