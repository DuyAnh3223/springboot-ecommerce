// Use in Server Action / Route Handler

import axios from "axios";

export const api = axios.create({
  baseURL: process.env.SPRING_API_URL || process.env.NEXT_PUBLIC_SPRING_API_URL,
  timeout: 10000,
});

api.interceptors.request.use(async (config) => {
  const isAuthPath = config.url?.startsWith("/auth/");
  if (!isAuthPath && !config.headers.Authorization) {
    try {
      const { cookies } = await import("next/headers");
      const cookieStore = await cookies();
      const token = cookieStore.get("token")?.value;
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {}
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    switch (error.response?.status) {
      case 401:
        console.error("Unauthorized - Access token is invalid or expired");
        break;

      case 403:
        console.error(
          "Forbidden - You do not have permission to access this resource",
        );
        break;

      case 500:
        console.error(
          "Internal Server Error - Something went wrong on the server",
        );
        break;
    }

    return Promise.reject(error);
  },
);
