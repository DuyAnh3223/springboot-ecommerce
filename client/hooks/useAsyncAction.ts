import { useState } from "react";

export function useAsyncAction() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = async <T>(fn: () => Promise<T>): Promise<T | null> => {
    setError(null);
    setIsLoading(true);
    try {
      return await fn();
    } catch (err: any) {
      setError(err.message || "Có lỗi xảy ra.");
      return null;
    } finally {
      setIsLoading(false);
    }
  };

  return { isLoading, error, setError, run };
}
