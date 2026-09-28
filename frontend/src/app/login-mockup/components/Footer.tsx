const LINKS = [
  { label: "Ayuda", href: "#" },
  { label: "Privacidad", href: "#" },
  { label: "Términos", href: "#" },
];

/** Real placeholder links (href="#"), not yet wired to pages that don't exist — matches the
 * brief exactly ("Ayuda, Privacidad y Términos son enlaces (href="#") por ahora"). Brand label on
 * the left, links on the right, no underline until hover/focus — matches the reference mockup. */
export function Footer() {
  return (
    <footer className="relative flex w-full flex-col items-center gap-2 px-4 pb-[3vh] text-[clamp(13px,1vw,16px)] sm:flex-row sm:items-center sm:justify-between sm:pb-[3vh] sm:pl-[4vw] sm:pr-[4.8vw]">
      <span>
        <span className="text-[#1B2F63]">NetProtect</span>
        <span className="text-[#5B6B8C]"> · Panel del tutor</span>
      </span>

      <nav aria-label="Enlaces del pie de página" className="flex items-center gap-[28px] text-[#5B6B8C]">
        {LINKS.map((link) => (
          <a key={link.label} href={link.href} className="no-underline hover:underline focus-visible:underline">
            {link.label}
          </a>
        ))}
      </nav>
    </footer>
  );
}
