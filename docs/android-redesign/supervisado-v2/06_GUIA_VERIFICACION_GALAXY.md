# 06 · Guía de verificación en el Galaxy S25 FE y con un tutor real

Para quien tenga el teléfono físico y el panel web delante. Cubre lo que **el rediseño v2 (S61 a S67) no pudo probar desde el emulador** y está declarado pendiente en `docs/sprint-67-evidence.md`.
No sustituye a `05_PRUEBA_USUARIOS.md` (sesiones con adolescentes y tutores): esta guía es una **verificación técnica** de cada punto, con el resultado esperado y dónde anotar lo que pase.

**Regla del proyecto:** nada se da por hecho sin evidencia. Si un bloque no se hizo, se deja en «sin hacer»; si falla, se anota qué se vio (captura, hora UTC y, si hace falta, la fila de `audit_logs`).

## 0. Qué ya está verificado (no hace falta repetirlo)

En el emulador `Pixel_8`, con los 8 jobs de CI en verde en cada PR: servicios estables al navegar (G-19), desvinculación real desde la base de datos (B1), el bloqueo real con YouTube con gestos y con tres botones, el aviso «Se desactivó un permiso» con el permiso de superponer quitado con `appops`,
y las pantallas a letra 2.0 en la galería. Detalle y capturas en `docs/android-redesign/supervisado-v2/s64` a `s67`.

## 1. Preparación

**En la PC**

1. Backend de desarrollo arriba (`make dev`) y `docker ps` con `netprotect-dev-backend-1` en `healthy` y `netprotect-dev-coturn-1` en `Up` (sin reinicios: `docker inspect netprotect-dev-coturn-1 --format '{{.RestartCount}}'` debe dar `0`).
2. Panel web en `http://localhost:3000` (`cd frontend && npm run dev`), con sesión de Google **del tutor**.
3. Teléfono por USB con depuración activada. Serie del Galaxy usado antes: `R5CY92MJ2PY` (compruébalo con `adb devices`).

**Instalar la build** (desde `mobile/`; es la release con R8, firmada con la clave de debug y apuntando al backend local; no incluye la galería de depuración):

```
ANDROID_SERIAL=R5CY92MJ2PY NETPROTECT_API_BASE_URL=http://localhost:8000 ./gradlew installMinified
adb -s R5CY92MJ2PY reverse tcp:8000 tcp:8000
adb -s R5CY92MJ2PY reverse --list        # debe mostrar tcp:8000
```

El puente `adb reverse` se pierde al reconectar el cable o reiniciar `adb`; si el inicio de sesión dice «failed to connect to localhost port 8000», repite el `reverse` antes de culpar al backend.

**Qué dos cuentas usar:** el tutor entra en el panel; el supervisado inicia sesión en el teléfono con **otra** cuenta de Google. Genera el código de vinculación desde el panel.

**Para volver siempre a la app por el lanzador** (no con `am start -n`, que crea una instancia nueva): `adb -s R5CY92MJ2PY shell monkey -p com.netprotect.app -c android.intent.category.LAUNCHER 1`.

## 2. Bloques de prueba

Marca cada bloque en la tabla del final. Horas siempre en UTC.

### A · Vincular y bienvenida

1. Vincular con el código de 6 dígitos. **Esperado:** aparece la bienvenida («VINCULACIÓN COMPLETADA», «Este teléfono ya está vinculado»), con dos puntos de paso.
2. Pulsa «Ver qué hace NetProtect». **Esperado:** el paso 2 muestra las mismas frases que **Privacidad** (estado cada minuto, lista de apps cada 5 minutos, reglas y bloqueos, ubicación aproximada cada unos 15 minutos, aviso de desinstalación, vista de pantalla solo con tu permiso, «NetProtect no graba ni guarda lo que se ve en tu pantalla»). Ninguna debe hablar de cuánto tiempo se conservan los datos.
3. «Atrás» del sistema en el paso 1: **no** debe saltarse la bienvenida ni salir de la app. En el paso 2 vuelve al paso 1.
4. Botón final: «Entendido, configurar permisos» si queda algún permiso pendiente (abre Permisos) y «Entendido» si no queda ninguno (vuelve a Inicio).
5. Cierra y abre la app: la bienvenida **no** reaparece. Desvincula desde el panel y vincula de nuevo: **sí** reaparece.

### B · Servicios estables al navegar (G-19) en el teléfono

Con la app en Inicio, anota los servicios y recórrela unos 3 minutos entrando y saliendo de Permisos, el Detalle de un permiso, Estado, Privacidad y «Más ajustes», mandándola a Inicio del sistema y volviendo **por el lanzador**:

```
adb -s R5CY92MJ2PY shell dumpsys activity services com.netprotect.app | grep -E "ServiceRecord|createTime"
adb -s R5CY92MJ2PY shell pidof com.netprotect.app
```

