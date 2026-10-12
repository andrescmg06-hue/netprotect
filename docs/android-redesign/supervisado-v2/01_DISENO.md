# 01 · Sistema visual v2 del modo supervisado ("Sello")

Estilo: editorial y sobrio. Crema/piedra/navy, títulos serif, texto Inter, acento azul NetProtect, líneas finas, radios de 4 dp, **sin degradados, sin vidrio, sin emoji, sin tema oscuro** (decisión del dueño: solo tema claro). Una sola familia de iconos lineales. Todo en español de Colombia.

Esto **extiende** el sistema de diseño ya implementado en S42 (`ui/theme/`, `ui/components/`). Antes de crear algo, comprueba con `grep` qué existe ya y reutilízalo; solo se añade lo que falte.

## Color

| Token sugerido | Valor | Uso |
|---|---|---|
| `NpCream` | #F5F3EE | Fondo de pantalla |
| `NpStone` | #F3F1EC | Fichas de icono, deshabilitado |
| `NpHairline` | #D9D6CF | Separadores de 1 dp |
| `NpNavy` | #0D1B2A | Panel de identidad (hero) y bloqueo |
| `NpInk` | #11243D | Texto principal |
| `NpInkMuted` | #5B6779 | Texto secundario (AA sobre crema) |
| `NpBlue` | #1769FF | Acción principal; **no** para texto sobre crema |
| `NpBlueText` | #1456D9 | Enlaces y texto azul |
| `NpOk` | #0B7A42 | Activo / listo |
| `NpWarn` | #9A5000 | Pendiente |
| `NpError` | #C4271C | Error |

Antes de crear tokens, compara con `ui/theme/Color.kt`: lo más probable es que casi todo ya exista con otro nombre.

## Tipografía

| Token | Fuente | Tamaño |
|---|---|---|
| `NpDisplay` | serif 600 | 36/40 sp |
| `NpTitle` | serif 600 | 26/32 sp |
| `NpBody` | Inter 400 | 16/25 sp |
| `NpBodySm` | Inter 400 | 14/21 sp |
| `NpLabel` | Inter 600 | 15/20 sp |
| `NpEyebrow` | Inter 600, +0,14 em | 11/16 sp |

- Siempre en **sp**. Probar con escala de fuente 200 %.
- **Fuente serif definitiva: pendiente.** Las capturas usan Playfair Display como aproximación. Candidatas a comparar: Source Serif 4, Newsreader. Debe tener licencia libre (OFL) y empaquetarse en la app.

## Espaciado, radios, sombras

Espaciado 4 · 8 · 12 · 16 · 24 · 28 · 36 dp; margen lateral `NpGutter` = 28 dp. Radio de campos y botones 4 dp; estados como chip (pill). Elevación 0 dp: se separa con línea, no con sombra.

## Componentes nuevos o a adaptar

| Componente | Qué es | Dónde |
|---|---|---|
| `NpSeal` | Sello de 4 arcos con el escudo al centro: arco verde dibujado = permiso listo, ámbar = pendiente, gris punteado = inactivo | Inicio, Cargando, Bienvenida |
| `NpHeroPanel` | Panel navy con titular serif, sello y resumen | Inicio |
| `NpRail` | Recorrido vertical de permisos con "siguiente paso" resaltado | Permisos |
| `NpRow` | Ficha de icono + título + descripción | Estado, Privacidad |
| `NpStatusChip` | Punto + texto (Listo, Activo, Pendiente, Vinculado, Error, Inactivo) | Varios |
| `NpPrimaryButton` | ≥ 60 dp de alto, chip de flecha/icono a la derecha que se desliza al pulsar | Acciones principales |
| `NpOutlineButton` | Borde 1,5 dp | Detener, Ahora no |
| `NpTextButton` | ≥ 48 dp, subrayado que se barre al pulsar | Cerrar sesión, Más tarde |
| `NpCodeField` | 6 casillas, solo dígitos, pegar | Vincular |
| `NpBottomBar` | Línea superior; acciones fijas | Consentimiento, Detalle |
| `NpBlockedScreen` | Panel navy + motivo | 7 variantes de bloqueo |

Ya existen en S42: `NpButton`, `NpCard`, `NpTopBar`, `NpBottomBar`, `OtpInput`, `PermissionCard`, `StatusPill`, `InfoBanner`, `ListRow`, `IconTile`, `SegmentedControl`, `ConfirmDialog`, `LoadingState`, `ErrorState`… Adapta, no dupliques.

## Iconos

Una sola familia lineal (la de `ui/icons/NpIcons.kt`, Lucide), trazo 1,75 dp, 22 dp (20 dp en botones). Sin emoji. Cada icono acompaña a un texto; nunca lo reemplaza.

## Movimiento

| Token | Valor | Uso |
|---|---|---|
| `NpMotionFast` | 120 ms | Pulsación |
| `NpMotionBase` | 240 ms | Transición |
| `NpMotionSlow` | 360 ms | Entrada de pantalla |
| `NpEasing` | cubic(0.2, 0, 0, 1) | Siempre |

Reglas:
- El movimiento ocurre **ante eventos**: entrada escalonada (fundido + 14 dp, 80 ms entre filas) y arcos del sello que se dibujan **una sola vez** en 1 s.
- **Sin animaciones en bucle**, salvo la vista remota en curso (punto que late), los indicadores de carga y la barra del contador.
- Con "Quitar animaciones" de Android (`ANIMATOR_DURATION_SCALE = 0`), todo pasa a cambio inmediato.

## Accesibilidad (criterios de aceptación de todas las pantallas)

- Objetivo táctil ≥ 48 dp (botones principales 60 dp).
- Contraste AA como mínimo; el azul de acción nunca como texto sobre crema.
- Funciona con **letra al 200 %**: sin alturas fijas en texto, nada cortado, scroll si no cabe (ver lámina 08).
- TalkBack: orden de lectura lógico, cada estado anunciado ("Permiso pendiente", "Comprobando…").
- El estado nunca depende solo del color: siempre hay texto + icono.
- Android 15+ dibuja edge-to-edge: respetar insets de barra de estado y de navegación.
- Forzar tema claro: desactivar "oscurecimiento forzado" (`android:forceDarkAllowed="false"`).
- Pendiente de diseño: tabletas, horizontal y plegables.
