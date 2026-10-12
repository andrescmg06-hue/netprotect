import sys; sys.path.insert(0,'/tmp/gen')
from lib import *

def inicio(state='ok',overlay='',k='',banner=False):
    cfg={
     'ok':dict(st=['ok']*4,mode='ok',h='Todo funciona',sub='Los 4 permisos están configurados y el dispositivo se comunicó hace un momento.',cnt='4 de 4'),
     'warn':dict(st=['ok','ok','pe','pe'],mode='warn',h='Falta un paso',sub='Faltan 2 permisos para que NetProtect funcione bien en este teléfono.',cnt='2 de 4'),
     'off':dict(st=['ok']*4,mode='off',h='Sin reporte reciente',sub='No recibimos una actualización reciente del dispositivo. El vínculo sigue activo.',cnt='4 de 4'),
    }[state]
    sealst=cfg['st'] if state!='off' else ['off']*4
    ban=f'<div style="background:#1769ff;color:#fff;padding:42px 14px 10px 24px;display:flex;align-items:center;gap:12px;flex:none;animation:fade .4s var(--e)"><span class="dot pl" style="background:#fff"></span><b style="flex:1;font-size:15px;line-height:1.3">Tu tutor está viendo tu pantalla</b><button style="min-height:48px;padding:0 18px;border-radius:4px;border:1.5px solid #fff;background:transparent;color:#fff;font:600 15px/1 Inter,sans-serif">Detener</button></div>' if banner else ''
    heroPad='20px' if banner else '54px'
    cta=f'<div style="margin-top:18px">{btn("Revisar permisos")}</div>' if state=='warn' else ''
    comm='Hace un momento' if state!='off' else 'Hace 12 min'
    commc='#0b7a42' if state!='off' else '#5b6779'
    commd='<span class="dot" style="background:#16b364"></span>' if state!='off' else '<span class="dot" style="background:#8a8678"></span>'
    commt='Conectado' if state!='off' else 'Sin actualización reciente'
    hero=f'''<div style="background:#0d1b2a;color:#fff;padding:{heroPad} 24px 22px;flex:none;position:relative;overflow:hidden">
<div style="display:flex;align-items:center;gap:10px"><img src="{SH}" alt="" style="height:22px;width:auto"><span class="eye" style="color:#9fb4d4">Dispositivo supervisado</span></div>
<div style="display:flex;align-items:center;justify-content:space-between;gap:6px;margin-top:10px">
<div style="flex:1;min-width:0" class="rs"><h1 class="disp" style="color:#fff">{cfg['h']}</h1></div>
<div style="position:relative;margin-right:-6px;flex:none;text-align:center">{seal(sealst,cfg['mode'],128)}<div class="eye" style="color:#9fb4d4;margin-top:-6px">{cfg['cnt']} permisos</div></div>
</div>
<p class="rs bd" style="margin:6px 0 0;color:#d3dcea;font-size:calc(15px*var(--k));--d:.15s">{cfg['sub']}</p>{cta}
</div>'''
    strip=f'''<div style="padding:0 24px;flex:none">
<div style="display:grid;grid-template-columns:1fr 1fr;border-bottom:1px solid #d9d6cf" class="rs">
<div style="padding:14px 12px 14px 0"><div class="xs">Dispositivo</div><div class="b6 bd">samsung SM-S731B</div><div class="sm">Android 16</div></div>
<div style="padding:14px 0 14px 14px;border-left:1px solid #d9d6cf"><div class="xs">Supervisado por</div><div class="b6 bd">Andres Mosquera</div><div class="sm" style="display:flex;align-items:center;gap:6px"><span class="dot" style="background:#16b364"></span>Vinculado</div></div>
</div>
<div class="rs" style="padding:12px 0;border-bottom:1px solid #d9d6cf;display:flex;justify-content:space-between;align-items:center;gap:10px;--d:.1s"><div><div class="xs">Última comunicación</div><div class="b6 bd">{comm}</div></div><span style="display:inline-flex;align-items:center;gap:8px;font-size:calc(14px*var(--k));color:{commc}">{commd}{commt}</span></div>
</div>'''
    pc='#0b7a42' if state!='warn' else '#9a5000'
    ptxt='Todos configurados' if state!='warn' else '2 pendientes'
    pico='check' if state!='warn' else 'alert'
    panels=f'''<div style="display:grid;grid-template-columns:1fr 1fr;margin:14px 24px 0;border-top:1.5px solid #11243d;flex:none" class="rs" >
<button style="all:unset;box-sizing:border-box;cursor:pointer;padding:16px 12px 14px 0;min-height:176px;display:flex;flex-direction:column;gap:8px;position:relative">{ic('lock',26,'color:#11243d')}<span class="b6 bd" style="margin-top:auto">Permisos</span><span class="sm" style="color:{pc};display:flex;align-items:center;gap:6px;font-weight:500">{ic(pico,16)}{ptxt}</span><span style="position:absolute;right:10px;top:16px;color:#11243d">{ic('arrow',20)}</span></button>
<button style="all:unset;box-sizing:border-box;cursor:pointer;padding:16px 0 14px 14px;border-left:1px solid #d9d6cf;min-height:176px;display:flex;flex-direction:column;gap:8px;position:relative">{ic('wifi' if state!='off' else 'wifioff',26,'color:#11243d')}<span class="b6 bd" style="margin-top:auto">Estado de NetProtect</span><span class="sm">Funciones activas</span><span style="position:absolute;right:0;top:16px;color:#11243d">{ic('arrow',20)}</span></button>
</div>'''
    more=f'''<div style="margin:0 24px;border-top:1px solid #d9d6cf;display:flex;align-items:center;justify-content:space-between;min-height:52px;flex:none" class="rs"><span style="display:flex;align-items:center;gap:12px;font-weight:500">{ic('shield',20)}Privacidad</span>{ic('chev',20,'color:#5b6779')}</div>
<div style="margin:0 24px 30px;border-top:1px solid #d9d6cf;border-bottom:1px solid #d9d6cf;display:flex;align-items:center;justify-content:space-between;min-height:52px;flex:none" class="rs"><span style="display:flex;align-items:center;gap:12px;font-weight:500">{ic('sliders',20)}Más ajustes</span>{ic('chev',20,'color:#5b6779')}</div>'''
    return ph(f'{ban}{hero}{strip}{panels}<div style="flex:1;min-height:6px"></div>{more}{overlay}',k)