**Esperado:** los mismos `ServiceRecord` al principio y al final, `createTime` que solo crece, y el mismo `pid`. En el backend, un latido por minuto (`docker logs -t netprotect-dev-backend-1 | grep heartbeat`).
Si cambian, guarda las dos salidas y `adb logcat -b events -d | grep wm_create_activity`.

### C · Permisos quitados en Ajustes y recuperados (los cuatro)

Con los cuatro permisos concedidos, quita **uno cada vez** en **Ajustes de Android** (a mano, como lo haría el menor), vuelve a la app **por el lanzador** y observa. Recupéralo con «Activar de nuevo» y confirma que el Detalle pasa solo a «Configurado».

| Permiso | Cómo quitarlo | Esperado en el teléfono | Esperado en el tutor (panel) |
|---|---|---|---|
| Acceso al uso de apps | Ajustes → acceso especial → acceso al uso | «Se desactivó un permiso», chip «Pendiente», nota «Tu tutor puede ver que este permiso está desactivado.» | Alerta «Permiso de acceso a uso revocado» en el siguiente latido |
| Ubicación aproximada | Ajustes → app → permisos → ubicación | Android **puede matar la app** al quitarla; al volver, el aviso debe salir igualmente (la línea base se guarda). Nota: «Activa el permiso para que NetProtect vuelva a funcionar.» **sin** mencionar al tutor | Nada nuevo (la ubicación no se reporta como «quitada») |
| Mostrar sobre otras apps | Ajustes → acceso especial → mostrar sobre otras apps | El aviso, con «es posible que NetProtect no pueda mostrar la pantalla de bloqueo.», sin mencionar al tutor | Nada nuevo |
| Protección contra desinstalación | Ajustes → seguridad → administradores del dispositivo → desactivar | Android muestra su diálogo («…tu tutor recibirá una alerta…»); al volver, el aviso con «Al desactivarla, NetProtect avisa a tu tutor. Actívala de nuevo…» | Alerta «Intento de desinstalación» |

Comprueba también: el aviso **no se repite** al volver otra vez; «Activar de nuevo» abre el Detalle de ese permiso; con dos permisos quitados a la vez se avisa del primero y Inicio cuenta el resto en «Falta un paso». Atajos opcionales por `adb` (en One UI pueden no comportarse igual que Ajustes, y no disparan el diálogo del administrador): `adb shell pm revoke com.netprotect.app android.permission.ACCESS_COARSE_LOCATION`.

### D · Aviso de batería (One UI)

1. Mira si Inicio muestra la fila «La batería puede detener NetProtect». Compara con Ajustes → Batería → límites de uso en segundo plano para NetProtect.
2. Pulsa la fila y «Abrir ajustes de batería». **Anota qué pantalla abre en One UI** (lista general de apps o la ficha de NetProtect) y **cómo se llaman exactamente las opciones** («Sin restricciones», «No optimizar»…).
3. Cambia el ajuste, vuelve a la app **por el lanzador**: la pantalla debe pasar a «Batería revisada» y la fila de Inicio desaparecer.
4. **El punto delicado:** ¿la fila coincide con lo que dice One UI? Anota los **falsos positivos** (aviso con «Sin restricciones» ya elegido) y **falsos negativos** (sin aviso aunque la app esté restringida). Es la comprobación estándar de Android y One UI puede no coincidir.

### E · Letra grande y animaciones

1. Ajustes → Pantalla → tamaño de letra: **130 %** y luego **200 %** (el máximo del teléfono). Recorre cada pantalla: Acceso, Vincular, bienvenida, Inicio (con y sin la fila de batería), Permisos, Detalle de cada permiso, aviso de permiso desactivado, batería, Estado (con y sin vista remota), consentimiento, Privacidad, «Más ajustes», «¿Cerrar sesión?», «Desvinculado» y la pantalla de bloqueo.
2. **Esperado:** ningún texto cortado ni partido por letras, ningún botón aplastado, y el pie fijo (consentimiento, Estado, Detalle) con scroll por encima y botones alcanzables.
3. Ajustes → Accesibilidad → «Quitar animaciones» activado: el candado de la pantalla de bloqueo aparece ya cerrado, sin animación, y nada se mueve en bucle.
4. Restaura el tamaño de letra y las animaciones al terminar.

### F · TalkBack y Accessibility Scanner

Activa TalkBack y recorre **las mismas pantallas** de E. En cada una comprueba: el orden de lectura es lógico, cada botón dice qué hace, los iconos decorativos no se leen, el titular se anuncia como encabezado, y el estado (p. ej. «Pendiente», «Configurado», los pasos «Paso 1 de 2») se entiende sin ver.
Críticas: Inicio (el sello con «4 de 4 permisos»), Permisos y Detalle (estados y botones por permiso), el consentimiento (casilla, cuenta atrás y «Continuar» desactivado hasta marcarla), la pantalla de bloqueo (ventana propia) y el código de vinculación de 6 casillas.
Pasa **Accessibility Scanner** por cada una y anota las áreas táctiles menores de 48 dp y los contrastes que marque.

