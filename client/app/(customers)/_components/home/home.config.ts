import { HeroSlide, SubBanner, TrustBadge, FlashSaleItem, BrandItem, NewsCard } from "./home.types";

export const HERO_SLIDES: HeroSlide[] = [
  {
    id: "slide-1",
    title: "VGA NVIDIA RTX 40 SERIES",
    subtitle: "Chinh Phục Mọi Tựa Game 4K Với DLSS 3.5 & Ray Tracing Đỉnh Cao",
    badge: "🔥 SIÊU PHẨM MỚI VỀ",
    ctaText: "Khám Phá Ngay",
    ctaLink: "/category/vga",
    image: "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=1200&q=80",
    highlightText: "Giảm tới 3.000.000đ khi mua kèm Mainboard",
  },
  {
    id: "slide-2",
    title: "CPU INTEL CORE ULTRA & AMD RYZEN 9000",
    subtitle: "Bứt Phá Hiệu Năng Xử Lý AI, Đồ Họa Nặng & Đa Nhiệm Mượt Mà",
    badge: "⚡ HIỆU NĂNG VƯỢT TRỘI",
    ctaText: "Xem Chi Tiết",
    ctaLink: "/category/cpu",
    image: "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?auto=format&fit=crop&w=1200&q=80",
    highlightText: "Tặng Tản Nhiệt Khí Khai Xuân 2026",
  },
  {
    id: "slide-3",
    title: "PC GAMING CUSTOM HI-TECH",
    subtitle: "Lắp Đặt Chuẩn Chuyên Gia - Tự Động Kiểm Tra Tương Thích Linh Kiện",
    badge: "🛠️ LẮP RÁP MIỄN PHÍ",
    ctaText: "Xây Cấu Hình",
    ctaLink: "#pc-builder",
    image: "https://images.unsplash.com/photo-1555680202-c86f0e12f086?auto=format&fit=crop&w=1200&q=80",
    highlightText: "Miễn Phí Giao Hàng & Bảo Hành 1 Đổi 1 Tại Nhà",
  },
];

export const SUB_BANNERS: SubBanner[] = [
  {
    id: "sub-1",
    title: "BO MẠCH CHỦ Z790 / X670",
    subtitle: "Hỗ trợ RAM DDR5 & PCIe 5.0 Siêu Tốc",
    badge: "GIẢM 20%",
    ctaLink: "/category/motherboard",
    image: "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=600&q=80",
    bgGradient: "from-slate-900 to-indigo-950",
  },
  {
    id: "sub-2",
    title: "RAM DDR5 & SSD NVME GEN4",
    subtitle: "Tốc độ đọc ghi lên tới 7400MB/s",
    badge: "HOT DEAL",
    ctaLink: "/category/ram",
    image: "https://images.unsplash.com/photo-1562976540-1502c2145186?auto=format&fit=crop&w=600&q=80",
    bgGradient: "from-slate-900 to-rose-950",
  },
];

export const TRUST_BADGES: TrustBadge[] = [
  {
    id: "trust-1",
    iconName: "truck",
    title: "Giao Hàng Siêu Tốc 2H",
    description: "Nội thành TP.HCM & Hà Nội, kiểm tra hàng trước khi thanh toán",
  },
  {
    id: "trust-2",
    iconName: "shield",
    title: "Cam Kết Chính Hãng 100%",
    description: "Đền tiền gấp 2 lần nếu phát hiện hàng giả, bảo hành 1 đổi 1",
  },
  {
    id: "trust-3",
    iconName: "creditCard",
    title: "Trả Góp 0% Lãi Suất",
    description: "Duyệt hồ sơ online chỉ trong 5 phút qua thẻ tín dụng hoặc CCCD",
  },
  {
    id: "trust-4",
    iconName: "headphones",
    title: "Hỗ Trợ Kỹ Thuật 24/7",
    description: "Tư vấn lắp đặt, cài đặt phần mềm & vệ sinh máy tính miễn phí",
  },
];

