import Image from "next/image";

/** Brand mark from public/brand/ (cropped from the delivered logo, transparent background).
 * Served through the Next image optimizer (same origin, so `img-src 'self'` already covers
 * `/_next/image`): the 720×208 sources weigh ~100 KB and are shown at ~44 px high. `quality` is
 * raised above the default 75 so the wordmark edges stay crisp.
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
        quality={90}
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
      quality={90}
      priority
    />
  );
}
