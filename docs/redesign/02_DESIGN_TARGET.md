# Diseño objetivo — tokens y componentes

Fuente de verdad visual: el sistema de diseño «NetProtect» publicado en Claude (README, tokens, componentes y mockups). Este archivo traduce esos tokens a las variables que **ya existen** en `frontend/src/app/globals.css`, para migrar sin renombrar nada y sin romper los 16 paneles.

## 1. Valores nuevos para `:root` (mismos nombres)

```css
:root {
  color-scheme: light;

  /* Azul de marca: solo acento (activo, primario, "encendido") */
  --color-primary: #1769ff;        /* relleno; texto blanco encima 4.7:1 */
  --color-primary-hover: #1456d9;
  --color-primary-soft: #eaf1ff;
  --color-primary-softer: #f3f1ec;  /* ahora es el lavado neutro cálido de hover */
  --color-primary-text: #1456d9;    /* azul para texto sobre crema/blanco: 5.6:1 */
  --color-navy: #11243d;            /* títulos y valores (antes #102b63) */

  /* Superficies crema */
  --color-bg: #f5f3ee;
  --color-bg-gradient: #f5f3ee;     /* sin degradado: color plano */
  --color-surface: #ffffff;
  --color-surface-muted: #f8f7f3;
  --color-border: #d9d6cf;          /* piedra: líneas y divisores */
  --color-border-strong: #8a8678;   /* borde de controles, 3.3:1 */

  /* Texto */
  --color-text: #11243d;
  --color-text-muted: #5b6779;      /* 5.2:1 sobre crema. El #697586 del brief solo sobre blanco */
  --color-text-subtle: #8a8678;     /* placeholders */

  /* Estado (sin cambios de valor; verde solo positivo, rojo solo alerta) */
  --color-success: #16b364;  --color-success-soft: #e7f8ef;  --color-success-text: #0b7a42;
  --color-danger:  #f04438;  --color-danger-soft:  #feeceb;  --color-danger-text:  #c4271c;
  --color-warning: #f79009;  --color-warning-soft: #fef3e2;  --color-warning-text: #9a5000;
  --color-purple: #7a5af8;   --color-purple-soft:  #f1edfe;  --color-purple-text:  #5b3fd6;
  --color-neutral-soft: #f3f1ec;

  /* Nuevas: navy del sidebar y superficies oscuras */
  --color-navy-950: #0d1b2a;
  --color-navy-900: #10233b;
  --color-navy-700: #17345c;
  --color-sidebar-bg: var(--color-navy-950);
  --color-sidebar-text: #e8edf5;
  --color-sidebar-muted: #9fb0c6;
  --color-sidebar-line: #ffffff1f;
  --color-sidebar-active: var(--color-primary);
  --color-primary-on-dark: #7fa8ff;
  --color-cream-50: #f8f7f3;
  --color-cream-200: #f3f1ec;

  /* Radios: casi cuadrados */
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 8px;
  --radius-xl: 8px;       /* tope: no subir de 8px */
  --radius-pill: 999px;   /* solo switch y puntos de estado */

  /* Sombras: casi ninguna */
  --shadow-card: 0 1px 2px #11243d0f;
  --shadow-pop: 0 12px 32px #0d1b2a24;
  --focus-ring: 0 0 0 2px #f5f3ee, 0 0 0 4px #1769ff;

  /* Espaciado: se conservan 1–10 y se añaden los de composición editorial */
  --space-12: 48px;
  --space-16: 64px;
  --space-24: 96px;
  --sidebar-width: 264px;
  --content-max: 1240px;
  --band-height: 168px;

  /* Tipografía */
  --font-sans: var(--font-inter), system-ui, -apple-system, "Segoe UI", sans-serif;
  --font-serif: var(--font-playfair), "Libre Caslon Text", Georgia, "Times New Roman", serif;
  --text-4xl: 44px;   /* display: título de página */
  --text-5xl: 64px;   /* display-xl: solo el "Inicia sesión" */
}
```

Reglas de uso:

