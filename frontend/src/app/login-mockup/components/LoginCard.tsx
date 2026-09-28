import { GoogleButton } from "./GoogleButton";
import { Logo } from "./Logo";

/** The sign-in card — the only place this screen shows the logo (the hero column doesn't repeat
 * it). Same real sign-in flow as the rest of the app (GoogleButton). No email/password form:
 * only Google, per the brief.
 *
 * Sizes below are clamp()s converted from the reference mockup (measured at 1672×941) so the card
 * scales with the viewport instead of sitting at one fixed size.
 *
 * `mb-0` on the heading/paragraphs: this route's `tailwind.css` doesn't load Tailwind's
 * `preflight` layer (scoped on purpose, see that file), so `<h1>`/`<p>` keep the browser's default
 * margin unless overridden — inside this `flex flex-col` card those margins don't collapse, so
 * left alone they'd add on top of every `mt-[...]` gap below and inflate the mockup's exact
 * spacing.
 *
 * `min-w-[440px]` is `lg:`-only: it's the mockup's clamp() floor for the desktop absolute layout,
 * not a floor for the phone-width stacked layout below `lg` — unscoped it forced the card wider
 * than a real phone viewport and `<main>`'s `overflow-hidden` clipped it instead of scrolling. */
export function LoginCard() {
  return (
    <div className="relative mx-auto flex w-full max-w-[420px] flex-col items-center rounded-[28px] bg-white px-[clamp(32px,2.75vw,46px)] pt-[clamp(44px,3.8vw,64px)] pb-[clamp(36px,3vw,50px)] shadow-[0_24px_60px_rgba(31,78,160,0.12)] lg:max-w-[600px] lg:min-w-[440px]">
      <Logo />

      <h1 className="mt-[clamp(40px,3.8vw,64px)] mb-0 text-center text-[clamp(28px,2.4vw,40px)] font-extrabold text-[#0B1F5C]">
        Inicia sesión
      </h1>

      <p className="mt-[20px] mb-0 text-center text-[clamp(15px,1.2vw,20px)] leading-[1.4] text-[#5B6B8C] lg:max-w-[27vw]">
        Accede a tu panel para administrar tus dispositivos, aplicaciones y reglas.
      </p>

      <div className="mt-[clamp(24px,2.15vw,36px)] w-full">
        <GoogleButton />
      </div>

      <div className="mt-[clamp(22px,2.05vw,34px)] w-full border-t border-[#E5EAF3] pt-[clamp(20px,1.8vw,30px)] text-center">
        <p className="m-0 text-[15px] text-[#5B6B8C]">Tu cuenta de Google es tu cuenta en NetProtect.</p>
      </div>
    </div>
  );
}
