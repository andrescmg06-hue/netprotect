import sys; sys.path.insert(0,'/tmp/gen')
from lib import *

# ---------- Acceso
def photo(h=300):
    return f'<div style="height:{h}px;overflow:hidden;position:relative;flex:none"><div style="position:absolute;inset:0;background:url({PHOTO}) center 35%/cover;"></div><div style="position:absolute;left:0;bottom:0;background:#0d1b2a;color:#fff;padding:10px 18px 10px 24px"><span class="eye" style="color:#cdd8e8">NetProtect · Dispositivo supervisado</span></div></div>'
def acceso(kind):
    status='<div class="hr" style="margin-top:22px;padding-top:12px;display:flex;align-items:center;gap:10px;font-size:14px"><span class="dot" style="background:#16b364"></span><span style="color:#0b7a42;font-weight:500">Servicio listo</span></div>'
    msg='';ttl='Inicia sesión';lead='Usa tu cuenta de Google para conectar este teléfono con tu tutor.'
    if kind=='srv':
        msg=f'<div class="msg rs" style="background:#feeceb;color:#c4271c;margin-top:18px">{ic("alert",22,"margin-top:1px")}<span>NetProtect no puede conectarse al servicio ahora. Inténtalo de nuevo en unos momentos.</span></div>'
        status=f'<div style="margin-top:6px">{tl("Reintentar","refresh")}</div>'
    if kind=='net':
        msg=f'<div class="msg rs" style="background:#feeceb;color:#c4271c;margin-top:18px">{ic("wifioff",22,"margin-top:1px")}<span>No hay conexión a Internet. Comprueba tu conexión e inténtalo de nuevo.</span></div>'
        status=f'<div style="margin-top:6px">{tl("Reintentar","refresh")}</div>'
    if kind=='exp':
        ttl='Vuelve a entrar';lead='Por seguridad cerramos tu sesión después de un tiempo. Inicia sesión de nuevo para continuar.'
        msg=f'<div class="msg rs" style="background:#fef3e2;color:#9a5000;margin-top:18px">{ic("clock",22,"margin-top:1px")}<span>Tu sesión caducó.</span></div>'
    g=f'<button class="btn o" style="margin-top:22px"><span style="display:flex;align-items:center;gap:12px">{GOOGLE}Iniciar sesión con Google</span><span class="ar">{ic("arrow",20)}</span></button>'
    return ph(f'''{photo(300)}
<div style="padding:22px 24px 0;display:flex;flex-direction:column;flex:1">
<h1 class="disp rs" style="font-size:42px">{ttl}</h1><p class="bd rs" style="margin:10px 0 0;color:#5b6779;--d:.08s">{lead}</p>{msg}{g}{status}
<div style="flex:1"></div><div class="xs" style="display:flex;gap:10px;align-items:center;padding-bottom:34px">{ic("info",18)}Solo puedes iniciar sesión con Google.</div></div>''')
def cargando():
    return ph(f'''<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;background:#0d1b2a;color:#fff;border-radius:28px">
<div style="position:relative;width:168px;height:168px">{seal(['ok']*4,'ok',168)}</div>
<div style="margin-top:26px;font:600 26px/1.2 'Playfair Display',serif">NetProtect</div>
<div class="sm" style="color:#9fb4d4;margin-top:6px;display:flex;align-items:center;gap:10px">{ic('spin',16,'')}Restaurando tu sesión…</div></div>''')
def v_acceso():
    b=col('1 · Cargando',cargando())+col('2 · Inicio de sesión · servicio listo',acceso('ok'))+col('3 · Servicio no disponible',acceso('srv'))+col('4 · Sin internet',acceso('net'))+col('5 · Tu sesión caducó',acceso('exp'))
    save('Acceso.dc.html',board('Cargando e inicio de sesión',2360,1050,b))

# ---------- Vincular
def link_graphic(state):
    # phone ---- tutor with traveling dot
    col_line={'wait':'#c9c5bb','go':'#1769ff','err':'#c4271c'}[state]
    dotanim='<span style="position:absolute;top:-4px;left:0;width:10px;height:10px;border-radius:50%;background:#1769ff;animation:hop 1.6s var(--e) infinite"></span>' if state=='go' else ''
    mid='<span style="position:absolute;left:50%;top:-12px;margin-left:-10px;color:#c4271c;font:700 15px/1 Inter">×</span>' if state=='err' else ''
    return f'''<div style="display:flex;align-items:center;gap:0;margin:0 0 22px" aria-hidden="true"><span class="tile" style="width:52px;height:52px;background:#fff;color:#11243d">{ic('phone',24)}</span><span style="position:relative;flex:1;height:0;border-top:2px dashed {col_line};margin:0 10px">{dotanim}{mid}</span><span class="tile" style="width:52px;height:52px;background:#0d1b2a;color:#fff;border-color:#0d1b2a">{ic('user',24)}</span></div>'''
