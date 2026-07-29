import { useState } from "react";

export function useTagInput(initial: string[] = []) {
  const [tags, setTags] = useState<string[]>(initial);
  const [tagInput, setTagInput] = useState("");

  const handleTagAdd = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      const cleaned = tagInput.trim();
      if (cleaned && !tags.includes(cleaned)) {
        setTags((prev) => [...prev, cleaned]);
      }
      setTagInput("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags((prev) => prev.filter((t) => t !== tagToRemove));
  };

  const reset = () => {
    setTags(initial);
    setTagInput("");
  };

  return { tags, tagInput, setTags, setTagInput, handleTagAdd, handleRemoveTag, reset };
}
