const Layout = () => import("@/layout/index.vue");

/** 项目与需求管理 */
export default [
  {
    path: "/project",
    name: "Project",
    component: Layout,
    redirect: "/project/list",
    meta: {
      icon: "ep/folder-opened",
      title: "项目管理",
      rank: 20
    },
    children: [
      {
        path: "/project/list",
        name: "ProjectList",
        component: () => import("@/views/project/index.vue"),
        meta: { title: "项目列表" }
      },
      {
        path: "/requirement/:projectId",
        name: "RequirementList",
        component: () => import("@/views/requirement/index.vue"),
        meta: { title: "需求管理", showLink: false, showParent: true }
      }
    ]
  }
] satisfies Array<RouteConfigsTable>;
