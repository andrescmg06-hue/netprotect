# Sprint 42 — Sistema de diseño Android y esqueleto de navegación

Tercer sprint del rediseño Android (`docs/android-redesign/sprints/S42-sistema-de-diseno-y-navegacion.md`).
Primer sprint con trabajo delegado a DeepSeek. Ninguna pantalla existente se rediseñó: solo la
base sobre la que se construyen las 17 pantallas.

## Qué se hizo

- **Claude**: `ui/navigation/NavStack` (pila de rutas inmutable, nunca vacía, sin duplicar la ruta de
  arriba, guardable/restaurable, con `rememberNavStack` y `NavBackHandler`) con 12 tests JVM (D-01).
  Dependencias de test de Compose (`ui-test-junit4`, `ui-test-manifest`, D-13) en commit propio.
  Envolvió `MainActivity` en `NetProtectTheme` con barras del sistema transparentes e iconos oscuros.
- **DeepSeek, encargo 42a** (`docs/delegated/done/sprint-42a-fundamentos.md`): `ui/theme` (30 colores,
  tipografía Inter, formas, elevación, tonos), Inter v4.1 empaquetada, 52 iconos Lucide 1.48.0 + la "G"
  de Google como `VectorDrawable`, logo, icono de lanzador adaptativo, `ui/format` (tiempo relativo,
  duraciones, agrupación por día, hora, etiquetas en español) y 31 tests JVM. Licencias en
  `mobile/third_party_licenses/`.
- **DeepSeek, encargo 42b** (`docs/delegated/done/sprint-42b-componentes.md`): 23 componentes en
  `ui/components/` (cada uno con `@Preview`), la galería de componentes solo para debug y 8 tests de UI (`ComponentsTest` 7, `DesignGalleryTest` 1).

## Lo que corrigió Claude en la revisión

- 42a: `AlertLabels` escribía "Se bloqueó null" si la API omitía el nombre de la app o de la zona
  (ahora "una app"/"una zona", con test); el icono de lanzador pierde el calificador `-v26` obsoleto
  (minSdk 26) y gana capa monocromo, lo que elimina 3 avisos de lint.
- 42b: la barra inferior era un rectángulo plano y el mockup 03 la muestra como tarjeta redondeada con
  sombra; las tarjetas usaban la sombra negra de `Surface` en vez de la azulada y suave de `DESIGN.md`
  (ahora `Modifier.shadow` con `NpShadow`); la galería se dibujaba bajo la barra de estado.

## Decisiones

- **`RelativeTime`**: el encargo decía a la vez "1–23 h → hace N h" y "ayer a las 23:00 visto a las 09:00 →
  ayer". Se adopta la lectura de DeepSeek: "hace N h" solo dentro del mismo día calendario; si la fecha es
  la de ayer, "ayer" aunque hayan pasado menos de 24 h.
- **Etiquetas de auditoría y estados de dispositivo**: el panel web no traduce las 25 acciones de
  auditoría (muestra `DEVICE_RENAMED`) y solo etiqueta 3 de los 6 estados. Las etiquetas móviles son
  decisión propia de este sprint y viven en `ui/format/AuditLabels` y `DeviceStatusLabels`.
- **Sin `Locale` en los formateadores**: meses, "a. m."/"p. m." construidos a mano; los datos de idioma
  de la JVM cambian entre versiones (p. ej. el espacio antes de "p. m.") y romperían los tests.
- **Controles pulsables de 48 dp** aunque `DESIGN.md` diga 32–40 px: es móvil, no escritorio.
- **Barras del sistema claras** en `MainActivity`: las pantallas que siguen con el aspecto oscuro anterior
  muestran la hora y los iconos casi negros sobre fondo oscuro hasta su sprint. Aceptado en la rama de
  integración `android-redesign`, **no en `main`** (S51 no debe integrar con este efecto).

## Excepción de seguridad documentada: la galería exportada

`DesignGalleryActivity` está declarada con `android:exported="true"` para poder abrirla con
`adb shell am start`. Contradice la invariante 11 de `security-reviewer` ("ningún componente exportado
nuevo"); se acepta **solo** porque cumple las tres condiciones siguientes, comprobadas:

1. Vive en `mobile/app/src/debug/` (solo compila en builds *debug*).
2. Sin `intent-filter` y sin permisos: no aparece en el lanzador ni expone datos.
3. **Ausente del manifiesto fusionado de release** (`merged_manifests/release/.../AndroidManifest.xml`:
   0 coincidencias de `DesignGallery`; en el de debug, 1).

## Pendiente

- Icono de lanzador en el Samsung real: comprobado en el emulador (escudo sobre fondo claro), no en el
  teléfono físico (no estaba conectado).
- Línea de tiempo (`TimelineItem`): la línea vertical queda larga entre eventos; se ajusta al usarla en el
  Historial (S46).
- Login real de Google: sin cambios (H-01).
