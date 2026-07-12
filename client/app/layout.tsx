import type { Metadata } from "next";
import "./globals.css";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import AuthInitializer from "@/features/auth/components/AuthInitializer";
import { getSession } from "@/app/actions/auth";

export const metadata: Metadata = {
  title:{
    template: "%s | AB Tech Zone",
    default: "AB Tech Zone"
  },
  description:"AB Tech Zone - Your one stop electronics store for all your needs",
  
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const user = await getSession();

  return (
    <html
      lang="en" 
    >
      <body className="font-poppins antialiased">
        <AuthInitializer user={user}>
          <div className="flex flex-col min-h-screen">
            <Header/>
            <main className="flex-1" >{children}</main>
            <Footer/>
          </div>
        </AuthInitializer>
      </body>
    </html>
  );
}

