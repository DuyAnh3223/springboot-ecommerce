import React from "react";
import { redirect } from "next/navigation";
import { getUserSession } from "@/features/auth/actions";
import ProfileForm from "@/features/users/components/ProfileForm";

export const metadata = {
  title: "Thông tin cá nhân | AB Tech Zone",
};

export default async function ProfilePage() {
  const user = await getUserSession();

  if (!user) {
    redirect("/sign-in");
  }

  return <ProfileForm initialUser={user} />;
}