def cells(vals,kind):
    out=''
    for i in range(6):
        v=vals[i] if i<len(vals) else ''
        st='border:1.5px solid #8a8678;background:#fff;'
        extra=''
        if kind=='focus' and i==len(vals): st='border:2px solid #1769ff;background:#fff;box-shadow:0 0 0 3px #dbe7ff;'
        if kind=='go': st='border:1.5px solid #1769ff;background:#eaf1ff;'; extra=f'animation:pulse 1.2s ease-in-out infinite;animation-delay:{i*.12}s;'
        if kind=='err': st='border:1.5px solid #c4271c;background:#fff;'
        out+=f'<input inputmode="numeric" maxlength="1" aria-label="Dígito {i+1}" value="{v}" {"disabled" if kind=="go" else ""} style="width:100%;min-width:0;height:68px;box-sizing:border-box;text-align:center;border-radius:3px;font:600 30px/1 \'Playfair Display\',serif;color:#11243d;padding:0;{st}{extra}">'
    return f'<div style="display:flex;gap:8px;{"animation:shake .5s" if kind=="err" else ""}">{out}</div>'
def vincular(kind):
    if kind=='wait':
        g=link_graphic('wait');c=cells([], 'focus');sub='';b=btn('Vincular dispositivo','off',None,True,'',chip='check');foot=f'<span class="dot" style="background:#1769ff"></span>Esperando el código'
        s='<p class="xs" style="margin:10px 0 0">Puedes pegar los 6 dígitos de una vez. El botón se activa con los 6 dígitos.</p>'
    if kind=='go':
        g=link_graphic('go');c=cells(list('482915'),'go');b=btn('Vinculando…','',None,False,'pointer-events:none');foot=f'<span class="dot" style="background:#1769ff"></span>Confirmando el código con tu tutor';s='<p class="xs" style="margin:10px 0 0">No cierres la app.</p>'
    if kind=='err':
        g=link_graphic('err');c=cells(list('482915'),'err');b=btn('Vincular dispositivo','',None,True,'',chip='check');foot=f'<span class="dot" style="background:#c4271c"></span>El código no se pudo usar'
        s=f'<div class="msg" style="background:#feeceb;color:#c4271c;margin-top:12px">{ic("alert",22,"margin-top:1px")}<span>El código no es válido o ya venció. Pídele uno nuevo a tu tutor.</span></div>'
    inner=f'''<div class="top"><span style="display:flex;align-items:center;gap:10px;margin-left:24px"><img src="{SH}" alt="NetProtect" style="height:26px;width:auto"><span class="eye">Dispositivo supervisado</span></span><span>{tl("Cerrar sesión","logout","#5b6779")}</span></div>
<div style="padding:26px 24px 0;display:flex;flex-direction:column;flex:1">
{g}<div class="eye">Paso 1 de 1 · Vinculación</div><h1 class="disp rs" style="margin-top:8px">Vincula este dispositivo</h1>
<p class="bd rs" style="margin:10px 0 24px;color:#5b6779;--d:.08s">Escribe el código de 6 dígitos que te dio tu tutor. Lo genera en el panel web y caduca pronto.</p>
{c}{s}<div style="margin-top:20px">{b}</div>
<p class="sm" style="margin:16px 0 0"><span style="color:#11243d;font-weight:500">¿No tienes un código?</span> Pídeselo a tu tutor.</p>
<div style="flex:1"></div>
<div class="hr" style="padding:12px 0 36px;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:4px 12px"><span style="display:flex;align-items:center;gap:10px;font-size:14px">{foot}</span>{tl("Comprobar vínculo","refresh") if kind!="go" else ""}</div></div>'''
    return ph(inner)
def v_vincular():
    b=col('1 · Esperando el código',vincular('wait'))+col('2 · Vinculando',vincular('go'))+col('3 · Código inválido o vencido',vincular('err'))
    save('Vincular.dc.html',board('Vincular este dispositivo',1440,1050,b))

