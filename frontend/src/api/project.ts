import { http } from "@/utils/http";
import type {
  ApiResponse,
  Project,
  ProjectMember,
  ProjectService,
  ProjectType
} from "./types";

/** POST /api/projects */
export const createProject = (data: {
  code: string;
  name: string;
  description?: string;
  projectType: ProjectType;
}) => {
  return http.request<ApiResponse<Project>>("post", "/api/projects", { data });
};

/** GET /api/projects */
export const listProjects = () => {
  return http.request<Project[]>("get", "/api/projects");
};

/** GET /api/projects/{id} */
export const getProject = (id: number) => {
  return http.request<Project>("get", `/api/projects/${id}`);
};

/** POST /api/projects/{id}/members */
export const addProjectMember = (
  id: number,
  data: { userId: number; role: string }
) => {
  return http.request<void>("post", `/api/projects/${id}/members`, { data });
};

/** GET /api/projects/{id}/members */
export const listProjectMembers = (id: number) => {
  return http.request<ProjectMember[]>(`get`, `/api/projects/${id}/members`);
};

/** POST /api/projects/{id}/services */
export const addProjectService = (
  id: number,
  data: { code: string; name: string; type: string }
) => {
  return http.request<void>("post", `/api/projects/${id}/services`, { data });
};

/** GET /api/projects/{id}/services */
export const listProjectServices = (id: number) => {
  return http.request<ProjectService[]>("get", `/api/projects/${id}/services`);
};
