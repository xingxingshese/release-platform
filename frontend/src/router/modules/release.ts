const Layout = () => import("@/layout/index.vue");

/** 发布管理：发布计划列表 + 详情 Timeline（核心链路） */
export default [
  {
    path: "/release",
    name: "Release",
    component: Layout,
    redirect: "/release/list",
    meta: {
      icon: "ep/promotion",
      title: "发布管理",
      rank: 10
    },
    children: [
      {
        path: "/release/list",
        name: "ReleaseList",
        component: () => import("@/views/release/list.vue"),
        meta: { title: "发布计划" }
      },
      {
        path: "/release/detail/:id",
        name: "ReleaseDetail",
        component: () => import("@/views/release/detail.vue"),
        meta: { title: "发布详情", showLink: false, showParent: true }
      }
    ]
  }
] satisfies Array<RouteConfigsTable>;
