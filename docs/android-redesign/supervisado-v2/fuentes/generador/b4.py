import sys; sys.path.insert(0,'/tmp/gen')
from lib import *
from b1 import PERMS

def bienvenida1():
    return ph(f'''<div style="background:#0d1b2a;color:#fff;flex:1;display:flex;flex-direction:column;padding:70px 24px 0;border-radius:28px">
<div class="eye" style="color:#9fb4d4">Vinculación completada</div>
<div style="margin:30px auto 0;position:relative;width:200px;height:200px">{seal(['ok']*4,'ok',200)}</div>
<h1 class="disp rs" style="color:#fff;margin-top:30px">Este teléfono ya está vinculado</h1>
<p class="bd rs" style="margin:12px 0 0;color:#d3dcea;--d:.15s">Antes de seguir, te explicamos en dos pasos qué hace NetProtect aquí.</p>
<div style="flex:1"></div>
<div style="padding-bottom:36px">{btn('Ver qué hace NetProtect')}<div style="display:flex;gap:6px;justify-content:center;margin-top:16px"><i style="width:22px;height:4px;border-radius:2px;background:#fff"></i><i style="width:22px;height:4px;border-radius:2px;background:#2b4565"></i></div></div></div>''')
def bienvenida2():
    def r(i_,t,s): return f'<div class="rs" style="display:flex;gap:14px;padding:14px 0;border-bottom:1px solid #d9d6cf"><span class="tile">{ic(i_)}</span><div><b class="bd">{t}</b><div class="sm">{s}</div></div></div>'
    return ph(f'''{top()}<div style="padding:8px 24px 0;flex:1">
<div class="eye">Paso 2 de 2</div><h1 class="disp rs" style="margin-top:6px;font-size:34px">Esto es lo que tu tutor puede ver</h1>
<div style="margin-top:14px;border-top:1px solid #11243d">
{r('wifi','Si el dispositivo está activo','Y cuándo se comunicó por última vez.')}
{r('grid','Cuánto tiempo usas las apps','Para aplicar las reglas que él configure.')}
{r('pin','Tu ubicación aproximada','Cada unos 15 minutos, no en tiempo real.')}
</div>
<div class="eye" style="margin-top:20px">Y tú decides</div>
<div class="rs" style="display:flex;gap:14px;padding:12px 0;font-size:calc(15px*var(--k))">{ic('eye',20,'color:#1456d9;margin-top:2px')}<span>La vista de pantalla solo se activa si tú la autorizas, y puedes detenerla.</span></div>
</div>
<div style="padding:12px 24px 36px;border-top:1px solid #d9d6cf"><div style="display:flex;gap:6px;justify-content:center;margin-bottom:12px"><i style="width:22px;height:4px;border-radius:2px;background:#2b4565;opacity:.35"></i><i style="width:22px;height:4px;border-radius:2px;background:#11243d"></i></div>{btn('Entendido, configurar permisos')}</div>''')
def bateria():
    def s(n,t): return f'<div class="rs" style="display:flex;gap:14px;padding:11px 0;font-size:calc(15px*var(--k));--d:{.1+n*.08:.2f}s"><span style="width:28px;height:28px;border-radius:50%;border:1.5px solid #11243d;display:flex;align-items:center;justify-content:center;font:600 13px/1 Inter;flex:none">{n}</span><span>{t}</span></div>'
    return ph(f'''{top()}<div style="padding:8px 24px 0;flex:1">
<div style="width:64px;height:64px;background:#fef3e2;color:#9a5000;border-radius:4px;display:flex;align-items:center;justify-content:center">{ic('alert',30)}</div>
<h1 class="disp rs" style="margin-top:16px;font-size:34px">NetProtect se detiene en segundo plano</h1>
<p class="bd rs" style="margin:10px 0 16px;color:#5b6779">Tu teléfono (samsung SM-S731B) está limitando la batería de NetProtect. Por eso tu tutor no recibe reportes.</p>
<div class="eye" style="border-bottom:1px solid #11243d;padding-bottom:6px">Cómo arreglarlo</div>
{s(1,'Abre los ajustes de batería de NetProtect.')}{s(2,'Elige “Sin restricciones” o “No optimizar”.')}{s(3,'Vuelve aquí. Lo comprobaremos solos.')}
<p class="xs" style="margin-top:8px">Los nombres pueden cambiar según el fabricante. Pasos por validar en un Samsung real.</p></div>
<div style="padding:12px 24px 36px;border-top:1px solid #d9d6cf;display:flex;flex-direction:column;gap:8px">{btn('Abrir ajustes de batería','',None,True,'',chip='ext')}{btn('Más tarde','g',None,False)}</div>''')
def revocado():
    return ph(f'''{top()}<div style="padding:8px 24px 0;flex:1">
<div class="eye" style="color:#9a5000">Permiso desactivado</div><h1 class="disp rs" style="margin-top:6px;font-size:34px">Se desactivó un permiso</h1>
<p class="bd rs" style="margin:10px 0 16px;color:#5b6779">“Mostrar sobre otras apps” se quitó en Ajustes de Android. Mientras tanto, NetProtect no puede mostrar la pantalla de bloqueo.</p>
<div class="rs" style="display:flex;gap:14px;align-items:flex-start;padding:16px;border:1.5px solid #9a5000;border-radius:4px;background:#fff"><span class="tile" style="background:#fef3e2;border-color:#f1d29b;color:#9a5000">{ic('monitor')}</span><div><b class="bd">Mostrar sobre otras apps</b><div style="margin-top:4px"><span class="chip c-pe">Pendiente</span></div></div></div>
<p class="xs" style="margin-top:14px">Tu tutor puede ver que este permiso está pendiente.</p></div>
<div style="padding:12px 24px 36px;border-top:1px solid #d9d6cf;display:flex;flex-direction:column;gap:8px">{btn('Activar de nuevo','',None,True,'',chip='ext')}{btn('Volver al inicio','g',None,False)}</div>''')
def notif():
    return ph(f'''<div style="background:#1a2230;flex:1;padding:56px 16px 0;border-radius:28px;display:flex;flex-direction:column;gap:10px">
<div class="rs" style="background:#2a3446;border-radius:16px;padding:14px 16px;display:flex;gap:12px"><span style="color:#f5b455">{ic('alert')}</span><div><div style="font-size:12px;color:#b9c3d4;margin-bottom:4px">NetProtect · ahora</div><b style="display:block;font-size:15px;line-height:1.35;color:#fff">Se desactivó un permiso</b><p style="margin:2px 0 0;font-size:14px;line-height:1.4;color:#d3dae6">“Mostrar sobre otras apps” ya no está activo. Toca para activarlo de nuevo.</p></div></div>
<div class="rs" style="background:#2a3446;border-radius:16px;padding:14px 16px;display:flex;gap:12px;--d:.1s"><span style="color:#9ec0ff">{ic('shield')}</span><div><div style="font-size:12px;color:#b9c3d4;margin-bottom:4px">NetProtect · ahora</div><b style="display:block;font-size:15px;line-height:1.35;color:#fff">NetProtect está activo</b><p style="margin:2px 0 0;font-size:14px;line-height:1.4;color:#d3dae6">Reporte y control de apps funcionando en este dispositivo.</p></div></div></div>''')
def v_casos():
    b=col('1 · Bienvenida: vinculado',bienvenida1(),'Primera vez')+col('2 · Bienvenida: qué ve el tutor',bienvenida2(),'Textos por validar')+col('3 · Batería limita a NetProtect',bateria(),'Pasos por validar en Samsung')+col('4 · Permiso desactivado (en la app)',revocado())+col('5 · Aviso de Android por permiso desactivado',notif())
    save('Casos.dc.html',board('Casos reales',2400,1050,b))

