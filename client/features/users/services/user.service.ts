import { api } from "@/lib/axios";
import { UserResponse, GetUsersParams, UserUpdateRequest } from "../user.type";

import { PageResponse } from "@/types/page.type";

export async function getUser(userId: string): Promise<UserResponse> {
  const response = await api.get(`/users/${userId}`);
  return response.data.result;
}

export async function getUsers(
  params?: GetUsersParams,
): Promise<PageResponse<UserResponse>> {
  const response = await api.get(`/users`, { params });
  return response.data.result;
}

export async function updateUser(
  userId: string,
  values: UserUpdateRequest,
): Promise<UserResponse> {
  const response = await api.patch(`/users/${userId}`, values);
  return response.data.result;
}

export async function deleteUser(userId: string): Promise<void> {
  await api.delete(`/users/${userId}`);
}
