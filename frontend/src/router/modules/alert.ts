const Layout = () => import("@/layout/index.vue");

/** 统一报警中心：列表 / ACK / 恢复 / 升级状态 */
export default [
  {
    path: "/alert",
    name: "Alert",
    component: Layout,
    redirect: "/alert/list",
    meta: {
      icon: "ep/bell",
      title: "报警中心",
      rank: 30
    },
    children: [
      {
        path: "/alert/list",
        name: "AlertList",
        component: () => import("@/views/alert/index.vue"),
        meta: { title: "报警列表" }
      }
    ]
  }
] satisfies Array<RouteConfigsTable>;
