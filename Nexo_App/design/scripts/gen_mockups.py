"""Mockups estáticos (SVG → PNG) del sistema de diseño NEXO v2.
Son referencia visual para el informe/presentación: el código Compose es la fuente de verdad."""
import os, re, sys, random, math, subprocess

REPO = sys.argv[1]
LOGO = os.path.join(REPO, "design/logo")
OUT = os.path.join(REPO, "design/mockups")
os.makedirs(OUT, exist_ok=True)
SKY, DEEP, NEON, MIST, NIGHT, ABYSS, SKYSOFT, NEONDIM = "#38BDF8", "#0B2D6B", "#E4FF3A", "#F5FAFF", "#061A3F", "#030E26", "#BFE8FC", "#B8D400"
FONT = "Inter"


def mark(nombre, x, y, size):
    s = open(os.path.join(LOGO, nombre + ".svg")).read()
    inner = re.sub(r"^<svg[^>]*>", "", s).replace("</svg>", "")
    inner = re.sub(r'<rect width="\d+" height="\d+" fill="[^"]+"/>', "", inner, count=1)
    inner = inner.replace('id="gArco"', f'id="gArco{nombre}{x}{y}"').replace("url(#gArco)", f"url(#gArco{nombre}{x}{y})")
    return f'<g transform="translate({x},{y}) scale({size / 100})">{inner}</g>'


def text(x, y, t, size, color, weight=400, anchor="start", family=FONT, ls=0, opacity=1):
    return (f'<text x="{x}" y="{y}" font-family="{family}" font-size="{size}" font-weight="{weight}" fill="{color}" '
            f'text-anchor="{anchor}" letter-spacing="{ls}" opacity="{opacity}">{t}</text>')


def fondo_alto(w, h, gid):
    return (f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{ABYSS}"/>'
            f'<stop offset=".55" stop-color="{NIGHT}"/><stop offset="1" stop-color="{DEEP}"/></linearGradient>'
            f'<radialGradient id="{gid}s"><stop offset="0" stop-color="{SKY}" stop-opacity=".42"/><stop offset="1" stop-color="{SKY}" stop-opacity="0"/></radialGradient>'
            f'<radialGradient id="{gid}d"><stop offset="0" stop-color="{DEEP}" stop-opacity=".9"/><stop offset="1" stop-color="{DEEP}" stop-opacity="0"/></radialGradient></defs>'
            f'<rect width="{w}" height="{h}" fill="url(#{gid})"/>'
            f'<circle cx="{w*.2}" cy="{h*.12}" r="{h*.6}" fill="url(#{gid}s)"/>'
            f'<circle cx="{w*.9}" cy="{h*.3}" r="{h*.5}" fill="url(#{gid}d)"/>'
            f'<circle cx="{w*.55}" cy="{h*.65}" r="{h*.45}" fill="url(#{gid}s)" opacity=".7"/>'
            f'<path d="M{-w*.1},{h*.7} C{w*.3},{h*.6} {w*.62},{h*.76} {w*1.1},{h*.58}" stroke="{NEON}" stroke-opacity=".22" stroke-width="1.2" fill="none"/>')


