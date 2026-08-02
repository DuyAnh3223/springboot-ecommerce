export function slugify(str: string, separator: "-" | "_" = "-"): string {
  if (!str) return "";
  const normalized = str
    .trim()
    .toLowerCase()
    .replace(/[đĐ]/g, "d")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "") // Remove accents
    .replace(/[^a-z0-9\s_-]/g, ""); // Remove other special characters except space, dash, underscore

  if (separator === "_") {
    return normalized
      .replace(/[\s\-\.]+/g, "_")
      .replace(/_+/g, "_")
      .replace(/^_+|_+$/g, "");
  } else {
    return normalized
      .replace(/[\s\-\._]+/g, "-")
      .replace(/-+/g, "-")
      .replace(/^-+|-+$/g, "");
  }
}
