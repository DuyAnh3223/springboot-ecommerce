"use server";

import { uploadFile, deleteFile, AwsS3FileResponse } from "../services/file.service";

export async function uploadFileAction(
  formData: FormData,
): Promise<{ data?: AwsS3FileResponse; error?: string }> {
  try {
    const file = formData.get("file") as File;
    const folder = (formData.get("folder") as string) || "products";

    if (!file) {
      return { error: "Vui lòng chọn file cần tải lên" };
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
