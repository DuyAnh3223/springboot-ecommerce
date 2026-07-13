import type { Metadata } from "next";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import AuthInitializer from "@/features/auth/components/AuthInitializer";
import { getUserSession } from "@/features/auth/actions";

export const metadata: Metadata = {
  title:{
    template: "%s | AB Tech Zone",
    default: "AB Tech Zone"
  },
  description:"AB Tech Zone - Your one stop electronics store for all your needs",
  
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
        <Header/>
        <main className="flex-1" >{children}</main>
        <Footer/>
      </div>
    </AuthInitializer>
  );
}