### G · Vista remota de punta a punta (con el tutor real)

Con el tutor en el panel, en la ficha del dispositivo:

1. **Solicitar ver la pantalla.** En el teléfono, el consentimiento se abre desde cualquier pantalla, con «Esta solicitud caduca en 2 min» y la barra de progreso bajando. «Continuar» debe estar desactivado hasta marcar la casilla.
2. **Aceptar:** «Continuar» y luego el diálogo de Android. **Esperado:** el tutor ve el vídeo; el teléfono muestra el aviso permanente y el banner «Tu tutor está viendo tu pantalla». Anota si la conexión fue directa o por el relé TURN (`docker logs netprotect-dev-coturn-1` muestra sesiones si hubo relé).
3. **Detener:** desde el banner, desde «Estado de NetProtect» («Detener ahora») y desde el aviso permanente. **Toca el cuerpo del aviso permanente: debe abrir NetProtect** (S65).
4. **«Atrás»** en el consentimiento: el panel debe mostrar «El dispositivo cerró la solicitud sin responder.» (motivo `dismissed`) y poder volver a pedir.
5. **Dejar caducar** (2 min sin responder): el panel debe mostrar «La solicitud caducó: el dispositivo no respondió a tiempo.» (`expired`). Si apagas el wifi del teléfono antes, el envío es de mejor esfuerzo y sin cola: el panel debe rendirse a los 135 s con «El dispositivo no respondió.».
6. **«Ahora no»:** el panel debe mostrar «La persona supervisada no aceptó la solicitud.» y la auditoría registrar «Vista remota rechazada».
7. Gira el teléfono con el consentimiento abierto: la cuenta atrás **debe seguir** y la solicitud caducar a la hora (S65, hallazgo MEDIA).
8. Ajustes → notificaciones: ¿se pueden descartar los avisos permanentes de NetProtect en Android 16? Estado dice «Android muestra un aviso mientras una función está activa.»; anota si el menor puede quitarlos.

Auditoría en el backend para cruzar lo visto: `docker exec netprotect-dev-db-1 psql -U netprotect -d netprotect -c "select created_at, action from audit_logs order by created_at desc limit 15;"` (acciones `SCREEN_SHARE_REQUESTED`, `…_CONSENT_GRANTED`, `…_STARTED`, `…_STOPPED`; **no** se leen coordenadas).

### H · Desvincular desde el panel (B1)

Desvincula el dispositivo desde el panel. **Esperado:** al volver a la app (y como mucho a los 5 minutos con la app abierta) el teléfono pasa a «Este dispositivo ya no está vinculado», con «Vincular de nuevo» y «Cerrar sesión»; el latido y la ubicación dejan de enviarse (`dumpsys activity services` sin `ServiceRecord`).
Un corte de wifi **no** debe provocar esa pantalla (prueba: avión 10 minutos y volver).

### I · Bloqueo real en el teléfono

Con reglas reales desde el panel sobre una app instalada (técnica de `docs/sprint-49-evidence.md` §3), comprueba las siete variantes: bloqueo, límite diario, límite semanal, horario, categoría, modo escolar y política del dispositivo. **Esperado:** panel navy con «APP BLOQUEADA», el candado, el nombre de la app, el motivo como titular, la nota «El bloqueo cubre la pantalla de la app, pero no la cierra.» y «Ir al inicio» por encima de la barra del sistema (con gestos y con tres botones). Ningún texto promete cuándo se desbloquea.

### J · Ubicación «Sin confirmar» (B2)

Con la ubicación concedida pero sin red (modo avión), espera más de 35 minutos (o prueba con la ubicación del teléfono apagada): Estado debe pasar la ubicación a «Sin confirmar» con «No hay una ubicación reciente confirmada.» y volver a «Activo» tras el siguiente envío correcto.

## 3. Registro de resultados

| Bloque | Resultado (✅ / ❌ / sin hacer) | Quién y cuándo | Evidencia (captura, hora UTC, notas) |
|---|---|---|---|
| A · Vincular y bienvenida | | | |
| B · Servicios estables (G-19) | | | |
| C · Permisos quitados (los 4) | | | |
| D · Batería en One UI | | | |
| E · Letra 130 % y 200 %, «quitar animaciones» | | | |
| F · TalkBack y Accessibility Scanner | | | |
| G · Vista remota con tutor real | | | |
| H · Desvincular desde el panel | | | |
| I · Bloqueo real (7 variantes) | | | |
| J · Ubicación «Sin confirmar» | | | |

**Si algo falla**, antes de arreglarlo: guarda la captura, la hora UTC, `adb logcat -d | tail -200` del teléfono y, si interviene el tutor, la fila de `audit_logs`. Un fallo de un texto sobre los datos de un menor se trata como un fallo de seguridad (se contrasta con el código antes de corregirlo).
Cuando un bloque pase, cópialo a `docs/sprint-67-evidence.md` (sección «Pendiente») con su evidencia, para que deje de estar declarado como pendiente.
