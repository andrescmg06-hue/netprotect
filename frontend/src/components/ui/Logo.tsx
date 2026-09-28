import Image from "next/image";

/** Brand mark from public/brand/ (cropped from the delivered logo, transparent background).
 * `unoptimized`: a static asset served from our own origin, so neither the image optimizer nor
 * a CSP change is needed. */
export function Logo({ variant = "full", height = 44 }: { variant?: "full" | "shield"; height?: number }) {
  if (variant === "shield") {
    return (
      <Image
        src="/brand/logo-shield.png"
        alt="NetProtect"
        width={Math.round(height * (219 / 256))}
        height={height}
        unoptimized
        priority
      />
    );
  }
  return (
    <Image
      src="/brand/logo-full.png"
      alt="NetProtect · Panel del tutor"
      width={Math.round(height * (720 / 208))}
      height={height}
      unoptimized
      priority
    />
  );
}
