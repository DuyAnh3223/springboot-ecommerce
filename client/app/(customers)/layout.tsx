import type { Metadata } from "next";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import AnnouncementBar from "@/components/AnnouncementBar";
import AuthInitializer from "@/features/auth/components/AuthInitializer";
import { getUserSession } from "@/features/auth/actions";

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

  return (
    <AuthInitializer user={user}>
      <div className="flex flex-col min-h-screen">
        <AnnouncementBar />
        <Header />
        <main className="flex-1">{children}</main>
        <Footer />
      </div>
    </AuthInitializer>
  );
}



