import React from "react";
import { redirect } from "next/navigation";
import { getUserSession } from "@/features/auth/actions";
import Container from "@/components/Container";
import ProfileSidebar from "@/features/users/components/ProfileSidebar";

export const metadata = {
  title: "Tài khoản của tôi",
  description: "Quản lý thông tin cá nhân, sổ địa chỉ và đơn hàng đã mua",
};

interface ProfileLayoutProps {
  children: React.ReactNode;
}

export default async function ProfileLayout({ children }: ProfileLayoutProps) {
  const user = await getUserSession();

  if (!user) {
    redirect("/sign-in");
  }

  return (
    <Container className="py-8 md:py-12">
      <div className="flex flex-col md:flex-row gap-6 md:gap-8 items-start">
        {/* Sidebar */}
        <ProfileSidebar user={user} />

        {/* Content Area */}
        <div className="flex-1 w-full min-w-0">{children}</div>
      </div>
    </Container>
  );
}