def circuitos(w, h, cx, cy, r0, n=14, seed=2026, alfa=.22, pulsos=True):
    rnd = random.Random(seed); paso = min(w, h) / 14; out = []
    for i in range(n):
        a = i / n * 2 * math.pi + (rnd.random() - .5) * .35
        dx, dy = math.cos(a), math.sin(a); hor = abs(dx) >= abs(dy)
        sx = 1 if dx >= 0 else -1; sy = 1 if dy >= 0 else -1
        x, y = cx + dx * r0, cy + dy * r0; pts = [(x, y)]
        A = paso * (1.2 + rnd.random() * 1.6)
        x, y = (x + sx * A, y) if hor else (x, y + sy * A); pts.append((x, y))
        B = paso * (.6 + rnd.random() * 1.2); x, y = x + sx * B, y + sy * B; pts.append((x, y))
        C = paso * (2 + rnd.random() * 4)
        x, y = (x + sx * C, y) if hor else (x, y + sy * C)
        x = min(max(x, paso / 2), w - paso / 2); y = min(max(y, paso / 2), h - paso / 2); pts.append((x, y))
        d = "M" + " L".join(f"{p[0]:.1f},{p[1]:.1f}" for p in pts)
        out.append(f'<path d="{d}" stroke="{SKY}" stroke-opacity="{alfa}" stroke-width="1.8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>')
        lit = pulsos and i % 3 == 0
        out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{4.5 if lit else 2.9}" fill="{SKY}" opacity="{1 if lit else alfa*2}"/>')
        if pulsos and i % 2 == 0:
            col = NEON if i % 3 == 0 else SKY
            # pulso cometa sobre el segundo tramo
            (x1, y1), (x2, y2) = pts[2], pts[3]
            mx, my = x1 + (x2 - x1) * .6, y1 + (y2 - y1) * .6
            out.append(f'<line x1="{x1 + (x2-x1)*.35:.1f}" y1="{y1 + (y2-y1)*.35:.1f}" x2="{mx:.1f}" y2="{my:.1f}" stroke="{col}" stroke-width="6" stroke-opacity=".22" stroke-linecap="round"/>')
            out.append(f'<line x1="{x1 + (x2-x1)*.45:.1f}" y1="{y1 + (y2-y1)*.45:.1f}" x2="{mx:.1f}" y2="{my:.1f}" stroke="{col}" stroke-width="2" stroke-linecap="round"/>')
        if rnd.random() < .6:
            L = paso * (.8 + rnd.random() * 1.2); sg = 1 if rnd.random() < .5 else -1
            qx, qy = pts[2]
            q1 = (qx, qy + sg * L) if hor else (qx + sg * L, qy)
            q2 = (q1[0] + sx * L * .7, q1[1] + sg * L * .7) if hor else (q1[0] + sg * L * .7, q1[1] + sy * L * .7)
            out.append(f'<path d="M{qx:.1f},{qy:.1f} L{q1[0]:.1f},{q1[1]:.1f} L{q2[0]:.1f},{q2[1]:.1f}" stroke="{SKY}" stroke-opacity="{alfa}" stroke-width="1.8" fill="none" stroke-linecap="round"/>')
            out.append(f'<circle cx="{q2[0]:.1f}" cy="{q2[1]:.1f}" r="2.9" fill="{SKY}" opacity="{alfa*2}"/>')
    return "".join(out)


def telefono(contenido, w=360, h=760):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><clipPath id="t"><rect width="{w}" height="{h}" rx="28"/></clipPath><g clip-path="url(#t)">{contenido}</g></svg>'


def guardar(nombre, svg):
    p = os.path.join(OUT, nombre + ".svg")
    open(p, "w").write(svg)
    subprocess.run(["rsvg-convert", "-z", "2", p, "-o", os.path.join(OUT, nombre + ".png")], check=True)
    os.remove(p)


W, H = 360, 760

# ── Splash ──
sp = fondo_alto(W, H, "ga") + circuitos(W, H, W / 2, H / 2, 68) + mark("logo_mark_oscuro", W / 2 - 64, H / 2 - 64, 128) + \
    text(W / 2, H / 2 + 100, "NEXO", 34, MIST, 800, "middle", ls=2)
guardar("splash_loop", telefono(sp))

# ── PIN ──
pin = fondo_alto(W, H, "gb") + circuitos(W, H, W / 2, 120, 40, n=10, alfa=.14, pulsos=False)
pin += mark("logo_mark_oscuro", W / 2 - 36, 64, 72)
pin += text(W / 2, 176, "Verifica tu PIN actual", 24, MIST, 700, "middle")
pin += text(W / 2, 200, "Ingresa tu PIN actual para continuar", 14, MIST, 400, "middle", opacity=.7)
pin += f'<rect x="20" y="228" width="320" height="436" rx="24" fill="{NIGHT}" fill-opacity=".72" stroke="{SKY}" stroke-opacity=".25"/>'
for i in range(4):
    cx = W / 2 - 54 + i * 36
    pin += f'<circle cx="{cx}" cy="268" r="8" fill="{NEON if i < 2 else "none"}" stroke="{NEON if i < 2 else MIST}" stroke-opacity="{1 if i < 2 else .45}" stroke-width="1.5"/>'
teclas = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫"]
for k, t in enumerate(teclas):
    r, c = divmod(k, 3); cx = 70 + c * 110; cy = 348 + r * 80
    if t and t != "⌫":
        pin += f'<circle cx="{cx}" cy="{cy}" r="34" fill="{MIST}" fill-opacity=".06" stroke="{SKY}" stroke-opacity=".25"/>'
    if t:
        pin += text(cx, cy + 9, t, 24, MIST, 700, "middle")
guardar("pin", telefono(pin))