export const FLASH_SALE_CAMPAIGN: FlashSaleItem[] = [
  {
    id: 101,
    productSlug: "vga",
    name: "Card Màn Hình ASUS ROG Strix GeForce RTX 4070 Ti Super 16GB",
    categoryName: "Card màn hình (VGA)",
    originalPrice: 26990000,
    salePrice: 22490000,
    discountPercent: 16,
    soldCount: 14,
    totalStock: 20,
    thumbnail: "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=500&q=80",
  },
  {
    id: 102,
    productSlug: "cpu",
    name: "Bộ Vi Xử Lý Intel Core i7-14700K (Up To 5.6GHz, 20 Nhân 28 Luồng)",
    categoryName: "Vi xử lý (CPU)",
    originalPrice: 11590000,
    salePrice: 9490000,
    discountPercent: 18,
    soldCount: 22,
    totalStock: 30,
    thumbnail: "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?auto=format&fit=crop&w=500&q=80",
  },
  {
    id: 103,
    productSlug: "ram",
    name: "Bộ Nhớ RAM Corsair Vengeance RGB 32GB (2x16GB) DDR5 6000MHz",
    categoryName: "Bộ nhớ RAM",
    originalPrice: 3890000,
    salePrice: 2990000,
    discountPercent: 23,
    soldCount: 38,
    totalStock: 50,
    thumbnail: "https://images.unsplash.com/photo-1562976540-1502c2145186?auto=format&fit=crop&w=500&q=80",
  },
  {
    id: 104,
    productSlug: "storage",
    name: "Ổ Cứng SSD Samsung 990 PRO 1TB M.2 NVMe PCIe Gen 4.0",
    categoryName: "Ổ cứng (SSD/HDD)",
    originalPrice: 3490000,
    salePrice: 2690000,
    discountPercent: 22,
    soldCount: 45,
    totalStock: 60,
    thumbnail: "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?auto=format&fit=crop&w=500&q=80",
  },
];

export const BRANDS_LIST: BrandItem[] = [
  { name: "ASUS ROG", slug: "asus" },
  { name: "MSI Gaming", slug: "msi" },
  { name: "GIGABYTE", slug: "gigabyte" },
  { name: "INTEL", slug: "intel" },
  { name: "AMD", slug: "amd" },
  { name: "CORSAIR", slug: "corsair" },
  { name: "KINGSTON", slug: "kingston" },
  { name: "NZXT", slug: "nzxt" },
];

export const TECH_NEWS: NewsCard[] = [
  {
    id: "news-1",
    title: "Hướng Dẫn Xây Dựng PC Gaming 15 Triệu Chiến Tốt Mọi Game Esport 2026",
    excerpt: "Tổng hợp cấu hình PC gaming tối ưu chi phí nhất trong phân khúc 15 triệu đồng với CPU Intel Gen 13 và VGA RTX 3060...",
    date: "01/08/2026",
    category: "Tư Vấn Cấu Hình",
    image: "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=600&q=80",
    readTime: "5 phút đọc",
  },
  {
    id: "news-2",
    title: "Đánh Giá Chi Tiết CPU Intel Core i7-14700K: Xử Lý AI & Đồ Họa Đột Phá",
    excerpt: "Liệu Intel Core i7-14700K có thực sự xứng đáng là vị vương trong phân khúc vi xử lý cận cao cấp năm nay?...",
    date: "28/07/2026",
    category: "Review Linh Kiện",
    image: "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?auto=format&fit=crop&w=600&q=80",
    readTime: "7 phút đọc",
  },
  {
    id: "news-3",
    title: "So Sánh Tản Nhiệt Nước AIO Và Tản Nhiệt Khí: Nên Chọn Loại Nào Cho CPU?",
    excerpt: "Phân tích chi tiết ưu nhược điểm, hiệu năng tản nhiệt và độ bền của tản nước AIO so với tản khí tháp đôi...",
    date: "25/07/2026",
    category: "Kinh Nghiệm PC",
    image: "https://images.unsplash.com/photo-1555680202-c86f0e12f086?auto=format&fit=crop&w=600&q=80",
    readTime: "4 phút đọc",
  },
];
