# Sprint 25 — Pruebas integrales

## Objetivo

`docs/planning/plan-desarrollo.md` (Paso 24) pide: "Unitarias, de integración, de API (colección
Postman/Newman en CI), E2E web con Playwright, instrumentadas de Android, offline, sincronización,
permisos, autorización y rendimiento con k6."

De esa lista, unitarias y de integración de backend ya existen desde su sprint de origen (261
pruebas de integración reales contra PostgreSQL/Redis en Docker, más las unitarias de cada
servicio) — este sprint no las repite. Lo que faltaba, y es el alcance real de este sprint, son
cinco frentes que ningún sprint anterior cubría:

1. Una prueba que recorre el propio router de FastAPI y verifica que ninguna ruta funcional queda
   sin una dependencia de autenticación — mencionada explícitamente como criterio de verificación
   del Sprint 4 (`plan-desarrollo.md`, Paso 3) pero nunca escrita.
2. Pruebas instrumentadas de Android para el offline/sincronización del Sprint 19
   (`RulesCacheStore`, `PendingRuleEventStore`) — hasta ahora sin ninguna cobertura, ni unitaria ni
   instrumentada, porque Room exige un runtime Android real (o Robolectric, no instalado en este
   proyecto) para ejecutar SQLite.
3. Una colección de API ejercitada con Newman en CI, cubriendo un flujo real de extremo a extremo.
4. Pruebas E2E web con Playwright, contra el panel web completo del Sprint 24 y el backend real.
5. Una prueba de rendimiento con k6 contra el backend real en Docker.

## Historias de usuario del sprint

| ID | Historia |
|---|---|
| HU-071 | Como responsable técnico del proyecto, quiero una prueba automatizada que confirme que ninguna ruta HTTP del backend quedó sin protección de autenticación, para no depender de revisar cada endpoint a mano cada vez que se agrega uno. |
| HU-072 | Como responsable técnico, quiero pruebas reales (no dobles) del caché offline y de la cola de eventos pendientes de Android, para confiar en que el dispositivo sigue aplicando reglas sin conexión tal como documenta el Sprint 19. |
| HU-073 | Como responsable técnico, quiero una colección de API ejecutable en CI que reproduzca un flujo real de tutor+supervisado, para detectar una regresión de contrato HTTP sin tener que leer los tests de pytest. |
| HU-074 | Como responsable técnico, quiero pruebas E2E del panel web que abran un navegador real contra el backend real, para detectar una regresión visual o de integración que las pruebas unitarias de React no pueden ver. |
| HU-075 | Como responsable técnico, quiero una medición real de latencia/throughput del backend bajo carga moderada, para tener una referencia antes del Sprint 26 (despliegue). |

## Criterios de aceptación

1. La prueba de barrido de rutas falla si se agrega un endpoint nuevo sin autenticación y no está
   en la lista explícita de excepciones documentadas (salud, login, refresh, WebSocket).
2. Las pruebas instrumentadas de Android corren contra una base de datos Room real (no un doble en
   memoria hecho a mano) en un dispositivo/emulador real, verificado con
   `./gradlew connectedDebugAndroidTest` ejecutado de verdad, no sólo compilado.
3. La colección de Newman corre en CI contra el backend real en Docker (mismo stack que
   `compose.test.yaml`) y falla si cualquier paso del flujo devuelve un código o cuerpo distinto
   del esperado.
4. Las pruebas de Playwright corren contra el backend real en Docker y el panel web servido de
   verdad (build de producción, no sólo `next dev`), con aserciones sobre contenido real, no sólo
   capturas de pantalla.
5. La prueba de k6 se ejecuta contra el backend real en Docker y su salida (requests/s, p95, tasa
   de error) queda documentada en `docs/sprint-25-evidence.md` con números reales, no estimados.
6. CI en GitHub Actions pasa en los jobs existentes más los que este sprint agrega, en un runner
   limpio.

## Decisiones de diseño y su motivo

### Por qué Newman necesita un script de "siembra" y no un login real

`/auth/google` valida un ID token real contra los JWKS de Google — no existe, ni debe existir, un
mecanismo en el propio backend para simular esa verificación desde fuera del proceso (eso sería
una puerta trasera de autenticación en código de producción, exactamente lo que este proyecto evita
en el resto de sus decisiones de seguridad). Los tests de pytest lo resuelven con
`unittest.mock.patch` sobre la función Python que llama a Google — algo que sólo es posible dentro
del mismo proceso, no desde una herramienta de caja negra como Newman.

La solución adoptada, coherente con el mismo principio que ya autorizó la verificación visual del
Sprint 24 (`docs/sprint-24-evidence.md`): un script (`backend/scripts/seed_test_session.py`) que
corre dentro del contenedor del backend y usa las funciones **reales y no mockeadas** del propio
proyecto (`create_access_token`, `hash_refresh_token`, la sesión de base de datos) para crear un
usuario y una sesión válidos — el mismo trabajo que hace `_make_account()` en los tests de pytest,
sólo que expuesto como script en lugar de fixture, porque Newman no puede invocar una fixture de
Python. No es un bypass de autenticación en el backend: ningún archivo de `backend/app` cambia,
sólo se añade un script bajo `backend/scripts/`, y su docstring dice explícitamente que no forma
parte de la aplicación. La llamada real a Google sigue siendo la única pieza que ningún agente
puede ejercitar (mismo límite ya aceptado en cada sprint de autenticación).

### Por qué Playwright corre contra un build de producción, no `next dev`

`next dev` compila bajo demanda e inyecta HMR; una prueba E2E que pase contra `next dev` pero falle
contra el build real que se despliega (Sprint 26) no sirve de nada. `npm run build && npm run
start` sirve exactamente lo que un usuario vería en producción, al costo de un paso de build más
lento en CI.

### Por qué las pruebas de Android son de Room, no de UI

Compose ya tiene cobertura indirecta a través de la revisión visual manual de cada sprint (Sprint
24, por ejemplo) y una prueba de UI instrumentada duplicaría esfuerzo por poco valor nuevo: la
lógica de Compose en este proyecto es mayormente declarativa y delgada. El offline/sincronización
del Sprint 19, en cambio, tenía **cero** cobertura de ningún tipo porque Room no puede probarse sin
un runtime Android real — es la brecha de mayor riesgo real (una regresión ahí rompe silenciosamente
"las reglas siguen aplicándose sin Internet", el criterio de aceptación central de ese sprint) y el
mejor uso del único emulador disponible en esta máquina.

### Por qué k6 corre un perfil de carga moderado, no una prueba de estrés

Sin infraestructura de producción real (Sprint 26 sigue pendiente), medir el punto de quiebre no
tiene valor todavía — cualquier número dependería de los recursos de un contenedor de desarrollo
en esta máquina, no del despliegue real. El valor de esta prueba en este sprint es tener una
**referencia repetible** (mismo script, mismo perfil) para comparar después de desplegar, no un
veredicto de capacidad.

## Alcance no cubierto en este sprint (pendiente explícito)

- Instrumentación de UI de Compose (justificada arriba).
- Cualquier prueba que dependa de un login real de Google — sigue siendo, como en cada sprint
  anterior, algo que ningún agente puede ejecutar.
- Pruebas de rendimiento de Android (batería, memoria) — fuera del alcance que
  `plan-desarrollo.md` pide para este paso.

Ver `docs/sprint-25-evidence.md` para los comandos ejecutados y su salida real.