# ---------- Consentimiento
def steps(cur):
    items=['Leer','Autorizar','Android']
    out=''
    for i,t in enumerate(items):
        on=i<=cur
        out+=f'<div style="flex:1"><div style="height:4px;border-radius:1px;background:{"#1769ff" if on else "#d9d6cf"}"></div><div class="eye" style="margin-top:6px;color:{"#11243d" if on else "#8a8678"}">{i+1} · {t}</div></div>'
    return f'<div style="display:flex;gap:8px;padding:6px 24px 0">{out}</div>'
def consent(checked):
    chk=' checked' if checked else ''
    css='''<style>
.cx{position:relative;display:flex;gap:14px;align-items:flex-start;padding:18px 0;min-height:48px;cursor:pointer;border-top:1px solid #d9d6cf;border-bottom:1px solid #d9d6cf;margin:14px 0 0}
.cx input{position:absolute;opacity:0;width:100%;height:100%;left:0;top:0;margin:0;cursor:pointer}
.cx .bx{width:30px;height:30px;border:2px solid #8a8678;border-radius:3px;background:#fff;flex:none;display:flex;align-items:center;justify-content:center;transition:background .2s,border-color .2s}
.cx .bx svg{width:20px;height:20px;stroke:#fff;fill:none;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:30;stroke-dashoffset:30;transition:stroke-dashoffset .3s var(--e)}
.cx input:checked~.bx{background:#1769ff;border-color:#1769ff}
.cx input:checked~.bx svg{stroke-dashoffset:0}
.cx input:focus-visible~.bx{box-shadow:0 0 0 3px #dbe7ff}
</style>'''
    return css,chk
def consentimiento(checked):
    css,chk=consent(checked)
    btnk='' if checked else 'off'
    exp='1 min 48 s' if checked else '2 min'
    facts=''.join(f'<div class="rs" style="display:flex;gap:14px;padding:11px 0;--d:{.1+i*.08:.2f}s;font-size:calc(15px*var(--k));line-height:1.45">{ic(i_,20,"color:#1456d9;margin-top:2px")}<span>{t}</span></div>' for i,(i_,t) in enumerate([('eye','Tu tutor verá en vivo lo que haces en este teléfono.'),('nog','NetProtect no graba ni guarda la pantalla.'),('stop','Puedes detenerlo en cualquier momento desde “Estado de NetProtect”.')]))
    inner=f'''{css}{top()}{steps(1)}
<div style="padding:16px 24px 0;flex:1;display:flex;flex-direction:column">
<div class="eye">Solicitud de tu tutor</div><h1 class="disp rs" style="font-size:34px;margin-top:6px">Quiere ver tu pantalla</h1>
<p class="bd rs" style="margin:8px 0 6px;color:#5b6779;--d:.05s">Solo se mostrará mientras tú lo permitas.</p>
{facts}
<label class="cx"><input type="checkbox"{chk} aria-describedby="n"><span class="bx"><svg viewBox="0 0 24 24"><path d="M20 6 9 17l-5-5"/></svg></span><span style="font-weight:500;font-size:calc(16px*var(--k));line-height:1.4">Autorizo a mi tutor a ver la pantalla de este dispositivo ahora</span></label>
<div style="display:flex;align-items:center;gap:8px;margin-top:12px" class="sm">{ic('clock',18)}Esta solicitud caduca en {exp}</div>
<div style="height:3px;background:#e4e1da;margin-top:8px;overflow:hidden"><div style="height:100%;background:#1769ff;transform-origin:left;animation:shrink 120s linear forwards"></div></div>
<p id="n" class="xs" style="margin:12px 0 0">Después, Android mostrará su propio aviso de captura de pantalla. Atrás no cuenta como aceptar.</p>
<div style="flex:1"></div></div>
<div style="padding:12px 24px 36px;display:flex;flex-direction:column;gap:8px;border-top:1px solid #d9d6cf">{'' if checked else '<div class="xs" style="text-align:center">Marca la casilla para continuar.</div>'}{btn("Continuar",btnk)}{btn("Ahora no","g",None,False)}</div>'''
    return ph(inner)