def v_vinculado():
    b=col('1 · Todo funciona',inicio('ok'))+col('2 · Falta un paso',inicio('warn'))+col('3 · Sin reporte reciente',inicio('off'))+col('4 · Vista remota en curso',inicio('ok',banner=True))
    save('Vinculado.dc.html',board('Dispositivo vinculado',1900,1050,b))

# ---------- Permisos (rail)
PERMS=[('Acceso al uso de apps','grid','Permite saber cuánto tiempo se usan las apps y aplicar las reglas de tu tutor.'),
('Ubicación aproximada','pin','Permite reportar la última ubicación conocida. No se usa la ubicación precisa.'),
('Mostrar sobre otras apps','monitor','Necesario para mostrar la pantalla de bloqueo cuando corresponda.'),
('Protección contra desinstalación','shield','Avisa a tu tutor si alguien intenta desinstalar NetProtect.')]
def rail(states):
    # states: list of 'ok'/'pe' per perm; first pending is 'next'
    order=[i for i,s in enumerate(states) if s!='ok']+[i for i,s in enumerate(states) if s=='ok']
    out='';nxt=True;n=0
    for idx,i in enumerate(order):
        name,icon,desc=PERMS[i];s=states[i]
        last=idx==len(order)-1
        d=f'--d:{.08*idx+.1:.2f}s'
        line='' if last else f'<span style="position:absolute;left:13px;top:34px;bottom:-4px;width:2px;background:{"#16b364" if s=="ok" else "#d9d6cf"}"></span>'
        if s=='ok':
            node=f'<span style="position:absolute;left:0;top:2px;width:28px;height:28px;border-radius:50%;background:#0b7a42;color:#fff;display:flex;align-items:center;justify-content:center">{ic("check",16,"stroke-width:2.6")}</span>'
            chip='<span class="chip c-ok">Configurado</span>'
            body=f'<div class="sm" style="margin-top:2px">{desc}</div>'
        elif nxt:
            nxt=False
            node=f'<span style="position:absolute;left:0;top:2px;width:28px;height:28px;border-radius:50%;border:2.5px solid #9a5000;background:#fef3e2;display:flex;align-items:center;justify-content:center;box-sizing:border-box"><span class="dot pu" style="background:#9a5000"></span></span>'
            chip='<span class="chip c-pe">Pendiente · siguiente</span>'
            body=f'<div class="sm" style="margin:2px 0 12px">{desc}</div>{btn("Activar en Ajustes","",None,True,"",chip="ext")}'
        else:
            node='<span style="position:absolute;left:0;top:2px;width:28px;height:28px;border-radius:50%;border:2px dashed #8a8678;box-sizing:border-box"></span>'
            chip='<span class="chip c-pe">Pendiente</span>'
            body=f'<div class="sm" style="margin-top:2px">{desc}</div>'
        out+=f'<div class="rs" style="position:relative;padding:0 0 {18 if not last else 4}px 48px;{d}">{line}{node}<div style="display:flex;flex-wrap:wrap;align-items:center;gap:4px 10px"><b class="bd">{name}</b>{chip}</div>{body}</div>'
    return out
