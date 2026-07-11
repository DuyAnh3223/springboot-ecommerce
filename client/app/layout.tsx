import type { Metadata } from "next";
import "./globals.css";
import Header from "@/components/Header";
import Footer from "@/components/Footer";

export const metadata: Metadata = {
  title:{
    template: "%s | AB Tech Zone",
    default: "AB Tech Zone"
  },
  description:"AB Tech Zone - Your one stop electronics store for all your needs",
  
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
    >
      <body className="font-poppins antialiased">
        <Header/>
        {children}
        <Footer/>
      </body>
    </html>
  );
}
