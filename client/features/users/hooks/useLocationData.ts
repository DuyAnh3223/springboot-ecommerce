import { useState, useEffect, useCallback } from "react";

export interface ProvinceItem {
    code: number;
    name: string;
}

export interface WardItem {
    code: number;
    name: string;
}

export function useLocationData(isOpen: boolean, initialProvince?: string) {
    const [provinces, setProvinces] = useState<ProvinceItem[]>([]);
    const [wards, setWards] = useState<WardItem[]>([]);
    const [selectedProvinceCode, setSelectedProvinceCode] = useState<number | null>(null);
    const [isLoadingProvinces, setIsLoadingProvinces] = useState(false);
    const [isLoadingWards, setIsLoadingWards] = useState(false);

    // Fetch provinces list when dialog opens
    useEffect(() => {
        if (!isOpen) return;

        let isMounted = true;
        setIsLoadingProvinces(true);

        fetch("https://provinces.open-api.vn/api/v1/p/")
            .then((res) => res.json())
            .then((data: ProvinceItem[]) => {
                if (!isMounted) return;
                setProvinces(data || []);
            })
            .catch((err) => console.error("Lỗi khi tải danh sách Tỉnh/Thành:", err))
            .finally(() => {
                if (isMounted) setIsLoadingProvinces(false);
            });

        return () => {
            isMounted = false;
        };
    }, [isOpen]);

    // Fetch wards when a province code is selected
    const fetchWardsByProvinceCode = useCallback(async (code: number) => {
        setIsLoadingWards(true);
        try {
            const res = await fetch(`https://provinces.open-api.vn/api/v1/p/${code}?depth=3`);
            const data = await res.json();
            if (data && Array.isArray(data.districts)) {
                const extractedWards: WardItem[] = data.districts.flatMap(
                    (d: { wards?: WardItem[] }) => d.wards || []
                );
                setWards(extractedWards);
            } else {
                setWards([]);
            }
        } catch (err) {
            console.error("Lỗi khi tải danh sách Phường/Xã:", err);
            setWards([]);
        } finally {
            setIsLoadingWards(false);
        }
    }, []);

    const handleProvinceSelect = useCallback(
        (provinceName: string) => {
            if (!provinceName) {
                setSelectedProvinceCode(null);
                setWards([]);
                return;
            }

            const matched = provinces.find((p) => p.name === provinceName);
            if (matched) {
                setSelectedProvinceCode(matched.code);
                fetchWardsByProvinceCode(matched.code);
            } else {
                setSelectedProvinceCode(null);
                setWards([]);
            }
        },
        [provinces, fetchWardsByProvinceCode]
    );

    // Auto-match and load wards for editing address
    useEffect(() => {
        if (!isOpen || provinces.length === 0 || !initialProvince) return;

        const matched = provinces.find(
            (p) => p.name === initialProvince || p.name.includes(initialProvince) || initialProvince.includes(p.name)
        );

        if (matched) {
            setSelectedProvinceCode(matched.code);
            fetchWardsByProvinceCode(matched.code);
        }
    }, [isOpen, provinces, initialProvince, fetchWardsByProvinceCode]);

    return {
        provinces,
        wards,
        selectedProvinceCode,
        isLoadingProvinces,
        isLoadingWards,
        handleProvinceSelect,
    };
}