def permisos(states,k=''):
    done=sum(1 for s in states if s=='ok')
    seg=''.join(f'<i style="flex:1;height:5px;border-radius:1px;background:{"#0b7a42" if s=="ok" else "#d9d6cf"}"></i>' for s in states)
    if done==4:
        head=f'<div class="b6" style="color:#0b7a42;display:flex;align-items:center;gap:8px;font-size:calc(15px*var(--k))">{ic("check",18)}Todos los permisos están configurados</div>'
        bar=f'<div style="flex:none;border-top:1px solid #d9d6cf;padding:14px 24px 36px;display:flex;flex-direction:column;gap:10px">{btn("Volver al inicio","",None,True,"",chip="home")}{tl("Verificar de nuevo","refresh")}</div>'
        note='<p class="xs" style="margin:6px 0 0 48px">Si revocas un permiso en Ajustes, lo comprobamos al volver a la app.</p>'
    else:
        head=f'<div class="b6" style="color:#9a5000;font-size:calc(15px*var(--k))">{done} de 4 configurados</div>'
        bar=f'<div style="flex:none;border-top:1px solid #d9d6cf;padding:14px 24px 36px">{btn("Ya lo activé, verificar de nuevo","o",None,True,"",chip="refresh")}</div>'
        note=''
    inner=f'''{top()}
<div style="padding:6px 24px 0;flex:none"><h1 class="disp rs">Permisos</h1><p class="sm rs" style="margin:6px 0 14px;--d:.05s">NetProtect necesita estos cuatro permisos en este teléfono.</p>{head}<div style="display:flex;gap:5px;margin-top:8px">{seg}</div></div>
<div style="padding:22px 24px 0;flex:1;overflow:hidden">{rail(states)}{note}</div>{bar}'''
    return ph(inner,k)
def v_permisos():
    b=col('1 · Dos permisos pendientes',permisos(['ok','ok','pe','pe']))+col('2 · Todos configurados',permisos(['ok']*4))
    save('Permisos.dc.html',board('Permisos del dispositivo',980,1050,b))

# ---------- Estado
def ecg():
    return '<svg viewBox="0 0 120 28" width="120" height="28" aria-hidden="true" style="display:block"><path d="M0 14h26l5-10 6 20 5-14 4 4h14l4-7 5 13 4-6h17" fill="none" stroke="#16b364" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="240" style="animation:ecg 2.4s linear infinite"/><path d="M0 14h26l5-10 6 20 5-14 4 4h14l4-7 5 13 4-6h17" fill="none" stroke="#d9d6cf" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity=".6" /></svg>'
