"use client";

import { usePathname } from "next/navigation";
import { motion } from "framer-motion";
import { Sidebar } from "@/components/shell/sidebar";
import { easeOutQuint, standard } from "@/lib/motion";

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  return (
    <div className="min-h-screen relative z-10">
      <Sidebar />
      <main className="ml-[240px] min-h-screen">
        <motion.div
          key={pathname}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: standard, ease: easeOutQuint }}
          className="mx-auto max-w-[1280px] px-8 py-8"
        >
          {children}
        </motion.div>
      </main>
    </div>
  );
}
