"use server";

import { uploadCategoryThumbnail } from "../services/file-upload.service";

export async function uploadCategoryThumbnailAction(formData: FormData) {
  try {
    const file = formData.get("file") as File;
    if (!file) {
      return { error: "Không tìm thấy file ảnh để tải lên." };
    }
    const result = await uploadCategoryThumbnail(file);
    return { success: true, fileData: result };
  } catch (error: any) {
    console.error("Upload category thumbnail action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Tải ảnh lên AWS S3 thất bại. Vui lòng thử lại.",
    };
  }
}
