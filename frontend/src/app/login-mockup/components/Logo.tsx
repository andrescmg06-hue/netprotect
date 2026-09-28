/** Shield-with-family mark, shown once on this screen — inside the login card only, horizontal
 * (shield left, "NetProtect" + "PANEL DEL TUTOR" stacked to its right), matching the reference
 * mockup. Same inline SVG as before (not the project's real PNG brand asset) so it stays crisp at
 * any size; only the layout changed from the earlier vertical/stacked arrangement. */
export function Logo() {
  return (
    <div className="flex items-center gap-[clamp(10px,0.9vw,14px)]">
      <svg
        className="w-[clamp(56px,4.6vw,76px)] shrink-0"
        viewBox="0 0 86 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <defs>
          <linearGradient id="netprotect-shield" x1="0" y1="0" x2="86" y2="100" gradientUnits="userSpaceOnUse">
            <stop stopColor="#0B3FBF" />
            <stop offset="1" stopColor="#3AA0FF" />
          </linearGradient>
        </defs>
        <path d="M43 0 84 15v27c0 30-17 48-41 58C19 90 2 72 2 42V15Z" fill="url(#netprotect-shield)" />
        <circle cx="38" cy="42" r="9" fill="#FFFFFF" />
        <path d="M22 74c1-14 8-21 16-21s15 7 16 21c-10 6-22 6-32 0Z" fill="#FFFFFF" />
        <circle cx="56" cy="58" r="6.5" fill="#BFDBFE" />
        <path d="M45 79c1-10 6-15 11-15s10 5 11 15c-7 4-15 4-22 0Z" fill="#BFDBFE" />
      </svg>

      <div className="flex flex-col gap-[2px]">
        <span className="font-display text-[clamp(28px,2.4vw,40px)] font-extrabold leading-none">
          <span className="text-brand-ink">Net</span>
          <span className="text-brand-primary">Protect</span>
        </span>
        <span className="text-[clamp(10px,0.72vw,12px)] font-semibold uppercase tracking-[0.3em] text-[#6B80B0]">
          Panel del tutor
        </span>
      </div>
    </div>
  );
}