# ── Home (claro / oscuro) ──
def home(oscuro):
    bg = NIGHT if oscuro else MIST
    tx = MIST if oscuro else DEEP
    sec_op = .7 if oscuro else .65
    glass = f'fill="{NIGHT if oscuro else MIST}" fill-opacity="{.72 if oscuro else .84}" stroke="{SKY if oscuro else MIST}" stroke-opacity="{.25 if oscuro else .9}"'
    g = (f'<defs><radialGradient id="h{oscuro}"><stop offset="0" stop-color="{SKY if oscuro else SKYSOFT}" stop-opacity="{.26 if oscuro else .5}"/>'
         f'<stop offset="1" stop-color="{SKY}" stop-opacity="0"/></radialGradient>'
         f'<filter id="sh{oscuro}" x="-20%" y="-20%" width="140%" height="160%"><feDropShadow dx="0" dy="6" stdDeviation="8" flood-color="{DEEP}" flood-opacity="{0 if oscuro else .14}"/></filter>'
         f'<linearGradient id="hf{oscuro}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{bg}" stop-opacity="0"/><stop offset="1" stop-color="{bg}"/></linearGradient></defs>'
         f'<rect width="{W}" height="{H}" fill="{bg}"/><circle cx="70" cy="80" r="300" fill="url(#h{oscuro})"/>'
         f'<rect y="90" width="{W}" height="230" fill="url(#hf{oscuro})"/><rect y="320" width="{W}" height="{H}" fill="{bg}"/>')
    g += text(20, 76, "Buenas tardes", 14, tx, 400, opacity=sec_op) + text(20, 108, "Hola", 26, tx, 700)
    g += mark("logo_mark_oscuro" if oscuro else "logo_mark", 300, 60, 40)
    g += f'<g filter="url(#sh{oscuro})"><rect x="20" y="136" width="320" height="168" rx="24" {glass}/></g>'
    g += text(40, 172, "Jornada escolar", 16, tx, 600, opacity=sec_op)
    g += text(40, 214, "08:00 – 14:00", 32, tx, 700)
    g += f'<rect x="40" y="240" width="134" height="30" rx="15" fill="{SKY}" fill-opacity="{.18 if oscuro else .22}"/>'
    g += f'<circle cx="56" cy="255" r="6" fill="{SKY if oscuro else DEEP}"/>' + text(70, 260, "Jornada activa", 12, SKY if oscuro else DEEP, 600)
    g += text(20, 340, "RESUMEN DE HOY", 12, tx, 600, ls=.7, opacity=sec_op)
    for i, (v, e) in enumerate([("186 min", "Tiempo total"), ("74 min", "Educación")]):
        x = 20 + i * 166
        g += f'<g filter="url(#sh{oscuro})"><rect x="{x}" y="356" width="154" height="84" rx="24" {glass}/></g>'
        g += text(x + 16, 394, v, 24, tx, 700) + text(x + 16, 420, e, 12, tx, 400, opacity=sec_op)
    g += f'<g filter="url(#sh{oscuro})"><rect x="20" y="452" width="320" height="96" rx="24" {glass}/></g>'
    g += text(40, 482, "Distracción", 12, tx, 600, opacity=sec_op)
    if oscuro:
        g += text(40, 518, "38 %", 18, NEON, 600)
    else:
        g += f'<rect x="40" y="498" width="66" height="28" rx="14" fill="{NEON}"/>' + text(73, 518, "38 %", 16, DEEP, 600, "middle")
    g += text(118, 517, "71 min en redes, juegos y entretenimiento", 11, tx, 400, opacity=sec_op)
    # bottom bar
    g += f'<g filter="url(#sh{oscuro})"><rect x="16" y="676" width="328" height="64" rx="32" fill="{NIGHT if oscuro else MIST}" fill-opacity=".9" stroke="{SKY if oscuro else DEEP}" stroke-opacity="{.25 if oscuro else .1}"/></g>'
    g += f'<rect x="22" y="682" width="70" height="52" rx="26" fill="{NEON}"/>'
    for i, lab in enumerate(["Inicio", "Estadísticas", "Horarios", "Ajustes"]):
        cx = 16 + 41 + i * 82
        col = DEEP if i == 0 else tx
        g += f'<rect x="{cx-9}" y="694" width="18" height="16" rx="4" fill="{col}" opacity="{1 if i == 0 else sec_op}"/>'
        g += text(cx, 726, lab, 11, col, 500, "middle", opacity=1 if i == 0 else sec_op)
    return telefono(g)


guardar("home_claro", home(False))
guardar("home_oscuro", home(True))

# ── Hoja del sistema de diseño ──
SW, SH = 1200, 760
d = f'<rect width="{SW}" height="{SH}" fill="{MIST}"/>'
d += mark("logo_mark", 40, 32, 64) + text(116, 76, "NEXO · Sistema de diseño v2", 30, DEEP, 800)
d += text(40, 140, "PALETA", 12, DEEP, 600, ls=1, opacity=.65)
swatches = [("NexoSky", SKY, "Marca luminosa"), ("NexoDeep", DEEP, "Texto · marca seria"), ("NexoNeon", NEON, "Solo destacar"),
            ("NexoMist", MIST, "Fondo claro"), ("NexoNight", NIGHT, "Fondo oscuro"), ("NexoAbyss", ABYSS, "Capa profunda"),
            ("NexoSkySoft", SKYSOFT, "Chips · contenedores"), ("NexoNeonDim", NEONDIM, "Neón presionado")]
