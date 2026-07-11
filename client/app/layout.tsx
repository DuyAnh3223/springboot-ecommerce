import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";



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
      <body className="font-poppins antialiased">{children}</body>
    </html>
  );
}