def frow(icon,name,chip,desc,extra='',i=0,last=False):
    bb='' if last else 'border-bottom:1px solid #d9d6cf;'
    return f'<div class="rs" style="display:flex;gap:16px;padding:12px 0;{bb}--d:{.08*i+.1:.2f}s"><div class="tile">{ic(icon)}</div><div style="flex:1;min-width:0"><div style="display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:4px 10px"><b class="bd">{name}</b>{chip}</div><div class="sm" style="margin-top:2px">{desc}</div>{extra}</div></div>'
def estado(live=False,k=''):
    okc='<span class="chip c-ok"><span class="dot" style="background:#16b364"></span>Activo</span>'
    liveblk=f'''<div class="rs" style="margin:10px -24px 2px;padding:14px 24px 16px;background:#0d1b2a;color:#fff"><div style="display:flex;align-items:center;gap:12px"><span style="position:relative;width:44px;height:44px;display:flex;align-items:center;justify-content:center;flex:none"><span class="rg" style="position:absolute;inset:0;border-radius:50%;border:2px solid #6ea0ff;transform-origin:center"></span>{ic('monitor',24,'color:#fff')}</span><div><div class="eye" style="color:#9fb4d4">Visualización de pantalla</div><div class="b6 bd">En curso</div></div></div><p class="sm" style="margin:6px 0 10px;color:#d3dcea">Tu tutor está viendo tu pantalla ahora.</p><button style="width:100%;min-height:54px;border-radius:4px;border:0;background:#fff;color:#0d1b2a;font:600 calc(16px*var(--k))/1.2 Inter,sans-serif;display:flex;align-items:center;justify-content:center;gap:10px">{ic('stop',20)}Detener ahora</button></div>''' if live else ''
    rows=frow('wifi','Reporte del dispositivo',okc,'Envía el estado del dispositivo a tu tutor.',f'<div class="sm" style="display:flex;align-items:center;gap:8px;margin-top:8px;color:#11243d;font-weight:500">{ic("clock",16)}Último reporte: hace 1 min</div><div class="xs" style="margin-top:2px">Cada minuto mientras la app esté abierta.</div>',0)
    rows+=frow('grid','Control de apps',okc,'Aplica las reglas de apps de tu tutor.','',1)
    if live:
        rows+=frow('pin','Ubicación aproximada','<span class="chip c-pe">Sin confirmar</span>','Todavía no recibimos una ubicación. Llega a tu tutor cada unos 15 minutos.','',2)
    else:
        rows+=frow('pin','Ubicación aproximada',okc,'Comparte la ubicación aproximada. Llega a tu tutor cada unos 15 minutos.','',2)
    rows+=frow('bell','Notificaciones permanentes','','Android muestra un aviso mientras una función está activa.','',3,True)
    inner=f'''{top()}
<div style="padding:6px 24px 0;flex:1;overflow:hidden"><h1 class="disp rs">Estado de NetProtect</h1><p class="sm rs" style="margin:6px 0 4px;--d:.05s">Qué funciones están activas en este teléfono.</p>{liveblk}{rows}</div>
<div style="flex:none;border-top:1px solid #d9d6cf;padding:12px 24px 36px;display:flex;flex-direction:column;gap:10px"><div class="xs" style="text-align:center">NetProtect no graba la pantalla ni controla el dispositivo.</div>{btn("Volver al inicio","",None,True,"",chip="home")}</div>'''
    return ph(inner,k)
def v_estado():
    b=col('1 · Todo activo',estado(False))+col('2 · Vista remota en curso',estado(True))
    save('Estado.dc.html',board('Estado de NetProtect',980,1050,b))
if __name__=='__main__':
    v_vinculado();v_permisos();v_estado()
