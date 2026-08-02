"use server";

import { uploadFile, deleteFile, AwsS3FileResponse } from "../services/file.service";

export async function uploadFileAction(
  formData: FormData,
): Promise<{ data?: AwsS3FileResponse; error?: string }> {
  try {
    const file = formData.get("file");
    const folder = formData.get("folder");

    if (!(file instanceof File)) {
      return { error: "Vui lòng chọn file cần tải lên" };
    }

    if (typeof folder !== "string" || !folder.trim()) {
      return { error: "Vui lòng cung cấp thư mục tải file" };
    }

    const res = await uploadFile(file, folder);
    return { data: res };
  } catch (err: any) {
    console.error("Lỗi khi tải file lên S3:", err);
    return {
      error:
        err.response?.data?.message ||
        "Tải file lên hệ thống thất bại. Vui lòng thử lại.",
    };
  }
}

export async function deleteFileAction(
  fileKey: string,
): Promise<{ success?: boolean; error?: string }> {
  try {
    if (!fileKey) return { success: true };
    await deleteFile(fileKey);
    return { success: true };
  } catch (err: any) {
    console.error("Lỗi khi xóa file S3:", err);
    return {
      error:
        err.response?.data?.message ||
        "Xóa file trên S3 thất bại.",
    };
  }
}