# ---- Variante B: Inicio unificado con pestañas
def varianteB(tab):
    seg=lambda t,on: f'<button style="flex:1;min-height:48px;border:0;border-radius:3px;background:{"#fff" if on else "transparent"};color:{"#11243d" if on else "#5b6779"};font:600 calc(15px*var(--k))/1.2 Inter,sans-serif;box-shadow:{"0 0 0 1.5px #11243d inset" if on else "none"}">{t}</button>'
    hero=f'''<div style="background:#0d1b2a;color:#fff;padding:54px 24px 20px;flex:none"><div style="display:flex;align-items:center;gap:10px"><img src="{SH}" alt="" style="height:22px;width:auto"><span class="eye" style="color:#9fb4d4">Dispositivo supervisado</span></div>
<div style="display:flex;align-items:center;justify-content:space-between;gap:6px;margin-top:8px"><div class="rs" style="flex:1"><h1 class="disp" style="color:#fff">Falta un paso</h1><p class="sm" style="color:#d3dcea;margin:6px 0 0">Faltan 2 permisos.</p></div><div style="margin-right:-6px">{seal(['ok','ok','pe','pe'],'warn',104)}</div></div></div>'''
    tabs=f'<div style="display:flex;gap:6px;padding:6px;margin:14px 24px 0;background:#ebe8e1;border-radius:4px;flex:none">{seg("Permisos",tab=="p")}{seg("Funciones",tab=="f")}</div>'
    if tab=='p':
        body=f'<div style="padding:12px 24px 0;flex:1;overflow:hidden">'
        for i,(n,ico,d) in enumerate([PERMS[2],PERMS[3],PERMS[0],PERMS[1]]):
            ok=i>=2
            chip='<span class="chip c-ok">Configurado</span>' if ok else '<span class="chip c-pe">Pendiente</span>'
            act='' if ok else f'<div style="margin-top:10px">{btn("Activar en Ajustes","",None,True,"",chip="ext")}</div>' if i==0 else ''
            body+=f'<div class="rs" style="display:flex;gap:14px;padding:12px 0;border-bottom:1px solid #d9d6cf;--d:{.06*i:.2f}s"><span class="tile">{ic(ico)}</span><div style="flex:1;min-width:0"><div style="display:flex;flex-wrap:wrap;gap:4px 10px;align-items:center"><b class="bd">{n}</b>{chip}</div>{act}</div></div>'
        body+='</div>'
    else:
        def f(ico,n,chip,d): return f'<div class="rs" style="display:flex;gap:14px;padding:12px 0;border-bottom:1px solid #d9d6cf"><span class="tile">{ic(ico)}</span><div style="flex:1;min-width:0"><div style="display:flex;flex-wrap:wrap;gap:4px 10px;align-items:center;justify-content:space-between"><b class="bd">{n}</b>{chip}</div><div class="sm">{d}</div></div></div>'
        ok='<span class="chip c-ok">Activo</span>'
        body='<div style="padding:12px 24px 0;flex:1;overflow:hidden">'+f('wifi','Reporte del dispositivo',ok,'Último reporte: hace 1 min.')+f('grid','Control de apps','<span class="chip c-pe">Requiere permiso</span>','Falta “Mostrar sobre otras apps” para bloquear.')+f('pin','Ubicación aproximada',ok,'Cada unos 15 minutos.')+'</div>'
    bar=f'<div style="padding:10px 24px 0;border-top:1px solid #d9d6cf;flex:none"><div style="display:flex;align-items:center;justify-content:space-between;min-height:52px;font-weight:500"><span style="display:flex;align-items:center;gap:12px">{ic("sliders",20)}Más ajustes</span>{ic("chev",20,"color:#5b6779")}</div></div><div style="height:30px;flex:none"></div>'
    return ph(hero+tabs+body+bar)
