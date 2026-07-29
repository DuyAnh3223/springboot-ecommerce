import { api } from "@/lib/axios";

export interface AwsS3FileResponse {
  fileName: string;
  fileKey: string;
  fileUrl: string;
  contentType: string;
  size: number;
  isPublic: boolean;
}

export async function uploadCategoryThumbnail(
  file: File,
): Promise<AwsS3FileResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("folder", "categories");

  const response = await api.post("/files/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });

  return response.data.result;
}