for i, (n, c, rol) in enumerate(swatches):
    x = 40 + (i % 4) * 280; y = 156 + (i // 4) * 120
    oscuroTx = c in (DEEP, NIGHT, ABYSS)
    d += f'<rect x="{x}" y="{y}" width="264" height="104" rx="20" fill="{c}" stroke="{DEEP}" stroke-opacity=".12"/>'
    d += text(x + 16, y + 36, n, 18, MIST if oscuroTx else DEEP, 700) + text(x + 16, y + 60, c, 14, MIST if oscuroTx else DEEP, 500, opacity=.8)
    d += text(x + 16, y + 86, rol, 13, MIST if oscuroTx else DEEP, 400, opacity=.7)
d += text(40, 422, "PROPORCIÓN (TEMA CLARO / OSCURO)", 12, DEEP, 600, ls=1, opacity=.65)
d += f'<rect x="40" y="436" width="{540*.55}" height="20" fill="{MIST}" stroke="{DEEP}" stroke-opacity=".2"/><rect x="{40+540*.55}" y="436" width="{540*.38}" height="20" fill="{SKY}"/><rect x="{40+540*.93}" y="436" width="{540*.07}" height="20" fill="{NEON}"/>'
d += f'<rect x="40" y="464" width="{540*.6}" height="20" fill="{NIGHT}"/><rect x="{40+540*.6}" y="464" width="{540*.3}" height="20" fill="{DEEP}"/><rect x="{40+540*.9}" y="464" width="{540*.1}" height="20" fill="{NEON}"/>'
d += text(40, 512, "TIPOGRAFÍA — Inter Display (títulos) · Inter (texto y números tabulares)", 12, DEEP, 600, ls=1, opacity=.65)
d += text(40, 556, "Headline · Mis jornadas", 30, DEEP, 700) + text(40, 588, "Title · Distribución por categoría", 20, DEEP, 600)
d += text(40, 616, "Body · Configura la supervisión digital de tu hogar en un par de minutos.", 14, DEEP, 400)
d += text(40, 640, "Texto secundario: NexoDeep 65 %", 14, DEEP, 400, opacity=.65)
d += text(640, 422, "COMPONENTES", 12, DEEP, 600, ls=1, opacity=.65)
d += f'<rect x="640" y="440" width="200" height="52" rx="26" fill="{NEON}"/>' + text(740, 472, "Continuar", 14, DEEP, 600, "middle")
d += f'<rect x="860" y="448" width="128" height="32" rx="16" fill="{SKY}" fill-opacity=".22"/>' + text(924, 469, "✓ Concedido", 12, DEEP, 600, "middle")
d += f'<rect x="1000" y="448" width="128" height="32" rx="16" fill="{NEON}" fill-opacity=".2"/>' + text(1064, 469, "⚠ Pendiente", 12, DEEP, 600, "middle")
d += f'<rect x="640" y="512" width="520" height="132" rx="24" fill="{MIST}" stroke="{DEEP}" stroke-opacity=".1"/>'
d += text(664, 548, "GlassCard", 20, DEEP, 600) + text(664, 574, "Tinte Mist 84 % · borde 1 dp · radio 24 dp · sombra Deep", 13, DEEP, 400, opacity=.65)
cats = [("Educación", DEEP, 1), ("Comunicación", SKY, 1), ("Otros", SKY, .4), ("Redes", NEON, 1), ("Entreten.", NEON, .65), ("Juegos", NEONDIM, 1)]
for i, (n, c, a) in enumerate(cats):
    x = 664 + i * 80
    d += f'<rect x="{x}" y="596" width="20" height="20" rx="5" fill="{c}" fill-opacity="{a}" stroke="{DEEP}" stroke-opacity="{.2 if c in (NEON, NEONDIM) else 0}"/>' + text(x + 26, 611, n, 11, DEEP, 500)
d += text(40, 700, "Reglas: el neón nunca es texto sobre fondo claro · el celeste no es texto chico sobre blanco · errores = ícono + texto", 13, DEEP, 500, opacity=.8)
guardar("sistema_de_diseno", f'<svg xmlns="http://www.w3.org/2000/svg" width="{SW}" height="{SH}" viewBox="0 0 {SW} {SH}">{d}</svg>')
print("ok")
