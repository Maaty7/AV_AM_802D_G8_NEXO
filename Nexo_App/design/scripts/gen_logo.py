"""Genera el logotipo NEXO (candado) como SVG fuente + VectorDrawable + previews.

Geometría en un lienzo de 100 x 100 unidades ("marca"):
  cuerpo   : rect redondeado x20..80, y44..90, radio 13 (~22 % del ancho)
  arco     : U invertida de x33 a x67, tope en y14, trazo 9, puntas redondas
  circuito : nodo en la pata derecha del arco + 2 pistas cortas con vías
  ojo      : cerradura clásica (círculo + ranura) en neón
"""
import os, sys, subprocess
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

REPO = sys.argv[1]
RES = os.path.join(REPO, "app/src/main/res/drawable")
DESIGN = os.path.join(REPO, "design")
os.makedirs(os.path.join(DESIGN, "logo"), exist_ok=True)
os.makedirs(os.path.join(DESIGN, "mockups"), exist_ok=True)

SKY, DEEP, NEON, MIST = "#38BDF8", "#0B2D6B", "#E4FF3A", "#F5FAFF"
NIGHT, ABYSS, SKYSOFT = "#061A3F", "#030E26", "#BFE8FC"

BODY = "M33,44 H67 A13,13 0 0 1 80,57 V77 A13,13 0 0 1 67,90 H33 A13,13 0 0 1 20,77 V57 A13,13 0 0 1 33,44 Z"
SHACKLE = "M33,47 V31 A17,17 0 0 1 67,31 V47"
KEYHOLE = "M50,56 A6,6 0 0 1 53,67.2 L54,76 H46 L47,67.2 A6,6 0 0 1 50,56 Z"
TRACES = ["M71.5,34 H80 L85,29 H92", "M71.5,40 H92"]
NODE = (67, 37, 4.6)            # nodo sobre la pata derecha del arco
VIAS = [(94.5, 29, 2.6), (94.5, 40, 2.6)]
TRACE_W, SHACKLE_W = 3.0, 9.0


def circle_path(cx, cy, r):
    return f"M{cx - r},{cy} A{r},{r} 0 1 0 {cx + r},{cy} A{r},{r} 0 1 0 {cx - r},{cy} Z"


# ── Palabra NEXO convertida a paths (Inter Display ExtraBold, tracking +6 %) ──
def wordmark(height_units, tracking=0.06):
    font = TTFont("/usr/share/fonts/opentype/inter/InterDisplay-ExtraBold.otf")
    gs = font.getGlyphSet(); cmap = font.getBestCmap()
    cap = font["OS/2"].sCapHeight
    s = height_units / cap
    upm = font["head"].unitsPerEm
    paths, x = [], 0.0
    for i, ch in enumerate("NEXO"):
        g = gs[cmap[ord(ch)]]
        pen = SVGPathPen(gs)
        g.draw(TransformPen(pen, (s, 0, 0, -s, x, height_units)))
        paths.append(pen.getCommands())
        x += g.width * s + (tracking * upm * s if i < 3 else 0)
    return " ".join(paths), x


def grad_shackle(top, bottom, fmt):
    if fmt == "svg":
        return (f'<linearGradient id="gArco" x1="50" y1="14" x2="50" y2="47" gradientUnits="userSpaceOnUse">'
                f'<stop offset="0" stop-color="{top}"/><stop offset="1" stop-color="{bottom}"/></linearGradient>')
    return (f'<aapt:attr name="android:strokeColor"><gradient android:type="linear" '
            f'android:startX="50" android:startY="14" android:endX="50" android:endY="47" '
            f'android:startColor="{vd(top)}" android:endColor="{vd(bottom)}"/></aapt:attr>')


def vd(hex_color, alpha=1.0):
    a = int(round(alpha * 255))
    return "#%02X%s" % (a, hex_color[1:].upper())


SHACKLE_ABIERTO = "M33,37 V21 A17,17 0 0 1 67,21 V37"


def mark_elements(v):
    """v = variante: dict con colores. Devuelve lista de (tipo, datos)."""
    els = []
    shackle = SHACKLE_ABIERTO if v.get("abierto") else SHACKLE
    # pistas de circuito (detrás del arco)
    for t in TRACES:
        els.append(("stroke", t, v["trace"], TRACE_W, None, "traza"))
    for (cx, cy, r) in VIAS:
        els.append(("fill", circle_path(cx, cy, r), v["trace"], None, None, "via"))
    els.append(("stroke", shackle, v.get("shackle_solid"), SHACKLE_W, v.get("shackle_grad"), "arco"))
    els.append(("fill", BODY, v["body"], None, None, "cuerpo"))
    if v.get("rim"):
        els.append(("stroke", BODY, v["rim"], 1.6, None, "borde"))
    els.append(("fill", circle_path(*NODE), v["node"], None, None, "nodo"))
    els.append(("fill", KEYHOLE, v["key"], None, None, "ojo"))
    if v.get("key_ring"):
        els.append(("stroke", KEYHOLE, v["key_ring"], 1.4, None, "ojo_borde"))
    return els