- `h1`–`h4` pasan a `font-family: var(--font-serif)` (peso 600, color `--color-navy`). Todo lo demás (botones, formularios, tablas, navegación, estados, datos, etiquetas) sigue en `--font-sans`.
- `body` deja de usar degradado: `background: var(--color-bg)`.
- Cifras con `font-variant-numeric: tabular-nums`.
- Etiquetas en mayúsculas (`eyebrow`): 11 px, peso 600, `letter-spacing: 0.14em`.
- Frase editorial de cada página (`quote`): serif cursiva 17/24.

## 2. Carga de la serif (`app/layout.tsx`)

```ts
import { Inter, Playfair_Display } from "next/font/google";
const playfair = Playfair_Display({ subsets: ["latin"], variable: "--font-playfair", display: "swap", weight: ["400", "600"], style: ["normal", "italic"] });
// <html lang="es" className={`${inter.variable} ${playfair.variable}`}>
```

Se sirve desde el propio origen en build, así que la CSP `font-src 'self'` no cambia. Playfair Display es una aproximación a los mockups (que son imágenes sin fuente declarada): confirmar la fuente definitiva con el diseñador antes de cerrar.

## 3. Componentes globales

| Componente | Cambio | Notas |
| --- | --- | --- |
| `Sidebar` | Fondo `--color-sidebar-bg`, grupos con eyebrow, divisores finos, ítem activo `--color-sidebar-active` con radio 4 px, pie con tarjeta de marca (foto + frase) | Mantener 3 layouts (full/riel/cajón). Logo `logo-full-on-dark.png`. |
| `Header` | Buscador a la izquierda; notificaciones, configuración y perfil a la derecha | Sin tarjetas. La búsqueda sigue siendo local (secciones y dispositivos ya cargados). |
| `PageHeader` | Banda de fotografía (`--band-height`) con migas, título serif, descripción, frase `quote` y slot `aside` (selector de dispositivo) | Solo `DashboardShell` la pinta. Velo crema a la izquierda para contraste. |
| `Card` / `MetricCard` | Pasan a panel abierto: sin sombra, sin fondo flotante; línea `--color-border` o fondo `--color-surface-muted`. `MetricCard` se vuelve cifra con etiqueta separada por líneas | Mantener props. Prohibido usar 4 en fila como patrón repetido. |
| `StatusBadge` | Radio 4 px o píldora solo para puntos; siempre texto además del color | Tonos por significado. |
| `Timeline` | Línea continua, hora a la izquierda, nodo azul en el evento más reciente, días agrupados | Protagonista de Historial y «Actividad reciente». |
| `ScheduleBar` (nuevo) | Barra de 24 h con tramo «modo escolar» arrastrable, un renglón por día | Reemplaza los selectores de hora como interacción principal; mantener selectores como alternativa de teclado. Usa la misma API de política. |
| `EmptyState` | Ilustración ligera por contexto (apps, escudo, documentos, campana, campana tachada) en vez de icono + texto + botón | Ver `assets/ilustraciones/` (3 disponibles; el resto pendientes). |
| `Switch`, `Button`, `Field`, `SegmentedControl`, `DataTable` | Solo ajustes de token (radio, borde `--color-border-strong`, foco) | Sin cambios de API. |

## 4. Assets

- `assets/logos/`: `logo-full.png` (sobre claro), `logo-full-on-dark.png` (sobre navy, derivado del oficial: validar), `logo-shield.png` (isotipo / favicon). **Faltan:** isotipo blanco monocromo y SVG limpio; hay que pedirlos al original vectorial. Los mockups muestran un wordmark serif distinto al del logo oficial (sans): el oficial manda.
- `assets/fotografia/`: banda alpina (vistas), estudio (login a sangre), valle al atardecer (tarjeta de marca), follaje suave (detalle). Servir optimizadas (`next/image`, ≤250 KB).
- `assets/ilustraciones/`: estilo neón/vidrio, más recargado que la dirección sobria y con logos de terceros; úsalas pequeñas o sustitúyelas por ilustración lineal propia.
- `assets/mockups/`: referencia visual por vista.

## 5. Criterios de aceptación visual

- Texto ≥ 4.5:1 sobre su fondo; bordes de control ≥ 3:1; foco visible.
- Ninguna página repite la rejilla de tarjetas; el azul de marca aparece en un solo punto de énfasis por pantalla.
- Un tutor sin conocimientos técnicos entiende en un vistazo: cómo están los dispositivos, qué requiere atención, qué pasó y qué hacer.
