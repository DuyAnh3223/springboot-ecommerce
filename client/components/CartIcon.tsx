"use client";

import Link from "next/link";
import React from "react";
import { ShoppingBag } from "lucide-react";
import { useCartHydration } from "@/features/customer/cart/components/CartInitializer";
import { useCartStore } from "@/features/customer/cart/stores/cart.store";

interface CartIconProps {
  initialQuantity?: number;
}

const CartIcon = ({ initialQuantity = 0 }: CartIconProps) => {
  const hydratedQuantity = useCartStore((state) => state.getTotalQuantity());
  const { isHydrated } = useCartHydration();
  const totalQuantity = isHydrated ? hydratedQuantity : initialQuantity;
  const displayCount = totalQuantity > 99 ? "99+" : totalQuantity.toString();

  return (
    <Link
      href="/cart"
      className="group relative inline-flex items-center justify-center p-1"
      aria-label={`Giỏ hàng, ${totalQuantity} sản phẩm`}
    >
      <ShoppingBag className="h-5 w-5 hover:text-shop_light_green hoverEffect" />
      {totalQuantity > 0 && (
        <span className="absolute -top-1 -right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-shop_dark_green px-1 text-[10px] font-bold text-white shadow-2xs">
          {displayCount}
        </span>
      )}
    </Link>
  );
};

export default CartIcon;