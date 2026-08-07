import type { Metadata } from "next";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import AnnouncementBar from "@/components/AnnouncementBar";
import AuthInitializer from "@/features/auth/components/AuthInitializer";
import { CartInitializer } from "@/features/customer/cart/components/CartInitializer";
import { cartService } from "@/features/customer/cart/services/cart.service";
import { getUserSession } from "@/features/auth/actions";
import { CartSnapshot } from "@/features/customer/cart/types/cart.types";

export const metadata: Metadata = {
  title: {
    template: "%s | ABTechZone",
    default: "ABTechZone - Thế Giới Linh Kiện PC & Gaming Rig",
  },
  description:
    "ABTechZone - Hệ thống bán lẻ linh kiện máy tính, CPU, Card màn hình VGA, Mainboard, RAM, SSD chính hãng với giá tốt nhất thị trường.",
};

export default async function ClientLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const user = await getUserSession();
  let cart: CartSnapshot | null = null;

  if (user) {
    try {
      cart = await cartService.getCart();
    } catch {
      cart = { cartId: null, status: "ACTIVE", items: [], userId: null };
    }
  }

  const initialCartQuantity = cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;
  const cartScopeKey = user?.username ?? "guest";

  return (
    <AuthInitializer user={user}>
      <CartInitializer cart={cart} scopeKey={cartScopeKey}>
        <div className="flex flex-col min-h-screen">
          <AnnouncementBar />
          <Header initialCartQuantity={initialCartQuantity} />
          <main className="flex-1">{children}</main>
          <Footer />
        </div>
      </CartInitializer>
    </AuthInitializer>
  );
}
