import { useEffect, useState, useRef, ChangeEvent } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { categorySchema, CategoryFormValues } from "../schemas/category.schema";
import { CategoryResponse } from "../category.type";
import {
  createCategoryAction,
  updateCategoryAction,
  uploadCategoryThumbnailAction,
} from "../actions";
import { slugify } from "@/lib/slugify";

interface UseCategoryFormDialogOptions {
  category: CategoryResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];

export function useCategoryFormDialog({
  category,
  open,
  onOpenChange,
  onSuccess,
}: UseCategoryFormDialogOptions) {
  const isEdit = !!category;
  const [error, setError] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [removeImage, setRemoveImage] = useState<boolean>(false);
  const [previewUrl, setPreviewUrl] = useState<string>("");
  const [uploadMode, setUploadMode] = useState<"file" | "url">("file");

  const blobUrlRef = useRef<string | null>(null);

  const revokePreviewBlob = () => {
    if (blobUrlRef.current && blobUrlRef.current.startsWith("blob:")) {
      URL.revokeObjectURL(blobUrlRef.current);
      blobUrlRef.current = null;
    }
  };

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: { name: "", slug: "", thumbnail: "" },
  });

  const nameValue = watch("name");
  const thumbnailValue = watch("thumbnail");

  // Auto-generate slug from name using shared slugify util (only in create mode)
  useEffect(() => {
    if (!isEdit && nameValue) {
      const slug = slugify(nameValue);
      setValue("slug", slug, { shouldValidate: false });
    }
  }, [nameValue, isEdit, setValue]);

  // Populate form and handle blob cleanup when category or open status changes
  useEffect(() => {
    revokePreviewBlob();
    setSelectedFile(null);
    setRemoveImage(false);
    setError(null);
    setUploadError(null);

    if (category) {
      const existingThumbnail = category.thumbnail || "";
      reset({
        name: category.name,
        slug: category.slug,
        thumbnail: existingThumbnail,
      });
      setPreviewUrl(existingThumbnail);
    } else {
      reset({ name: "", slug: "", thumbnail: "" });
      setPreviewUrl("");
    }
  }, [category, open, reset]);

  // Unmount cleanup for Blob URL
  useEffect(() => {
    return () => {
      revokePreviewBlob();
    };
  }, []);

  // Sync previewUrl when manual thumbnail input changes in URL mode
  useEffect(() => {
    if (uploadMode === "url") {
      setPreviewUrl(thumbnailValue || "");
    }
  }, [thumbnailValue, uploadMode]);

  const handleModeChange = (mode: "file" | "url") => {
    setUploadMode(mode);
    setUploadError(null);

    if (mode === "url") {
      revokePreviewBlob();
      setSelectedFile(null);
      setPreviewUrl(thumbnailValue || "");
    } else {
      if (selectedFile && blobUrlRef.current) {
        setPreviewUrl(blobUrlRef.current);
      } else {
        const initialThumbnail = category?.thumbnail || "";
        setPreviewUrl(removeImage ? "" : initialThumbnail);
      }
    }
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadError(null);

    if (file.size > MAX_FILE_SIZE) {
      setUploadError("Dung lượng file tối đa là 5MB. Vui lòng chọn ảnh nhỏ hơn.");
      e.target.value = "";
      return;
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      setUploadError("Định dạng file không được hỗ trợ (chỉ nhận JPG, PNG, WEBP, GIF).");
      e.target.value = "";
      return;
    }

    revokePreviewBlob();

    const localBlobUrl = URL.createObjectURL(file);
    blobUrlRef.current = localBlobUrl;

    setSelectedFile(file);
    setPreviewUrl(localBlobUrl);
    setRemoveImage(false);

    e.target.value = "";
  };

  const handleRemoveImage = () => {
    revokePreviewBlob();
    setSelectedFile(null);
    setPreviewUrl("");
    setValue("thumbnail", "");
    setRemoveImage(true);
    setUploadError(null);
  };

  const onSubmit = async (values: CategoryFormValues) => {
    setError(null);
    let resolvedThumbnail: string | undefined;

    if (uploadMode === "file" && selectedFile) {
      try {
        const formData = new FormData();
        formData.append("file", selectedFile);

        const uploadResult = await uploadCategoryThumbnailAction(formData);

        if (uploadResult.error || !uploadResult.fileData) {
          setError(uploadResult.error || "Tải ảnh lên AWS S3 thất bại.");
          return;
        }

        resolvedThumbnail = uploadResult.fileData.fileUrl;
      } catch (err: any) {
        setError("Có lỗi xảy ra khi tải ảnh lên AWS S3.");
        return;
      }
    } else if (removeImage) {
      resolvedThumbnail = undefined;
    } else if (uploadMode === "url") {
      resolvedThumbnail = values.thumbnail || undefined;
    } else {
      resolvedThumbnail = category?.thumbnail || undefined;
    }

    const payload = {
      name: values.name,
      slug: values.slug,
      thumbnail: resolvedThumbnail,
    };

    const result = isEdit
      ? await updateCategoryAction(category!.id, payload)
      : await createCategoryAction(payload);

    if (result.error) {
      setError(result.error);
      return;
    }

    revokePreviewBlob();
    onSuccess();
    onOpenChange(false);
  };

  return {
    isEdit,
    error,
    uploadError,
    previewUrl,
    uploadMode,
    register,
    errors,
    isSubmitting,
    handleModeChange,
    handleFileChange,
    handleRemoveImage,
    handleSubmit: handleSubmit(onSubmit),
  };
}