def notifs():
    def n(i_,t,s,d): return f'<div class="rs" style="background:#2a3446;border-radius:16px;padding:14px 16px;display:flex;gap:12px;--d:{d}s"><span style="color:#9ec0ff">{ic(i_)}</span><div><div style="font-size:12px;color:#b9c3d4;margin-bottom:4px">NetProtect · ahora</div><b style="display:block;font-size:15px;line-height:1.35;color:#fff">{t}</b><p style="margin:2px 0 0;font-size:14px;line-height:1.4;color:#d3dae6">{s}</p></div></div>'
    return f'''<div style="background:#1a2230;color:#eef1f6;flex:1;padding:56px 16px 0;display:flex;flex-direction:column;gap:10px;border-radius:28px">
{n('shield','NetProtect está activo','Reporte y control de apps funcionando en este dispositivo.',.1)}
{n('pin','NetProtect comparte la ubicación','Se envía una ubicación aproximada cada unos 15 minutos.',.2)}
{n('monitor','Tu tutor está viendo esta pantalla','Toca para abrir NetProtect y detener la vista.',.3)}
<div style="font-size:12px;line-height:1.4;color:#8f9bb0;margin:6px 4px 0">Cada aviso aparece solo mientras su función está activa. Android no permite quitarlos.</div></div>'''
def v_consent():
    b=col('1 · Sin autorizar',consentimiento(False))+col('2 · Autorizado',consentimiento(True))+col('3 · Notificaciones permanentes de Android',ph(notifs()))
    save('Consentimiento.dc.html',board('Consentimiento de vista remota',1440,1050,b))

# ---------- Bloqueo
def lockicon():
    return '<svg viewBox="0 0 64 64" width="84" height="84" aria-hidden="true"><g style="animation:shackle 1.2s var(--e) both"><path d="M20 28V20a12 12 0 0 1 24 0v8" fill="none" stroke="#fff" stroke-width="4" stroke-linecap="round"/></g><rect x="12" y="28" width="40" height="28" rx="4" fill="#1769ff"/><circle cx="32" cy="42" r="4" fill="#0d1b2a"/></svg>'
def bloqueo(cap,reason,sup,catline,until=None,uico='clock'):
    u=f'<div class="rs" style="margin-top:20px;border-top:1px solid #d9d6cf;border-bottom:1px solid #d9d6cf;padding:14px 0;display:flex;align-items:center;gap:12px;font-weight:500;--d:.2s">{ic(uico,20,"color:#1456d9")}{until}</div>' if until else ''
    inner=f'''<div style="background:#0d1b2a;color:#fff;padding:60px 24px 28px;flex:0 0 440px;box-sizing:border-box;display:flex;flex-direction:column;position:relative;overflow:hidden">
<div class="eye" style="color:#9fb4d4;display:flex;align-items:center;gap:10px">{ic('lock',16)}App bloqueada</div>
<div style="position:absolute;right:20px;top:54px;opacity:.95">{lockicon()}</div>
<div style="margin-top:auto;display:flex;align-items:center;gap:16px"><div class="tile" style="width:60px;height:60px;background:#10233b;border-color:#2b4565;color:#9fb4d4">{ic('app',28)}</div><div><div style="font:600 28px/1.1 'Playfair Display',serif">Nombre de la app</div><div class="sm" style="color:#9fb4d4">{catline}</div></div></div></div>
<div style="padding:26px 24px 0;flex:1;display:flex;flex-direction:column"><h2 class="ttl rs" style="font-size:31px">{reason}</h2><p class="bd rs" style="margin:10px 0 0;color:#5b6779;--d:.1s">{sup}</p>{u}
<div style="flex:1"></div><p class="xs" style="margin:0 0 12px">El bloqueo cubre la pantalla de la app, pero no la cierra.</p>{btn("Ir al inicio","",None,True,"",chip="home")}<div style="height:36px"></div></div>'''
    return col(cap,ph(inner))
def v_bloqueo():
    items=[('1 · Bloqueo directo','Tu tutor bloqueó esta app.','No puedes usarla por ahora. Si crees que es un error, habla con tu tutor.','Ejemplo de app bloqueada',None,'clock'),
    ('2 · Límite diario','Llegaste al límite diario de esta app.','Tu tutor puso un tiempo máximo por día para ella.','Ejemplo de app con límite','Vuelve a abrirla mañana','clock'),
    ('3 · Límite semanal','Llegaste al límite semanal de esta app.','Tu tutor puso un tiempo máximo por semana para ella.','Ejemplo de app con límite','Vuelve a abrirla el lunes','clock'),
    ('4 · Fuera de horario','Esta app no está disponible en este horario.','Tu tutor definió cuándo puedes usarla.','Ejemplo de app con horario','Se desbloquea cuando termine el horario','cal'),
    ('5 · Categoría bloqueada','Tu tutor bloqueó esta categoría de apps.','Todas las apps de la categoría Juegos están bloqueadas por ahora.','Categoría: Juegos (ejemplo)',None,'clock'),
    ('6 · Horario escolar','Esta app está bloqueada durante el horario escolar.','Podrás usarla cuando termine.','Ejemplo de app bloqueada','Horario escolar activo','cal'),
    ('7 · Solo apps aprobadas','Ahora solo puedes usar apps aprobadas por tu tutor.','Esta app no está en esa lista. Pídele a tu tutor que la apruebe si la necesitas.','No está en la lista aprobada',None,'clock')]
    b=''.join(bloqueo(*i) for i in items)
    save('Bloqueo.dc.html',board('App bloqueada',3280,1050,b))

