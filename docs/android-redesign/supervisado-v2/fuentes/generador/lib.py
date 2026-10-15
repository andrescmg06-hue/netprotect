import re
OUT='/tmp/claude-0/-home-claude/bc9477f9-b2ab-524b-b2ce-9551973f53b4/scratchpad/dc/project/'
SH='/_blob/3f5387e827e7fdd88ecb416ef79ce2ab'
LOGO='/_blob/d58222f4d0c52cef0b34d8a2b62ddef7'
PHOTO='/_blob/de71e030645f005d90919c407805cfd7'
CSS='''
body{margin:0}
:root{--k:1;--kd:1;--e:cubic-bezier(.2,0,0,1)}
.ph{position:relative;width:411px;height:891px;background:#f5f3ee;overflow:hidden;font-family:Inter,sans-serif;color:#11243d;display:flex;flex-direction:column;border:1px solid #c9c5bb;border-radius:28px;box-sizing:border-box;font-size:calc(16px*var(--k));line-height:1.5}
.ph.k2{--k:2;--kd:1.4;height:auto;min-height:891px}
.cap{font:600 12px/16px Inter,sans-serif;letter-spacing:.14em;text-transform:uppercase;color:#5b6779;margin-bottom:12px}
.ic{width:22px;height:22px;stroke:currentColor;fill:none;stroke-width:1.75;stroke-linecap:round;stroke-linejoin:round;flex:none}
.gest{position:absolute;bottom:8px;left:50%;margin-left:-60px;width:120px;height:4px;border-radius:2px;background:#11243d;opacity:.45;z-index:9}
.disp{font:600 calc(38px*var(--kd))/1.08 'Playfair Display',serif;letter-spacing:-.01em;margin:0}
.ttl{font:600 calc(26px*var(--kd))/1.15 'Playfair Display',serif;margin:0}
.bd{font-size:calc(16px*var(--k));line-height:1.5}
.sm{font-size:calc(14px*var(--k));line-height:1.5;color:#5b6779}
.xs{font-size:calc(14px*var(--k));line-height:1.45;color:#5b6779}
.eye{font:600 calc(11px*var(--k))/1.4 Inter,sans-serif;letter-spacing:.14em;text-transform:uppercase;color:#5b6779}
.b6{font-weight:600}
.hr{border-top:1px solid #d9d6cf}
.top{padding:46px 14px 0;display:flex;align-items:center;justify-content:space-between;flex:none}
.bk{width:48px;height:48px;display:flex;align-items:center;justify-content:center;background:none;border:0;color:#11243d;border-radius:50%;transition:background .2s}
.bk:hover,.bk:active{background:#e9e6df}
.btn{position:relative;overflow:hidden;display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:60px;padding:6px 8px 6px 22px;border-radius:4px;font:600 calc(16px*var(--k))/1.3 Inter,sans-serif;border:0;box-sizing:border-box;width:100%;background:#1769ff;color:#fff;text-align:left;transition:transform .12s var(--e),background .2s}
.btn:active{transform:scale(.985)}
.btn .ar{width:46px;height:46px;border-radius:3px;background:rgba(255,255,255,.18);display:flex;align-items:center;justify-content:center;flex:none;transition:transform .24s var(--e)}
.btn:hover .ar{transform:translateX(3px)}
.btn.o{background:#fff;color:#11243d;box-shadow:inset 0 0 0 1.5px #11243d}
.btn.o .ar{background:#11243d;color:#fff}
.btn.g{background:transparent;color:#11243d;box-shadow:inset 0 0 0 1.5px #c9c5bb}
.btn.g .ar{background:#ebe8e1}
.btn.off{background:#e4e1da;color:#5b6779}
.btn.off .ar{background:#d9d6cf}
.btn.dg{background:#c4271c}
.btn.c{justify-content:center;padding:6px 22px}
.tl{display:inline-flex;align-items:center;gap:10px;min-height:48px;background:none;border:0;padding:0 2px;font:600 calc(15px*var(--k))/1.3 Inter,sans-serif;color:#1456d9;background-image:linear-gradient(currentColor,currentColor);background-repeat:no-repeat;background-position:0 calc(50% + 14px);background-size:0 1.5px;transition:background-size .3s var(--e)}
.tl:hover,.tl:active{background-size:100% 1.5px}
.chip{display:inline-flex;align-items:center;gap:7px;font:600 calc(13px*var(--k))/1.3 Inter,sans-serif;padding:3px 10px;border-radius:999px}
.c-ok{color:#0b7a42;background:#e7f8ef}.c-pe{color:#9a5000;background:#fef3e2}.c-ve{color:#1456d9;background:#eaf1ff}.c-gr{color:#4a5568;background:#ebe8e1}.c-er{color:#c4271c;background:#feeceb}
.dot{width:9px;height:9px;border-radius:50%;flex:none}
.rs{animation:rise .55s var(--e) both;animation-delay:var(--d,0s)}
.msg{display:flex;gap:12px;align-items:flex-start;padding:14px 16px;border-radius:4px;font-size:calc(15px*var(--k));line-height:1.45}
.tile{width:44px;height:44px;border-radius:4px;background:#f3f1ec;border:1px solid #d9d6cf;display:flex;align-items:center;justify-content:center;color:#11243d;flex:none;box-sizing:border-box}
.scrim{position:absolute;inset:0;background:rgba(13,27,42,.58);z-index:5;display:flex;align-items:flex-end;padding:0 18px 52px;animation:fade .3s var(--e)}
.dlg{background:#f5f3ee;border-radius:4px;padding:28px 22px 18px;width:100%;box-sizing:border-box;animation:rise .45s var(--e) both .05s}
.dlg h2{margin:0 0 10px;font:600 26px/1.15 'Playfair Display',serif}
.dlg p{margin:0 0 20px;font-size:15px;line-height:1.5;color:#5b6779}
.pv{display:inline-block;margin:0 0 12px;font:600 11px/16px Inter,sans-serif;letter-spacing:.08em;text-transform:uppercase;color:#9a5000;background:#fef3e2;padding:2px 8px;border-radius:2px}
@keyframes rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}
@keyframes fade{from{opacity:0}to{opacity:1}}
@keyframes draw{from{stroke-dashoffset:92}to{stroke-dashoffset:0}}
@keyframes ring{0%{transform:scale(1);opacity:.55}100%{transform:scale(1.5);opacity:0}}
@keyframes breathe{0%,100%{transform:scale(1)}50%{transform:scale(1.05)}}
@keyframes sheen{0%,70%{left:-80px}100%{left:120%}}
@keyframes blink{0%,100%{opacity:1}50%{opacity:0}}
@keyframes pulse{0%,100%{opacity:.35}50%{opacity:1}}
@keyframes spin{to{transform:rotate(360deg)}}
@keyframes scan{0%{transform:translateX(-100%)}100%{transform:translateX(320%)}}
@keyframes kb{from{transform:scale(1)}to{transform:scale(1.08)}}
@keyframes ecg{from{stroke-dashoffset:240}to{stroke-dashoffset:0}}
@keyframes tick{from{stroke-dashoffset:30}to{stroke-dashoffset:0}}
@keyframes shake{0%,100%{transform:translateX(0)}20%{transform:translateX(-6px)}40%{transform:translateX(6px)}60%{transform:translateX(-4px)}80%{transform:translateX(3px)}}
@keyframes hop{0%{left:0;opacity:0}15%{opacity:1}85%{opacity:1}100%{left:calc(100% - 10px);opacity:0}}
@keyframes shrink{from{transform:scaleX(1)}to{transform:scaleX(0)}}
@keyframes shackle{0%{transform:translateY(-5px)}60%{transform:translateY(-5px)}80%{transform:translateY(1px)}100%{transform:translateY(0)}}
.arc{animation:draw 1s var(--e) both}
.a1{animation-delay:.1s}.a2{animation-delay:.3s}.a3{animation-delay:.5s}.a4{animation-delay:.7s}
.rg{transform-origin:84px 84px;animation:ring 2.8s ease-out infinite}
.bre{animation:breathe 3.2s ease-in-out infinite}
.pl{animation:pulse 1.8s ease-in-out infinite}
.sp{animation:spin 1s linear infinite}
@media (prefers-reduced-motion:reduce){.rs,.arc,.rg,.bre,.pl,.sp,.scrim,.dlg{animation:none!important}.btn,.btn .ar,.tl{transition:none}}
'''
SYM={
'back':'<path d="m12 19-7-7 7-7"/><path d="M19 12H5"/>',
'arrow':'<path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>',
'chev':'<path d="m9 18 6-6-6-6"/>',
'check':'<path d="M20 6 9 17l-5-5"/>',
'alert':'<circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/>',
'wifi':'<path d="M12 20h.01"/><path d="M2 8.82a15 15 0 0 1 20 0"/><path d="M5 12.859a10 10 0 0 1 14 0"/><path d="M8.5 16.429a5 5 0 0 1 7 0"/>',
'wifioff':'<path d="M12 20h.01"/><path d="M8.5 16.429a5 5 0 0 1 7 0"/><path d="M5 12.859a10 10 0 0 1 5.17-2.69"/><path d="M19 12.859a10 10 0 0 0-2.007-1.523"/><path d="M2 8.82a15 15 0 0 1 4.177-2.643"/><path d="M22 8.82a15 15 0 0 0-11.288-3.764"/><path d="m2 2 20 20"/>',
'grid':'<rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/>',
'pin':'<path d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"/><circle cx="12" cy="10" r="3"/>',
'monitor':'<rect width="20" height="14" x="2" y="3" rx="2"/><line x1="8" x2="16" y1="21" y2="21"/><line x1="12" x2="12" y1="17" y2="21"/>',
'shield':'<path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/>',
'refresh':'<path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/>',
'home':'<path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
'lock':'<rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
'logout':'<path d="m16 17 5-5-5-5"/><path d="M21 12H9"/><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>',
'unlink':'<path d="m18.84 12.25 1.72-1.71a5.004 5.004 0 0 0-.12-7.07 5.006 5.006 0 0 0-6.95 0l-1.72 1.71"/><path d="m5.17 11.75-1.71 1.71a5.004 5.004 0 0 0 .12 7.07 5.006 5.006 0 0 0 6.95 0l1.71-1.71"/><line x1="8" x2="8" y1="2" y2="5"/><line x1="2" x2="5" y1="8" y2="8"/><line x1="16" x2="16" y1="19" y2="22"/><line x1="19" x2="22" y1="16" y2="16"/>',
'swap':'<path d="m16 3 4 4-4 4"/><path d="M20 7H4"/><path d="m8 21-4-4 4-4"/><path d="M4 17h16"/>',
'eye':'<path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0"/><circle cx="12" cy="12" r="3"/>',
'clock':'<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
'info':'<circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>',
'stop':'<circle cx="12" cy="12" r="10"/><rect width="6" height="6" x="9" y="9"/>',
'nog':'<circle cx="12" cy="12" r="10"/><path d="m4.9 4.9 14.2 14.2"/>',
'spin':'<path d="M21 12a9 9 0 1 1-6.219-8.56"/>',
'app':'<rect width="18" height="18" x="3" y="3" rx="3"/><path d="M3 9h18"/>',
'cal':'<path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/>',
'ext':'<path d="M15 3h6v6"/><path d="M10 14 21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>',
'phone':'<rect width="14" height="20" x="5" y="2" rx="2" ry="2"/><path d="M12 18h.01"/>',
'user':'<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
'layers':'<path d="m12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83Z"/><path d="m22 17.65-9.17 4.16a2 2 0 0 1-1.66 0L2 17.65"/><path d="m22 12.65-9.17 4.16a2 2 0 0 1-1.66 0L2 12.65"/>',
'bell':'<path d="M10.268 21a2 2 0 0 0 3.464 0"/><path d="M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"/>',
'more':'<circle cx="5" cy="12" r="1.6"/><circle cx="12" cy="12" r="1.6"/><circle cx="19" cy="12" r="1.6"/>',
'sliders':'<line x1="4" x2="20" y1="8" y2="8"/><line x1="4" x2="20" y1="16" y2="16"/><circle cx="9" cy="8" r="2.2"/><circle cx="15" cy="16" r="2.2"/>',
'trash':'<path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>',
}
def sprite():
    return '<svg width="0" height="0" style="position:absolute" aria-hidden="true"><defs>'+''.join(f'<symbol id="i-{k}" viewBox="0 0 24 24">{v}</symbol>' for k,v in SYM.items())+'</defs></svg>'
