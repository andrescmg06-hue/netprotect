import Image from "next/image";

/** Brand mark from public/brand/ (cropped from the delivered logo, transparent background).
 * `unoptimized`: a static asset served from our own origin, so neither the image optimizer nor
 * a CSP change is needed.
 *
 * `onDark` (default false) swaps the full logo for the version with a white wordmark, for navy
 * surfaces such as the sidebar (S54). Same 720×208 geometry, so the width math is shared. The
 * shield has no on-dark variant: it already reads on navy. */
export function Logo({
  variant = "full",
  height = 44,
  onDark = false,
}: {
  variant?: "full" | "shield";
  height?: number;
  onDark?: boolean;
}) {
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
      src={onDark ? "/brand/logo-full-on-dark.png" : "/brand/logo-full.png"}
      alt="NetProtect · Panel del tutor"
      width={Math.round(height * (720 / 208))}
      height={height}
      unoptimized
      priority
    />
  );
}
