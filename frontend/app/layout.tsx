import type { Metadata } from "next";
import { GeistSans } from "geist/font/sans";
import { GeistMono } from "geist/font/mono";
import { Instrument_Serif } from "next/font/google";
import { NoiseOverlay } from "@/components/primitives/noise-overlay";
import "./globals.css";

const instrumentSerif = Instrument_Serif({
  subsets: ["latin"],
  weight: "400",
  style: "italic",
  variable: "--font-instrument-serif",
});

export const metadata: Metadata = {
  title: "GTM-Zero",
  description: "AI Sales Engineer for Technical Founders",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="en"
      className={`${GeistSans.variable} ${GeistMono.variable} ${instrumentSerif.variable} dark`}
    >
      <body className="bg-bg-base text-text-primary antialiased">
        <NoiseOverlay />
        {children}
      </body>
    </html>
  );
}
