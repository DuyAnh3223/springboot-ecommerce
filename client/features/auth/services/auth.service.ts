import { api } from "@/lib/axios";
import { SignInInput, SignUpInput } from "../schemas/auth.schema";
import { PassThrough } from "stream";

export async function signIn(values: SignInInput) {
  const response = await api.post("/auth/sign-in", {
    username: values.username,
    password: values.password,
  });
  if (response.data.code !== 1000) {
    throw new Error(response.data.message);
  }
  return response.data.result;
}

export async function signUp(values: SignUpInput) {
  const response = await api.post("/users", {
    username: values.username,
    password: values.password,
    confirmPassword: values.confirmPassword,
    firstName: values.firstName,
    lastName: values.lastName,
  });

  if (response.data.code !== 1000) {
    throw new Error(response.data.message);
  }
  return response.data.result;
}

export async function signOut(token: string) {
  return api.post("/auth/sign-out", {
    token,
  });
}

export async function getCurrentUser(token: string) {
  const response = await api.get("/users/my-info", {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (response.data.code !== 1000) {
    throw new Error(response.data.message);
  }
  return response.data.result;
}
