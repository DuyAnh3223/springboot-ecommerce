import React from "react";
import { redirect } from "next/navigation";
import { getUserSession } from "@/features/auth/actions";
import { getAddresses } from "@/features/users/services/address.service";
import AddressList from "@/features/users/components/AddressList";

export const metadata = {
  title: "Sổ địa chỉ | AB Tech Zone",
};

export default async function ProfileAddressPage() {
  const user = await getUserSession();

  if (!user) {
    redirect("/sign-in");
  }

  // Fetch initial addresses page from server side
  let addresses: any[] = [];
  try {
    const pageResult = await getAddresses();
    addresses = pageResult.content || [];
  } catch (error) {
    console.error("Failed to load addresses on server side:", error);
  }

  return <AddressList addresses={addresses} />;
}