VARIANTES = {
    "claro": dict(trace=SKY, shackle_grad=(SKY, DEEP), body=DEEP, node=SKY, key=NEON),
    "oscuro": dict(trace=SKY, shackle_grad=(SKY, DEEP), body=DEEP, rim=SKY + "99", node=SKY, key=NEON),
    "launcher": dict(trace=SKY, shackle_grad=(SKY, MIST), body=MIST, node=SKY, key=NEON, key_ring=DEEP),
    "mono": dict(trace="#FFFFFF", shackle_solid="#FFFFFF", body="#FFFFFF", node="#FFFFFF", key=None),
}


# ── Escritores ──
def svg_doc(w, h, body, bg=None):
    bgr = f'<rect width="{w}" height="{h}" fill="{bg}"/>' if bg else ""
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w*4}" height="{h*4}">{bgr}{body}</svg>'


def svg_mark(v, transform=""):
    out = []
    for kind, d, color, sw, grad, name in mark_elements(v):
        if kind == "fill" and color is None:
            continue
        if name == "ojo" and v.get("key") is None:
            continue
        if kind == "fill":
            out.append(f'<path id="{name}" d="{d}" fill="{color}"/>')
        else:
            stroke = "url(#gArco)" if grad else color
            out.append(f'<path id="{name}" d="{d}" fill="none" stroke="{stroke}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round"/>')
    defs = f"<defs>{grad_shackle(*v['shackle_grad'], 'svg')}</defs>" if v.get("shackle_grad") else ""
    if v.get("key") is None:  # mono: ojo recortado del cuerpo
        out = [o.replace(f'd="{BODY}" fill', f'd="{BODY} {KEYHOLE}" fill-rule="evenodd" fill') for o in out]
    return defs + f'<g transform="{transform}">' + "".join(out) + "</g>"


def vd_color(c):
    if len(c) == 9:  # #RRGGBBAA → #AARRGGBB
        return "#" + c[7:9].upper() + c[1:7].upper()
    return vd(c)


def vd_mark(v, sx=1.0, tx=0.0, ty=0.0):
    out = [f'<group android:name="marca" android:scaleX="{sx}" android:scaleY="{sx}" android:translateX="{tx}" android:translateY="{ty}">']
    for kind, d, color, sw, grad, name in mark_elements(v):
        if name == "ojo" and v.get("key") is None:
            continue
        if kind == "fill":
            if name == "cuerpo" and v.get("key") is None:
                out.append(f'<path android:name="{name}" android:fillType="evenOdd" android:fillColor="{vd_color(color)}" android:pathData="{BODY} {KEYHOLE}"/>')
            else:
                out.append(f'<path android:name="{name}" android:fillColor="{vd_color(color)}" android:pathData="{d}"/>')
        else:
            if grad:
                out.append(f'<path android:name="{name}" android:pathData="{d}" android:strokeWidth="{sw}" '
                           f'android:strokeLineCap="round" android:strokeLineJoin="round">{grad_shackle(*grad, "vd")}</path>')
            else:
                out.append(f'<path android:name="{name}" android:pathData="{d}" android:strokeColor="{vd_color(color)}" '
                           f'android:strokeWidth="{sw}" android:strokeLineCap="round" android:strokeLineJoin="round"/>')
    out.append("</group>")
    return "\n    ".join(out)


def vector(w_dp, h_dp, vw, vh, inner):
    return (f'<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- Generado por design/README: logotipo NEXO (candado). No editar a mano: regenerar desde design/logo/*.svg -->\n'
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    xmlns:aapt="http://schemas.android.com/aapt"\n'
            f'    android:width="{w_dp}dp" android:height="{h_dp}dp"\n'
            f'    android:viewportWidth="{vw}" android:viewportHeight="{vh}">\n    {inner}\n</vector>\n')


def write(path, text):
    with open(path, "w") as fh:
        fh.write(text)


# 1) logo_mark (claro / oscuro / mono)
for nombre, var in [("logo_mark", "claro"), ("logo_mark_oscuro", "oscuro"), ("logo_mono", "mono")]:
    write(os.path.join(RES, nombre + ".xml"), vector(48, 48, 100, 100, vd_mark(VARIANTES[var])))
    write(os.path.join(DESIGN, "logo", nombre + ".svg"), svg_doc(100, 100, svg_mark(VARIANTES[var])))

