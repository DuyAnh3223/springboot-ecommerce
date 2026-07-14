export interface RoleResponse {
  id: string;
  name: string;
  description?: string;
  scope?: string;
}

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  active: boolean;
  createdAt: string;
  roles?: RoleResponse[];
}

export interface GetUsersParams {
  search?: string;
  isActive?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
}

export interface UserUpdateRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  email: string;
  password?: string;
  roles: string[];
}
