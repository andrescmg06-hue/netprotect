import sys; sys.path.insert(0,'/tmp/gen')
from lib import *
from b1 import inicio, permisos, estado
def dlg(h,p,b1,b2): return f'<div class="scrim"><div class="dlg" role="alertdialog"><h2>{h}</h2><p>{p}</p>{b1}<div style="height:10px"></div>{b2}</div></div>'
def hold(label):
    return f'<button class="btn dg c" style="position:relative;overflow:hidden"><span style="position:absolute;left:0;top:0;bottom:0;width:100%;background:#8f1b13;transform-origin:left;transform:scaleX(0);animation:fillh 3.2s var(--e) infinite"></span><span style="position:relative">{label}</span></button>'
def mas(overlay=''):
    def row(i_,t,s='',c='#11243d'): return f'<div class="rs" style="display:flex;align-items:center;gap:14px;min-height:64px;border-bottom:1px solid #d9d6cf;color:{c}">{ic(i_,22)}<div style="flex:1"><div class="b6 bd">{t}</div>{f"<div class=sm>{s}</div>" if s else ""}</div>{ic("chev",20,"color:#5b6779")}</div>'
    return ph(f'''{top()}<div style="padding:8px 24px 0;flex:1"><h1 class="disp rs">Más ajustes</h1><p class="sm" style="margin:6px 0 14px">Opciones de este teléfono.</p>
<div style="border-top:1px solid #d9d6cf">{row('shield','Privacidad','Qué hace NetProtect y qué no.')}{row('swap','Cambiar de modo','Por validar con el equipo técnico.')}{row('logout','Cerrar sesión','Salir de tu cuenta en este teléfono.')}</div>
<div class="rs" style="margin-top:22px;padding:14px 16px;background:#eaf1ff;border-radius:4px;display:flex;gap:12px;color:#11243d">{ic('info',20,'color:#1456d9;margin-top:2px')}<div class="bd" style="font-size:calc(15px*var(--k))">Para desvincular este dispositivo, pídeselo a tu tutor. Solo él puede hacerlo.</div></div></div>{overlay}''')
def decisiones():
    d1=dlg('¿Cerrar sesión?','Saldrás de tu cuenta en este teléfono. Para volver a entrar tendrás que iniciar sesión otra vez.',btn('Cerrar sesión','',None,False),btn('Cancelar','g',None,False))
    modo=ph(f'''{top()}<div style="padding:8px 24px 0;flex:1"><h1 class="disp rs">Cambiar de modo</h1><p class="bd rs" style="margin:8px 0 18px;color:#5b6779">Elige cómo usar NetProtect en este teléfono.</p>
<div style="border:2px solid #1769ff;border-radius:4px;padding:16px;display:flex;gap:14px;background:#fff"><span style="width:24px;height:24px;border:7px solid #1769ff;border-radius:50%;box-sizing:border-box;flex:none;margin-top:2px"></span><div><b class="bd">Dispositivo supervisado</b><div class="sm">Modo actual. Este teléfono está vinculado a tu tutor.</div></div></div>
<div style="border:1.5px solid #c9c5bb;border-radius:4px;padding:16px;display:flex;gap:14px;margin-top:10px"><span style="width:24px;height:24px;border:1.5px solid #8a8678;border-radius:50%;box-sizing:border-box;flex:none;margin-top:2px;background:#fff"></span><div><b class="bd">Modo normal</b><div class="sm">La supervisión dejará de estar activa en este teléfono.</div></div></div>
<p class="xs" style="margin-top:16px">Antes de confirmar verás qué funciones dejarán de estar activas.</p></div>
<div style="padding:12px 24px 36px;border-top:1px solid #d9d6cf"><div class="xs" style="text-align:center;margin-bottom:8px">Elige una opción para continuar.</div>{btn('Continuar','off',None,True,'',chip='check')}</div>''')
    desv=ph(f'''<div style="flex:1;display:flex;flex-direction:column;padding:96px 24px 0"><div style="position:relative;width:128px;height:128px">{seal(['off']*4,'off',128)}<span style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:#11243d">{ic('unlink',30)}</span></div>
<div class="eye" style="margin-top:26px">Vínculo terminado</div><h1 class="disp rs" style="margin-top:8px">Este dispositivo ya no está vinculado</h1>
<p class="bd rs" style="margin:12px 0 0;color:#5b6779;--d:.1s">Tu tutor terminó el vínculo. Si fue un error, pídele un código nuevo para vincularlo otra vez.</p></div>
<div style="padding:12px 24px 36px;border-top:1px solid #d9d6cf;display:flex;flex-direction:column;gap:8px">{btn('Vincular de nuevo','',None)}{btn('Cerrar sesión','g',None,False)}</div>''')
    def pt(t,s,last=False): return f'<div class="rs" style="padding:14px 0;{"" if last else "border-bottom:1px solid #d9d6cf;"}"><b class="bd">{t}</b><div class="sm">{s}</div></div>'
    priv=ph(f'''{top()}<div style="padding:8px 24px 0;flex:1"><h1 class="disp rs">Privacidad</h1><p class="sm" style="margin:6px 0 10px">Qué hace NetProtect en este teléfono.</p>
<div class="eye" style="border-bottom:1px solid #11243d;padding-bottom:6px;margin-top:10px">Qué hace</div>
{pt('Informa el estado del dispositivo','Cada minuto mientras la app esté abierta.')}{pt('Aplica las reglas de apps de tu tutor','Puede bloquear apps y mostrar el motivo.')}{pt('Comparte tu ubicación aproximada','Llega a tu tutor cada unos 15 minutos, no en tiempo real.',True)}
<div class="eye" style="border-bottom:1px solid #11243d;padding-bottom:6px;margin-top:16px">Vista de pantalla</div>{pt('Solo con tu permiso','Tu tutor puede pedírtela. Tú decides si aceptas y puedes detenerla cuando quieras.',True)}
<p class="xs" style="margin-top:12px">Sobre grabación y almacenamiento de la pantalla: texto pendiente de validar con la implementación.</p></div>''')
    b=col('1 · Más ajustes',mas(),'Desvincular solo por el tutor: decisión propuesta')+col('2 · Confirmar cierre de sesión',mas(dlg('¿Cerrar sesión?','Saldrás de tu cuenta en este teléfono. Para volver a entrar tendrás que iniciar sesión otra vez.',btn('Cerrar sesión','',None,False),btn('Cancelar','g',None,False))),'Confirmación breve')+col('3 · Cambiar de modo',modo,'Pantalla por validar')+col('4 · Dispositivo desvinculado por el tutor',desv,'Qué queda en el teléfono: por validar')+col('5 · Privacidad',priv,'Textos de grabación: por validar')
    html=board('Sesión, vínculo y modo',2400,1050,b)
    save('Decisiones.dc.html',html)
def letra():
    b=col('Dispositivo vinculado · 200 %',inicio('warn',k='k2'))+col('Permisos · 200 %',permisos(['ok','ok','pe','pe'],k='k2'))+col('Estado de NetProtect · 200 %',estado(True,k='k2'))
    save('Letra200.dc.html',board('Letra al 200 %',1440,2260,b))
if __name__=='__main__':
    decisiones();letra()