# 2) logo_full_vertical / horizontal (+ oscuro)
wm_h = 22
wm_path, wm_w = wordmark(wm_h)
for sufijo, var, txt in [("", "claro", DEEP), ("_oscuro", "oscuro", MIST)]:
    v = VARIANTES[var]
    # vertical: candado arriba (100x100) + palabra centrada abajo
    W, H = 120, 140
    tx_m = (W - 100) / 2
    tx_w = (W - wm_w) / 2
    inner = vd_mark(v, 1.0, tx_m, 0) + (
        f'\n    <group android:name="palabra" android:translateX="{tx_w:.2f}" android:translateY="108">'
        f'<path android:name="nexo" android:fillColor="{vd(txt)}" android:pathData="{wm_path}"/></group>')
    write(os.path.join(RES, f"logo_full_vertical{sufijo}.xml"), vector(120, 140, W, H, inner))
    write(os.path.join(DESIGN, "logo", f"logo_full_vertical{sufijo}.svg"),
          svg_doc(W, H, svg_mark(v, f"translate({tx_m},0)") + f'<path transform="translate({tx_w:.2f},108)" d="{wm_path}" fill="{txt}"/>',
                  bg=(NIGHT if var == "oscuro" else MIST)))
    # horizontal: candado a la izquierda + palabra a la derecha
    W, H = 104 + wm_w + 6, 100
    inner = vd_mark(v) + (
        f'\n    <group android:name="palabra" android:translateX="104" android:translateY="{(H - wm_h) / 2 + 6:.2f}">'
        f'<path android:name="nexo" android:fillColor="{vd(txt)}" android:pathData="{wm_path}"/></group>')
    write(os.path.join(RES, f"logo_full_horizontal{sufijo}.xml"), vector(round(W * 0.48), 48, round(W, 2), H, inner))
    write(os.path.join(DESIGN, "logo", f"logo_full_horizontal{sufijo}.svg"),
          svg_doc(round(W, 2), H, svg_mark(v) + f'<path transform="translate(104,{(H - wm_h) / 2 + 6:.2f})" d="{wm_path}" fill="{txt}"/>',
                  bg=(NIGHT if var == "oscuro" else MIST)))

# 3) Ícono adaptativo (108 x 108, zona segura = círculo de 66 dp centrado)
#    La marca ocupa ~78 u de ancho (20..97) → escala 0.58 ≈ 45 dp, centrada.
S = 0.64
cx_mark, cy_mark = 58.5, 52  # centro visual del candado + pistas
tx, ty = 54 - cx_mark * S, 54 - cy_mark * S
write(os.path.join(RES, "ic_launcher_foreground.xml"), vector(108, 108, 108, 108, vd_mark(VARIANTES["launcher"], S, round(tx, 2), round(ty, 2))))
write(os.path.join(RES, "ic_launcher_monochrome.xml"), vector(108, 108, 108, 108, vd_mark(VARIANTES["mono"], S, round(tx, 2), round(ty, 2))))

# Fondo: degradado diagonal Deep → Night + patrón sutil de pistas (Sky 12 %)
pcb = [
    "M0,22 H20 L28,30 H46", "M108,18 H86 L78,26 H66", "M0,88 H16 L24,80 H40",
    "M108,92 H90 L82,84 H70", "M18,0 V12 L26,20", "M90,108 V96 L82,88", "M62,0 V10 L70,18 H84",
    "M40,108 V98 L32,90 H20",
]
vias = [(46, 30), (66, 26), (40, 80), (70, 84), (26, 20), (82, 88), (84, 18), (20, 90)]
bg_inner = [
    '<path android:pathData="M0,0h108v108h-108z"><aapt:attr name="android:fillColor">'
    f'<gradient android:type="linear" android:startX="0" android:startY="0" android:endX="108" android:endY="108" '
    f'android:startColor="{vd(DEEP)}" android:endColor="{vd(NIGHT)}"/></aapt:attr></path>'
]
for p in pcb:
    bg_inner.append(f'<path android:pathData="{p}" android:strokeColor="{vd(SKY, 0.12)}" android:strokeWidth="1.2" android:strokeLineCap="round" android:strokeLineJoin="round"/>')
for (x, y) in vias:
    bg_inner.append(f'<path android:pathData="{circle_path(x, y, 1.8)}" android:fillColor="{vd(SKY, 0.16)}"/>')
write(os.path.join(RES, "ic_launcher_background.xml"), vector(108, 108, 108, 108, "\n    ".join(bg_inner)))

# 4) Ícono del splash del sistema (Android 12+): misma marca, 108 u, sin fondo.
#    Escala 0.48 → el candado mide ~107 dp en el contenedor de 240 dp del sistema.
SS = 0.48
write(os.path.join(RES, "splash_icon.xml"),
      vector(108, 108, 108, 108, vd_mark(dict(VARIANTES["oscuro"], abierto=True), SS, round(54 - 50 * SS, 2), round(54 - 50 * SS, 2))))

# SVG fuente del ícono completo + previews
icon_svg = (f'<defs><linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">'
            f'<stop offset="0" stop-color="{DEEP}"/><stop offset="1" stop-color="{NIGHT}"/></linearGradient></defs>'
            f'<rect width="108" height="108" fill="url(#bg)"/>'
            + "".join(f'<path d="{p}" fill="none" stroke="{SKY}" stroke-opacity="0.12" stroke-width="1.2" stroke-linecap="round"/>' for p in pcb)
            + svg_mark(VARIANTES["launcher"], f"translate({tx:.2f},{ty:.2f}) scale({S})"))
write(os.path.join(DESIGN, "logo", "ic_launcher.svg"), svg_doc(108, 108, icon_svg))
print("ok", wm_w)
