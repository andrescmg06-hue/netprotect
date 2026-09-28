"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { useAuth } from "@/contexts/AuthContext";

import { BackgroundScene } from "./components/BackgroundScene";
import { Footer } from "./components/Footer";
import { HeroShowcase } from "./components/HeroShowcase";
import { LoginCard } from "./components/LoginCard";

/** Two-column reproduction of the referenced mockup (see docs/sprint-38-cierre.md for why this
 * lives at its own route instead of replacing the real "/" login). Sign-in is real
 * (LoginCard → GoogleButton → the same AuthContext every login path uses); the hero showcase and
 * background are illustrative exports, not live views.
 *
 * Fourth pass: pixel positions taken from the reference mockup (measured at 1672×941) converted
 * to vw/vh so they scale, with the desktop layout (`lg:` and up) placed by `position: absolute`
 * against the viewport instead of flex flow — the mockup gives exact coordinates for the title,
 * showcase and card, not a flow relationship between them. Below `lg` the layout stays a plain
 * stacked column (unchanged from before): the mockup's coordinates were only ever meant for the
 * desktop sizes it was checked against (1440×900 up to 1920×1080), not small screens.
 *
 * `tailwind.css` only loads the `theme`/`utilities` layers, not `preflight` (scoped intentionally,
 * see that file) — so `<h2>`/`<p>` keep the browser's default margin unless a class overrides it.
 * At `lg` this route positions elements by exact vh math, so that default margin (not collapsed,
 * since the container is `flex`) silently pushed the title down and the subtitle into the
 * showcase image; `lg:mt-0 lg:mb-0` on both zeroes it out so the only spacing is the explicit
 * `mt-[18px]` between them.
 */
export default function LoginMockupPage() {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/");
    }
  }, [status, router]);

  return (
    <main className="relative flex min-h-screen flex-col overflow-hidden px-4 py-10 sm:px-6 lg:block lg:h-screen lg:px-0 lg:py-0">
      <BackgroundScene />

      {/* Hero title + subtitle */}
      <div className="relative flex w-full flex-col items-center text-center lg:absolute lg:left-[15.5vw] lg:top-[17vh] lg:w-auto lg:max-w-none lg:items-start lg:text-left">
        <h2 className="text-[32px] font-extrabold leading-tight text-[#0B1F5C] sm:text-[36px] lg:mt-0 lg:mb-0 lg:text-[min(4vw,7vh)] lg:leading-[1.05]">
          Panel del tutor
        </h2>
        <p className="mt-3 max-w-[440px] text-base leading-relaxed text-[#5B6B8C] lg:mt-[18px] lg:mb-0 lg:max-w-[30vw] lg:text-[min(1.55vw,2.8vh)] lg:leading-[1.4]">
          Administra dispositivos, aplicaciones y reglas desde un solo lugar.
        </p>
      </div>

      <HeroShowcase />

      {/* Login card column */}
      <div className="relative mt-10 flex w-full items-center justify-center lg:absolute lg:left-[63vw] lg:right-[6vw] lg:top-1/2 lg:mt-0 lg:w-auto lg:-translate-y-1/2 lg:justify-start">
        <LoginCard />
      </div>

      <div className="lg:absolute lg:inset-x-0 lg:bottom-0">
        <Footer />
      </div>
    </main>
  );
}
