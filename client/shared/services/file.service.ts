import { api } from "@/shared/http/api";

export interface AwsS3FileResponse {
  fileName: string;
  fileKey: string;
  fileUrl: string;
  contentType: string;
  size: number;
  isPublic: boolean;
}

export async function uploadFile(
  file: File,
  folder: string,
): Promise<AwsS3FileResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("folder", folder);

  const response = await api.post("/files/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });

  return response.data.result;
}

export async function deleteFile(fileKey: string): Promise<string> {
  const response = await api.delete(`/files`, {
    params: { key: fileKey },
  });
  return response.data.result;
}