def ic(n,s=None,extra=''):
    st=f' style="width:{s}px;height:{s}px;{extra}"' if s else (f' style="{extra}"' if extra else '')
    return f'<svg class="ic"{st}><use href="#i-{n}"/></svg>'
def btn(label,kind='',icon=None,arrow=True,extra='',chip=None):
    ar=f'<span class="ar">{ic(chip or "arrow",20)}</span>' if arrow else ''
    i=ic(icon,20) if icon else ''
    cls='btn '+kind+('' if arrow else ' c')
    return f'<button class="{cls.strip()}" style="{extra}"><span style="display:flex;align-items:center;gap:10px">{i}{label}</span>{ar}</button>'
def tl(label,icon=None,color=None):
    st=f' style="color:{color}"' if color else ''
    return f'<button class="tl"{st}>{ic(icon,20) if icon else ""}{label}</button>'
def top(back=True,right=''):
    b=f'<button class="bk" aria-label="Atrás">{ic("back")}</button>' if back else '<span></span>'
    return f'<div class="top">{b}{right or "<img src=%s alt=NetProtect style=height:26px;width:auto;margin-right:12px>"%SH}</div>'
def seal(st,mode='ok',size=150):
    # st: list of 4 in 'ok','pe','off'
    arcs=''
    for i,s in enumerate(st):
        rot=-90+i*90+7
        if s=='ok': col,cls,op='#16b364',f'arc a{i+1}',1
        elif s=='pe': col,cls,op='#f5b455','',1
        else: col,cls,op='#4a5f7d','',1
        arcs+=f'<circle cx="84" cy="84" r="70" fill="none" stroke="#1c3350" stroke-width="8" stroke-linecap="round" stroke-dasharray="92 348" transform="rotate({rot} 84 84)"/>'
        dash='stroke-dasharray="92 348"' if s=='ok' else ('stroke-dasharray="92 348"' if s=='pe' else 'stroke-dasharray="3 12"')
        arcs+=f'<circle class="{cls}" cx="84" cy="84" r="70" fill="none" stroke="{col}" stroke-width="{8 if s!="off" else 3}" stroke-linecap="round" {dash} transform="rotate({rot} 84 84)"/>'
    pulse=''
    return f'<svg viewBox="0 0 168 168" width="{size}" height="{size}" aria-hidden="true">{pulse}{arcs}<image href="{SH}" x="54" y="54" width="60" height="60" opacity="{1 if mode!="off" else .5}"/></svg>'
def board(title,w,h,body,props=None):
    return f'''<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Playfair+Display:ital,wght@0,600;1,400&display=swap" rel="stylesheet">
<style>{CSS}</style>
</helmet>
{sprite()}
<div style="position:relative;width:{w}px;height:{h}px;background:#ebe8e1;box-sizing:border-box;padding:56px;display:flex;gap:48px;align-items:flex-start">
{body}
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{w},"height":{h}}}}}'>
class Component extends DCLogic {{
renderVals() {{ return {{}}; }}
}}
</script>
</body>
</html>
'''
def col(cap,phone,pv=None):
    p=f'<span class="pv">{pv}</span>\n' if pv else ''
    return f'<div>\n<div class="cap">{cap}</div>\n{p}{phone}\n</div>\n'
def ph(inner,k=''):
    return f'<div class="ph {k}">{inner}<div class="gest"></div></div>'
def save(name,html): open(OUT+name,'w').write(html)
GOOGLE='<svg width="22" height="22" viewBox="0 0 18 18" aria-hidden="true"><path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/><path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z" fill="#34A853"/><path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332z" fill="#FBBC05"/><path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/></svg>'