def v_varianteb():
    b=col('Variante B · pestaña Permisos',varianteB('p'),'Prueba A/B: una sola pantalla')+col('Variante B · pestaña Funciones',varianteB('f'),'Prueba A/B: una sola pantalla')
    save('Variante.dc.html',board('Variante B: Inicio unificado',980,1050,b))

# ---- Plan de prueba
def prueba():
    rows=[('1','Vincular el teléfono con un código de 6 dígitos','Completa sin ayuda; tiempo; errores al escribir'),
    ('2','Entender por qué dice “Falta un paso” y resolverlo','Encuentra el permiso pendiente en menos de 20 s'),
    ('3','Responder a una solicitud de vista remota y luego detenerla','Entiende qué verá su tutor; encuentra “Detener” sin ayuda'),
    ('4','Explicar con sus palabras qué ve el tutor y qué no','Responde bien “¿Puede grabar tu pantalla?” (según lo validado)'),
    ('5','Recuperarse tras desactivar un permiso en Ajustes','Llega a “Activar de nuevo” desde el aviso'),
    ('6','Encontrar cómo cerrar sesión y entender que no desvincula','Lo encuentra en Más ajustes; no confunde con desvincular'),
    ('7','Repetir 2 y 3 con letra al 200 % y TalkBack','Sin texto cortado; orden de lectura lógico')]
    tr=''.join(f'<tr><td class="n">{a}</td><td>{b}</td><td class="s">{c}</td></tr>' for a,b,c in rows)
    return f'''<style>.pg{{position:relative;width:1440px;height:1180px;background:#f5f3ee;box-sizing:border-box;padding:56px 64px;font-family:Inter,sans-serif;color:#11243d;overflow:hidden}}
.pg h1{{margin:8px 0 8px;font:600 44px/50px 'Playfair Display',serif}}.pg table{{border-collapse:collapse;width:100%}}.pg th{{font:600 11px/16px Inter;letter-spacing:.14em;text-transform:uppercase;color:#5b6779;text-align:left;padding:0 12px 8px 0;border-bottom:1px solid #11243d}}.pg td{{padding:10px 16px 10px 0;border-bottom:1px solid #d9d6cf;font-size:14px;line-height:20px;vertical-align:top}}.pg td.n{{width:30px;font-weight:600;color:#5b6779}}.pg td.s{{color:#5b6779;width:480px}}.cols{{display:grid;grid-template-columns:1fr 1fr 1fr;gap:40px;margin-top:28px}}.cols h2{{margin:0 0 10px;font:600 12px/16px Inter;letter-spacing:.16em;text-transform:uppercase;color:#5b6779;padding-bottom:8px;border-bottom:1px solid #d9d6cf}}.cols p{{margin:0 0 8px;font-size:14px;line-height:21px}}</style>
<div class="pg"><div class="eye">Validación con usuarios</div><h1>Plan de prueba: 5 personas, 25 minutos</h1>
<p class="bd" style="margin:0 0 22px;color:#5b6779;max-width:860px">Objetivo: confirmar que cualquier persona entiende el estado, completa los permisos y comprende qué ve su tutor. Se prueba un prototipo clicable en un Android real, con pensamiento en voz alta.</p>
<table><tr><th>#</th><th>Tarea</th><th>Qué cuenta como éxito</th></tr>{tr}</table>
<div class="cols">
<div><h2>Quiénes</h2><p>3 adolescentes y 2 tutores. Para menores, consentimiento previo de su tutor y de ellos.</p><p>Incluir a una persona que use letra grande o TalkBack.</p></div>
<div><h2>Qué medimos</h2><p>Éxito sin ayuda (meta: 4 de 5 en cada tarea). Tiempo por tarea. Errores. Comprensión de privacidad. Escala SUS al final.</p></div>
<div><h2>Qué decide cada resultado</h2><p>Variante A (Inicio con dos accesos) contra Variante B (una pantalla con pestañas): gana la que complete más tareas sin ayuda.</p><p>Si falla la tarea 4, se reescribe Privacidad antes de construir.</p></div></div></div>'''
def v_prueba():
    html=f'''<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Plan de prueba</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Playfair+Display:ital,wght@0,600;1,400&display=swap" rel="stylesheet">
<style>{CSS}</style>
</helmet>
{prueba()}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":1440,"height":1180}}}}'>
class Component extends DCLogic {{
renderVals() {{ return {{}}; }}
}}
</script>
</body>
</html>
'''
    save('Prueba.dc.html',html)
if __name__=='__main__':
    v_casos();v_varianteb();v_prueba()
