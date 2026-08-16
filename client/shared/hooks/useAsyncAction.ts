import { useCallback, useState } from "react";

export function useAsyncAction() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(async <T>(fn: () => Promise<T>): Promise<T | null> => {
    setError(null);
    setIsLoading(true);
    try {
      return await fn();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Có lỗi xảy ra.";
      setError(message);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { isLoading, error, setError, run };
}