# ---------- Detalle
def detalle(kind):
    seg=''.join(f'<i style="flex:1;height:5px;border-radius:1px;background:{"#0b7a42" if i<2 or (kind=="ok" and i==2) else ("#9a5000" if i==2 else "#d9d6cf")}"></i>' for i in range(4))
    if kind=='pe': chip='<span class="chip c-pe">Pendiente</span>';mid=f'''<div class="eye" style="border-bottom:1px solid #11243d;padding-bottom:6px">Cómo activarlo</div>
{''.join(f'<div class="rs" style="display:flex;gap:14px;padding:12px 0;--d:{.1+i*.08:.2f}s;font-size:calc(15px*var(--k))"><span style="width:28px;height:28px;border-radius:50%;border:1.5px solid #11243d;display:flex;align-items:center;justify-content:center;font:600 13px/1 Inter;flex:none">{i+1}</span><span>{t}</span></div>' for i,t in enumerate(['Toca “Abrir Ajustes”.','Busca NetProtect y activa el permiso.','Vuelve a esta pantalla. La comprobaremos automáticamente.']))}''';bar=btn('Abrir Ajustes','',None,True,'',chip='ext')+btn('Volver a permisos','g',None,False)
    if kind=='ck': chip='<span class="chip c-ve"><svg class="ic sp" style="width:14px;height:14px"><use href="#i-spin"/></svg>Comprobando…</span>';mid='<div class="eye" style="border-bottom:1px solid #11243d;padding-bottom:6px">Estado</div><p class="bd" style="color:#5b6779">Estamos comprobando si Android confirma el permiso. No marcaremos nada como listo hasta tener la respuesta.</p><div style="height:4px;background:#e4e1da;overflow:hidden;margin-top:16px"><div style="width:30%;height:100%;background:#1769ff;animation:scan 1.4s var(--e) infinite"></div></div>';bar=btn('Comprobando…','off',None,False)+btn('Volver a permisos','g',None,False)
    if kind=='ok': chip='<span class="chip c-ok">Configurado</span>';mid='<div class="eye" style="border-bottom:1px solid #11243d;padding-bottom:6px">Estado</div><div style="display:flex;gap:14px;align-items:center;padding:16px 0"><svg viewBox="0 0 64 64" width="64" height="64"><circle cx="32" cy="32" r="28" fill="#e7f8ef" stroke="#0b7a42" stroke-width="3"/><path d="M20 33l8 8 17-18" fill="none" stroke="#0b7a42" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="30" style="animation:tick .6s var(--e) both .2s"/></svg><p class="bd" style="margin:0;color:#5b6779">Android confirma que este permiso está activo.</p></div><p class="xs">Para desactivarlo, ve a los ajustes de Android.</p>';bar=btn('Volver a permisos','',None,True,'',chip='back')
    inner=f'''{top()}
<div style="padding:8px 24px 0"><div style="display:flex;gap:5px">{seg}</div><div class="eye" style="margin-top:8px">Permiso 3 de 4</div></div>
<div style="padding:18px 24px 0;flex:1"><div style="width:64px;height:64px;background:#0d1b2a;color:#fff;border-radius:4px;display:flex;align-items:center;justify-content:center">{ic('monitor',30)}</div>
<h1 class="disp rs" style="margin-top:16px;font-size:34px">Mostrar sobre otras apps</h1><div style="margin:10px 0 18px">{chip}</div>
<div class="eye" style="border-bottom:1px solid #11243d;padding-bottom:6px">Qué habilita</div><p class="bd" style="margin:10px 0 22px">Permite que NetProtect muestre la pantalla de bloqueo cuando una app no está disponible.</p>{mid}</div>
<div style="padding:12px 24px 36px;display:flex;flex-direction:column;gap:8px;border-top:1px solid #d9d6cf">{bar}</div>'''
    return ph(inner)
def v_detalle():
    b=col('1 · Permiso pendiente',detalle('pe'))+col('2 · Comprobando al volver de Ajustes',detalle('ck'))+col('3 · Permiso configurado',detalle('ok'))
    save('Detalle.dc.html',board('Detalle de permiso',1440,1050,b))
if __name__=='__main__':
    v_acceso();v_vincular();v_consent();v_bloqueo();v_detalle()
