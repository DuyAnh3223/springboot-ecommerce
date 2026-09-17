"use client";

/* eslint-disable react-hooks/set-state-in-effect -- network loading state is synchronized by effects. */

import { useCallback, useEffect, useState } from "react";
import {
  getShippingDistricts,
  getShippingProvinces,
  getShippingWards,
} from "../api/shipping-location.api";
import type {
  ShippingDistrict,
  ShippingProvince,
  ShippingWard,
} from "../shipping-location.type";

interface UseShippingLocationsOptions {
  enabled?: boolean;
  provinceId?: number | null;
  districtId?: number | null;
}

export function useShippingLocations({
  enabled = true,
  provinceId,
  districtId,
}: UseShippingLocationsOptions = {}) {
  const [provinces, setProvinces] = useState<ShippingProvince[]>([]);
  const [districts, setDistricts] = useState<ShippingDistrict[]>([]);
  const [wards, setWards] = useState<ShippingWard[]>([]);
  const [loadingProvinces, setLoadingProvinces] = useState(false);
  const [loadingDistricts, setLoadingDistricts] = useState(false);
  const [loadingWards, setLoadingWards] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const retry = useCallback(() => setRetryKey((value) => value + 1), []);

  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController();
    setLoadingProvinces(true);
    setError(null);
    getShippingProvinces(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setProvinces(data);
      })
      .catch((reason: unknown) => {
        if (!controller.signal.aborted) {
          setProvinces([]);
          setError(reason instanceof Error ? reason.message : "Không thể tải tỉnh/thành phố.");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingProvinces(false);
      });
    return () => controller.abort();
  }, [enabled, retryKey]);

  useEffect(() => {
    setDistricts([]);
    setWards([]);
    if (!enabled || !provinceId || provinceId <= 0) {
      setLoadingDistricts(false);
      return;
    }
    const controller = new AbortController();
    setLoadingDistricts(true);
    setError(null);
    getShippingDistricts(provinceId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setDistricts(data);
      })
      .catch((reason: unknown) => {
        if (!controller.signal.aborted) {
          setDistricts([]);
          setError(reason instanceof Error ? reason.message : "Không thể tải quận/huyện.");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingDistricts(false);
      });
    return () => controller.abort();
  }, [enabled, provinceId, retryKey]);

  useEffect(() => {
    setWards([]);
    if (!enabled || !districtId || districtId <= 0) {
      setLoadingWards(false);
      return;
    }
    const controller = new AbortController();
    setLoadingWards(true);
    setError(null);
    getShippingWards(districtId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setWards(data);
      })
      .catch((reason: unknown) => {
        if (!controller.signal.aborted) {
          setWards([]);
          setError(reason instanceof Error ? reason.message : "Không thể tải phường/xã.");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingWards(false);
      });
    return () => controller.abort();
  }, [enabled, districtId, retryKey]);

  return {
    provinces,
    districts,
    wards,
    loadingProvinces,
    loadingDistricts,
    loadingWards,
    error,
    retry,
  };
}
