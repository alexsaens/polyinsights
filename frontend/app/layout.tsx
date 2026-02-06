import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "PolyInsights",
  description: "Market Insight & Report Generator powered by Polymarket data.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
